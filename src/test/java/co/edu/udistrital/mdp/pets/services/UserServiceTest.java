package co.edu.udistrital.mdp.pets.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.pets.entities.MessageEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationEntity;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(UserService.class)
class UserServiceTest {

	private static final String PASSWORD = "Str0ngPass";
	private static final String ADMIN = "ADMIN";

	@Autowired
	private UserService userService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<UserEntity> userList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from MessageEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ReturnEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionRequestEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			UserEntity user = buildUser("user" + i + "@pets.com");
			entityManager.persist(user);
			userList.add(user);
		}
	}

	private UserEntity buildUser(String email) {
		UserEntity user = new UserEntity();
		user.setFirstName("Name" + email);
		user.setLastName("Last" + email);
		user.setEmail(email);
		user.setPassword(PASSWORD);
		user.setPhone("3001234567");
		return user;
	}

	private AdopterEntity persistAdopter() {
		AdopterEntity adopter = factory.manufacturePojo(AdopterEntity.class);
		entityManager.persist(adopter);
		return adopter;
	}

	private PetEntity persistPet() {
		PetEntity pet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(pet);
		return pet;
	}

	private UserEntity newUpdate(String firstName, String lastName, String email) {
		UserEntity update = new UserEntity();
		update.setFirstName(firstName);
		update.setLastName(lastName);
		update.setEmail(email);
		update.setPhone("3109999999");
		return update;
	}

	// ---------------------------------------------------------------- create

	@Test
	void testCreateUser() throws IllegalOperationException {
		UserEntity newUser = buildUser("new@pets.com");

		UserEntity result = userService.createUser(newUser);

		assertNotNull(result.getId());
		UserEntity stored = entityManager.find(UserEntity.class, result.getId());
		assertEquals("new@pets.com", stored.getEmail());
	}

	@Test
	void testCreateUserWithNullUser() {
		assertThrows(IllegalOperationException.class, () -> userService.createUser(null));
	}

	@Test
	void testCreateUserWithMissingAttributes() {
		UserEntity noFirstName = buildUser("a@pets.com");
		noFirstName.setFirstName(null);
		assertThrows(IllegalOperationException.class, () -> userService.createUser(noFirstName));

		UserEntity blankLastName = buildUser("b@pets.com");
		blankLastName.setLastName("  ");
		assertThrows(IllegalOperationException.class, () -> userService.createUser(blankLastName));

		UserEntity noEmail = buildUser("c@pets.com");
		noEmail.setEmail(null);
		assertThrows(IllegalOperationException.class, () -> userService.createUser(noEmail));

		UserEntity noPassword = buildUser("d@pets.com");
		noPassword.setPassword(null);
		assertThrows(IllegalOperationException.class, () -> userService.createUser(noPassword));
	}

	@Test
	void testCreateUserWithWeakPassword() {
		UserEntity weak = buildUser("weak@pets.com");
		weak.setPassword("1234");
		assertThrows(IllegalOperationException.class, () -> userService.createUser(weak));
	}

	@Test
	void testCreateUserWithDuplicatedEmail() {
		UserEntity duplicated = buildUser(userList.get(0).getEmail());
		assertThrows(IllegalOperationException.class, () -> userService.createUser(duplicated));
	}

	// ------------------------------------------------------------------ read

	@Test
	void testReadUserDoesNotReturnPassword() throws EntityNotFoundException, IllegalOperationException {
		UserEntity expected = userList.get(0);

		UserEntity result = userService.readUser(expected.getId());

		assertEquals(expected.getId(), result.getId());
		assertEquals(expected.getEmail(), result.getEmail());
		assertNull(result.getPassword());
		// La entidad gestionada (lo que hay en la BD) conserva su contraseña
		assertEquals(PASSWORD, entityManager.find(UserEntity.class, expected.getId()).getPassword());
	}

	@Test
	void testReadUserWithInvalidId() {
		assertThrows(IllegalOperationException.class, () -> userService.readUser(null));
		assertThrows(IllegalOperationException.class, () -> userService.readUser(0L));
		assertThrows(IllegalOperationException.class, () -> userService.readUser(-1L));
	}

	@Test
	void testReadUserNotFound() {
		assertThrows(EntityNotFoundException.class, () -> userService.readUser(999999L));
	}

	// -------------------------------------------------------------- readAll

	@Test
	void testReadAllUsersAsAdmin() throws IllegalOperationException {
		List<UserEntity> result = userService.readAllUsers(ADMIN);

		assertEquals(userList.size(), result.size());
		result.forEach(u -> assertNull(u.getPassword()));
	}

	@Test
	void testReadAllUsersAsNonAdmin() {
		assertThrows(IllegalOperationException.class, () -> userService.readAllUsers("ADOPTER"));
		assertThrows(IllegalOperationException.class, () -> userService.readAllUsers(null));
	}

	@Test
	void testReadAllUsersFilteredByEmail() throws IllegalOperationException {
		UserEntity target = userList.get(1);

		List<UserEntity> result = userService.readAllUsers(ADMIN, null, null, target.getEmail(), null);

		assertEquals(1, result.size());
		assertEquals(target.getId(), result.get(0).getId());
	}

	@Test
	void testReadAllUsersWithAllFiltersMatching() throws IllegalOperationException {
		UserEntity target = userList.get(2);

		List<UserEntity> result = userService.readAllUsers(ADMIN, target.getFirstName(), target.getLastName(),
				target.getEmail(), null);

		assertEquals(1, result.size());
		assertEquals(target.getId(), result.get(0).getId());
	}

	@Test
	void testReadAllUsersWithFiltersThatDoNotMatchTogether() throws IllegalOperationException {
		// Cada filtro coincide con un usuario distinto: al combinarlos no hay resultado
		UserEntity first = userList.get(0);
		UserEntity second = userList.get(1);

		List<UserEntity> result = userService.readAllUsers(ADMIN, first.getFirstName(), null, second.getEmail(),
				null);

		assertTrue(result.isEmpty());
	}

	@Test
	void testReadAllUsersWithBlankFilter() {
		assertThrows(IllegalOperationException.class, () -> userService.readAllUsers(ADMIN, "  ", null, null, null));
	}

	// ---------------------------------------------------------------- update

	@Test
	void testUpdateUser() throws EntityNotFoundException, IllegalOperationException {
		UserEntity target = userList.get(0);

		userService.updateUser(target.getId(), newUpdate("NewName", "NewLast", "changed@pets.com"));

		UserEntity stored = entityManager.find(UserEntity.class, target.getId());
		assertEquals("NewName", stored.getFirstName());
		assertEquals("NewLast", stored.getLastName());
		assertEquals("changed@pets.com", stored.getEmail());
		assertEquals("3109999999", stored.getPhone());
		assertEquals(PASSWORD, stored.getPassword());
	}

	@Test
	void testUpdateUserKeepingOwnEmail() throws EntityNotFoundException, IllegalOperationException {
		UserEntity target = userList.get(0);

		UserEntity result = userService.updateUser(target.getId(),
				newUpdate("OtherName", "OtherLast", target.getEmail()));

		assertEquals("OtherName", result.getFirstName());
		assertEquals(target.getEmail(), result.getEmail());
	}

	@Test
	void testUpdateUserWithInvalidId() {
		UserEntity update = newUpdate("A", "B", "x@pets.com");
		assertThrows(IllegalOperationException.class, () -> userService.updateUser(null, update));
		assertThrows(IllegalOperationException.class, () -> userService.updateUser(0L, update));
		assertThrows(IllegalOperationException.class, () -> userService.updateUser(-1L, update));
	}

	@Test
	void testUpdateUserWithNullAttributes() {
		Long id = userList.get(0).getId();
		assertThrows(IllegalOperationException.class, () -> userService.updateUser(id, null));
		assertThrows(IllegalOperationException.class,
				() -> userService.updateUser(id, newUpdate(null, "B", "x@pets.com")));
		assertThrows(IllegalOperationException.class,
				() -> userService.updateUser(id, newUpdate("A", "  ", "x@pets.com")));
		assertThrows(IllegalOperationException.class, () -> userService.updateUser(id, newUpdate("A", "B", null)));
	}

	@Test
	void testUpdateUserWithEmailOfAnotherUser() {
		Long id = userList.get(0).getId();
		UserEntity update = newUpdate("A", "B", userList.get(1).getEmail());
		assertThrows(IllegalOperationException.class, () -> userService.updateUser(id, update));
	}

	@Test
	void testUpdateUserNotFound() {
		UserEntity update = newUpdate("A", "B", "x@pets.com");
		assertThrows(EntityNotFoundException.class, () -> userService.updateUser(999999L, update));
	}

	// ---------------------------------------------------------------- delete

	@Test
	void testDeleteUser() throws EntityNotFoundException, IllegalOperationException {
		UserEntity target = userList.get(0);

		userService.deleteUser(target.getId());
		entityManager.flush();

		assertNull(entityManager.find(UserEntity.class, target.getId()));
	}

	@Test
	void testDeleteUserWithInvalidId() {
		assertThrows(IllegalOperationException.class, () -> userService.deleteUser(null));
		assertThrows(IllegalOperationException.class, () -> userService.deleteUser(0L));
		assertThrows(IllegalOperationException.class, () -> userService.deleteUser(-1L));
	}

	@Test
	void testDeleteUserNotFound() {
		assertThrows(EntityNotFoundException.class, () -> userService.deleteUser(999999L));
	}

	@Test
	void testDeleteUserWithActiveAdoptionRequest() {
		AdopterEntity adopter = persistAdopter();
		AdoptionRequestEntity request = new AdoptionRequestEntity();
		request.setAdopter(adopter);
		request.setPet(persistPet());
		request.setStatus("PENDING");
		request.setDate(new Date());
		request.setDescription("I would like to adopt");
		entityManager.persist(request);

		assertThrows(IllegalOperationException.class, () -> userService.deleteUser(adopter.getId()));
		assertNotNull(entityManager.find(UserEntity.class, adopter.getId()));
	}

	@Test
	void testDeleteUserWithOngoingAdoption() {
		AdopterEntity adopter = persistAdopter();
		AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
		adoption.setAdopter(adopter);
		adoption.setPet(persistPet());
		adoption.setStatus("IN_PROGRESS");
		entityManager.persist(adoption);

		assertThrows(IllegalOperationException.class, () -> userService.deleteUser(adopter.getId()));
	}

	@Test
	void testDeleteUserWithActiveTrialCohabitation() {
		AdopterEntity adopter = persistAdopter();
		TrialCohabitationEntity trial = factory.manufacturePojo(TrialCohabitationEntity.class);
		trial.setAdopter(adopter);
		trial.setStatus("IN_PROGRESS");
		entityManager.persist(trial);

		assertThrows(IllegalOperationException.class, () -> userService.deleteUser(adopter.getId()));
	}

	@Test
	void testDeleteUserWithMessages() {
		UserEntity sender = userList.get(0);
		UserEntity receiver = userList.get(1);
		MessageEntity message = new MessageEntity();
		message.setSendUser(sender);
		message.setReceivesUser(receiver);
		message.setMessage("Hello");
		message.setDate(new Date());
		message.setTime(new Date());
		message.setIsRead(false);
		entityManager.persist(message);

		assertThrows(IllegalOperationException.class, () -> userService.deleteUser(sender.getId()));
		assertThrows(IllegalOperationException.class, () -> userService.deleteUser(receiver.getId()));
	}
}
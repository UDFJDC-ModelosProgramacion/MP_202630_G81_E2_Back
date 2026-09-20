package co.edu.udistrital.mdp.pets.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(ShelterService.class)
class ShelterServiceTest {

	@Autowired
	private ShelterService shelterService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<ShelterEntity> shelterList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from UserEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ShelterEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			ShelterEntity shelterEntity = factory.manufacturePojo(ShelterEntity.class);
			entityManager.persist(shelterEntity);
			shelterList.add(shelterEntity);

			UserEntity userEntity = factory.manufacturePojo(UserEntity.class);
			userEntity.setShelter(shelterEntity);
			entityManager.persist(userEntity);
		}
	}

	private ShelterEntity newValidShelter() {
		return factory.manufacturePojo(ShelterEntity.class);
	}

	@Test
	void testCreateShelter() throws IllegalOperationException {
		ShelterEntity newEntity = newValidShelter();
		UserEntity user = factory.manufacturePojo(UserEntity.class);
		List<UserEntity> users = new ArrayList<>();
		users.add(user);
		newEntity.setUsers(users);

		ShelterEntity result = shelterService.createShelter(newEntity);

		assertNotNull(result);
		ShelterEntity entity = entityManager.find(ShelterEntity.class, result.getId());
		assertEquals(newEntity.getName(), entity.getName());
		assertEquals(newEntity.getNit(), entity.getNit());
	}

	@Test
	void testCreateShelterWithNullName() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity newEntity = newValidShelter();
			newEntity.setName(null);
			List<UserEntity> users = new ArrayList<>();
			users.add(factory.manufacturePojo(UserEntity.class));
			newEntity.setUsers(users);
			shelterService.createShelter(newEntity);
		});
	}

	@Test
	void testCreateShelterWithDuplicatedNit() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity newEntity = newValidShelter();
			newEntity.setNit(shelterList.get(0).getNit());
			List<UserEntity> users = new ArrayList<>();
			users.add(factory.manufacturePojo(UserEntity.class));
			newEntity.setUsers(users);
			shelterService.createShelter(newEntity);
		});
	}

	@Test
	void testCreateDuplicatedShelter() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity existing = shelterList.get(0);
			ShelterEntity newEntity = newValidShelter();
			newEntity.setName(existing.getName());
			newEntity.setCity(existing.getCity());
			newEntity.setLocation(existing.getLocation());
			List<UserEntity> users = new ArrayList<>();
			users.add(factory.manufacturePojo(UserEntity.class));
			newEntity.setUsers(users);
			shelterService.createShelter(newEntity);
		});
	}

	@Test
	void testCreateShelterWithoutUsers() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity newEntity = newValidShelter();
			newEntity.setUsers(new ArrayList<>());
			shelterService.createShelter(newEntity);
		});
	}

	@Test
	void testReadShelter() {
		List<ShelterEntity> list = shelterService.readShelter();
		assertEquals(shelterList.size(), list.size());
	}

	@Test
	void testReadShelterEmpty() {
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from UserEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ShelterEntity").executeUpdate();
		List<ShelterEntity> list = shelterService.readShelter();
		assertTrue(list.isEmpty());
	}

	@Test
	void testReadShelterFilteredByCityAndLocation() {
		ShelterEntity target = shelterList.get(0);
		List<ShelterEntity> list = shelterService.readShelter(target.getCity(), target.getLocation());
		assertTrue(list.stream().anyMatch(s -> s.getId().equals(target.getId())));

		List<ShelterEntity> emptyList = shelterService.readShelter(target.getCity(), "unmatched-location-xyz");
		assertTrue(emptyList.isEmpty());
	}

	@Test
	void testReadAllSheltersById() throws EntityNotFoundException, IllegalOperationException {
		ShelterEntity entity = shelterList.get(0);
		ShelterEntity result = shelterService.readAllShelters(entity.getId());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testReadAllSheltersInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			shelterService.readAllShelters(0L);
		});
	}

	@Test
	void testReadAllSheltersCombinedFiltersNoMatch() {
		assertThrows(EntityNotFoundException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			shelterService.readAllShelters(entity.getId(), "unmatched-name-xyz", entity.getNit());
		});
	}

	@Test
	void testReadNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			shelterService.readAllShelters(1000L);
		});
	}

	@Test
	void testUpdateShelter() throws EntityNotFoundException, IllegalOperationException {
		ShelterEntity entity = shelterList.get(0);
		ShelterEntity pojoEntity = factory.manufacturePojo(ShelterEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setNit(entity.getNit());

		shelterService.updateShelter(entity.getId(), pojoEntity);

		ShelterEntity resp = entityManager.find(ShelterEntity.class, entity.getId());
		assertEquals(pojoEntity.getName(), resp.getName());
		assertEquals(entity.getNit(), resp.getNit());
	}

	@Test
	void testUpdateShelterInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			ShelterEntity pojoEntity = factory.manufacturePojo(ShelterEntity.class);
			shelterService.updateShelter(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateShelterChangingNit() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			ShelterEntity pojoEntity = factory.manufacturePojo(ShelterEntity.class);
			pojoEntity.setId(entity.getId());
			shelterService.updateShelter(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeleteShelter() throws EntityNotFoundException, IllegalOperationException {
		ShelterEntity entity = shelterList.get(0);
		shelterService.deleteShelter(entity.getId());
		ShelterEntity deleted = entityManager.find(ShelterEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInvalidShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			shelterService.deleteShelter(1000L);
		});
	}

	@Test
	void testDeleteShelterWithPets() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			PetEntity pet = factory.manufacturePojo(PetEntity.class);
			pet.setShelter(entity);
			entityManager.persist(pet);
			shelterService.deleteShelter(entity.getId());
		});
	}
}
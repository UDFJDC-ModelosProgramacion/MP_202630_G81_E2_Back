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

import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.pets.entities.EventEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ReturnEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationRequestEntity;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
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

	// Test for createShelter
	
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

	// Test for getShelter

	@Test
	void testGetShelter() {
		List<ShelterEntity> list = shelterService.getShelter();
		assertEquals(shelterList.size(), list.size());
	}

	@Test
	void testGetShelterEmpty() {
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from UserEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ShelterEntity").executeUpdate();
		List<ShelterEntity> list = shelterService.getShelter();
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetShelterFilteredByCityAndLocation() {
		ShelterEntity target = shelterList.get(0);
		List<ShelterEntity> list = shelterService.getShelter(target.getCity(), target.getLocation());
		assertTrue(list.stream().anyMatch(s -> s.getId().equals(target.getId())));

		List<ShelterEntity> emptyList = shelterService.getShelter(target.getCity(), "unmatched-location-xyz");
		assertTrue(emptyList.isEmpty());
	}

	// Test for getAllShelters

	@Test
	void testGetAllSheltersById() throws EntityNotFoundException, IllegalOperationException {
		ShelterEntity entity = shelterList.get(0);
		ShelterEntity result = shelterService.getAllShelters(entity.getId());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testGetAllSheltersInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			shelterService.getAllShelters(0L);
		});
	}

	@Test
	void testGetAllSheltersCombinedFiltersNoMatch() {
		assertThrows(EntityNotFoundException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			shelterService.getAllShelters(entity.getId(), "unmatched-name-xyz", entity.getNit());
		});
	}

	@Test
	void testGetNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			shelterService.getAllShelters(1000L);
		});
	}

	// TEst for updateShelter
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

	// Test for deleteShelter
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

	@Test
	void testDeleteShelterWithActiveAdoptionRequest() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			AdoptionRequestEntity request = factory.manufacturePojo(AdoptionRequestEntity.class);
			request.setShelter(entity);
			entityManager.persist(request);
			shelterService.deleteShelter(entity.getId());
		});
	}

	@Test
	void testDeleteShelterWithActiveCohabitationRequest() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			TrialCohabitationRequestEntity request = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			request.setShelter(entity);
			entityManager.persist(request);
			shelterService.deleteShelter(entity.getId());
		});
	}

	@Test
	void testDeleteShelterWithActiveTrialCohabitation() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			TrialCohabitationEntity trial = factory.manufacturePojo(TrialCohabitationEntity.class);
			trial.setShelter(entity);
			entityManager.persist(trial);
			shelterService.deleteShelter(entity.getId());
		});
	}

	@Test
	void testDeleteShelterWithOngoingAdoption() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
			adoption.setShelter(entity);
			adoption.setReturnAfterAdoption(null); // sin cerrar -> proceso activo
			entityManager.persist(adoption);
			shelterService.deleteShelter(entity.getId());
		});
	}

	@Test
	void testDeleteShelterWithCompletedAdoptionHistory() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
			adoption.setShelter(entity);
			entityManager.persist(adoption); // returnAfterAdoption != null -> cuenta como historial
			shelterService.deleteShelter(entity.getId());
		});
	}

	@Test
	void testDeleteShelterWithEventHistory() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			EventEntity event = factory.manufacturePojo(EventEntity.class);
			event.setShelter(entity);
			entityManager.persist(event);
			shelterService.deleteShelter(entity.getId());
		});
	}

	@Test
	void testDeleteShelterWithVeterinarianHistory() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			VeterinarianEntity vet = factory.manufacturePojo(VeterinarianEntity.class);
			vet.setShelter(entity);
			entityManager.persist(vet);
			shelterService.deleteShelter(entity.getId());
		});
	}

	@Test
	void testDeleteShelterWithReturnDuringTrialHistory() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			ReturnEntity returnEntity = factory.manufacturePojo(ReturnEntity.class);
			returnEntity.setShelter(entity);
			entityManager.persist(returnEntity);
			shelterService.deleteShelter(entity.getId());
		});
	}
}
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

	private List<UserEntity> newUsers() {
		List<UserEntity> users = new ArrayList<>();
		users.add(factory.manufacturePojo(UserEntity.class));
		return users;
	}

	// Test for createShelter

	@Test
	void testCreateShelter() throws IllegalOperationException {
		ShelterEntity newEntity = newValidShelter();
		newEntity.setUsers(newUsers());

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
			newEntity.setUsers(newUsers());
			shelterService.createShelter(newEntity);
		});
	}

	@Test
	void testCreateShelterWithBlankName() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity newEntity = newValidShelter();
			newEntity.setName("   ");
			newEntity.setUsers(newUsers());
			shelterService.createShelter(newEntity);
		});
	}

	@Test
	void testCreateShelterWithNullCity() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity newEntity = newValidShelter();
			newEntity.setCity(null);
			newEntity.setUsers(newUsers());
			shelterService.createShelter(newEntity);
		});
	}

	@Test
	void testCreateShelterWithNullLocation() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity newEntity = newValidShelter();
			newEntity.setLocation(null);
			newEntity.setUsers(newUsers());
			shelterService.createShelter(newEntity);
		});
	}

	@Test
	void testCreateShelterWithNullNit() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity newEntity = newValidShelter();
			newEntity.setNit(null);
			newEntity.setUsers(newUsers());
			shelterService.createShelter(newEntity);
		});
	}

	@Test
	void testCreateShelterWithDuplicatedNit() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity newEntity = newValidShelter();
			newEntity.setNit(shelterList.get(0).getNit());
			newEntity.setUsers(newUsers());
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
			newEntity.setUsers(newUsers());
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
	void testCreateShelterWithNullUsers() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity newEntity = newValidShelter();
			newEntity.setUsers(null);
			shelterService.createShelter(newEntity);
		});
	}

	// Test for getShelters

	@Test
	void testGetShelters() {
		List<ShelterEntity> list = shelterService.getShelters();
		assertEquals(shelterList.size(), list.size());
	}

	@Test
	void testGetSheltersEmpty() {
		clearData();
		List<ShelterEntity> list = shelterService.getShelters();
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetSheltersFilteredByCityAndLocation() {
		ShelterEntity target = shelterList.get(0);
		List<ShelterEntity> list = shelterService.getShelters(target.getCity(), target.getLocation());
		assertTrue(list.stream().anyMatch(s -> s.getId().equals(target.getId())));
	}

	@Test
	void testGetSheltersFilteredByCityOnly() {
		ShelterEntity target = shelterList.get(0);
		List<ShelterEntity> list = shelterService.getShelters(target.getCity(), null);
		assertTrue(list.stream().anyMatch(s -> s.getId().equals(target.getId())));
	}

	@Test
	void testGetSheltersFilteredByLocationOnly() {
		ShelterEntity target = shelterList.get(0);
		List<ShelterEntity> list = shelterService.getShelters(null, target.getLocation());
		assertTrue(list.stream().anyMatch(s -> s.getId().equals(target.getId())));
	}

	@Test
	void testGetSheltersWithUnmatchedCity() {
		List<ShelterEntity> list = shelterService.getShelters("unmatched-city-xyz", null);
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetSheltersWithUnmatchedLocation() {
		ShelterEntity target = shelterList.get(0);
		List<ShelterEntity> list = shelterService.getShelters(target.getCity(), "unmatched-location-xyz");
		assertTrue(list.isEmpty());
	}

	// Test for getShelter

	@Test
	void testGetShelterById() throws EntityNotFoundException, IllegalOperationException {
		ShelterEntity entity = shelterList.get(0);
		ShelterEntity result = shelterService.getShelter(entity.getId());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testGetShelterInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			shelterService.getShelter(0L);
		});
	}

	@Test
	void testGetNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			shelterService.getShelter(1000L);
		});
	}

	@Test
	void testGetShelterCombinedFiltersMatch() throws EntityNotFoundException, IllegalOperationException {
		ShelterEntity entity = shelterList.get(0);
		ShelterEntity result = shelterService.getShelter(entity.getId(), entity.getName(), entity.getNit());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testGetShelterCombinedFiltersNameNoMatch() {
		assertThrows(EntityNotFoundException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			shelterService.getShelter(entity.getId(), "unmatched-name-xyz", entity.getNit());
		});
	}

	@Test
	void testGetShelterCombinedFiltersNitNoMatch() {
		assertThrows(EntityNotFoundException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			shelterService.getShelter(entity.getId(), entity.getName(), "unmatched-nit-xyz");
		});
	}

	@Test
	void testGetShelterCombinedFiltersInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			shelterService.getShelter(0L, entity.getName(), entity.getNit());
		});
	}

	// Test for updateShelter

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
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity pojoEntity = factory.manufacturePojo(ShelterEntity.class);
			shelterService.updateShelter(0L, pojoEntity);
		});
	}

	@Test
	void testUpdateShelterNullId() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity pojoEntity = factory.manufacturePojo(ShelterEntity.class);
			shelterService.updateShelter(null, pojoEntity);
		});
	}

	@Test
	void testUpdateNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			ShelterEntity pojoEntity = factory.manufacturePojo(ShelterEntity.class);
			shelterService.updateShelter(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateShelterWithNullName() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			ShelterEntity pojoEntity = factory.manufacturePojo(ShelterEntity.class);
			pojoEntity.setNit(entity.getNit());
			pojoEntity.setName(null);
			shelterService.updateShelter(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateShelterWithNullCity() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			ShelterEntity pojoEntity = factory.manufacturePojo(ShelterEntity.class);
			pojoEntity.setNit(entity.getNit());
			pojoEntity.setCity(null);
			shelterService.updateShelter(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateShelterWithNullLocation() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			ShelterEntity pojoEntity = factory.manufacturePojo(ShelterEntity.class);
			pojoEntity.setNit(entity.getNit());
			pojoEntity.setLocation(null);
			shelterService.updateShelter(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateShelterWithNullNit() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			ShelterEntity pojoEntity = factory.manufacturePojo(ShelterEntity.class);
			pojoEntity.setNit(null);
			shelterService.updateShelter(entity.getId(), pojoEntity);
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
	void testUpdateShelterDuplicated() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity entity = shelterList.get(0);
			ShelterEntity other = shelterList.get(1);
			ShelterEntity pojoEntity = factory.manufacturePojo(ShelterEntity.class);
			pojoEntity.setNit(entity.getNit());
			pojoEntity.setName(other.getName());
			pojoEntity.setCity(other.getCity());
			pojoEntity.setLocation(other.getLocation());
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
	void testDeleteShelterInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			shelterService.deleteShelter(0L);
		});
	}

	@Test
	void testDeleteShelterNullId() {
		assertThrows(IllegalOperationException.class, () -> {
			shelterService.deleteShelter(null);
		});
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
			entityManager.flush();
			entityManager.clear();
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
			entityManager.flush();
			entityManager.clear();
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
			entityManager.flush();
			entityManager.clear();
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
			entityManager.flush();
			entityManager.clear();
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
			entity.getVeterinarians().add(vet);
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
			entityManager.flush();
			entityManager.clear();
			shelterService.deleteShelter(entity.getId());
		});
	}
}
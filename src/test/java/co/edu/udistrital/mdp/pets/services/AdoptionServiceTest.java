package co.edu.udistrital.mdp.pets.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
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
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(AdoptionService.class)
class AdoptionServiceTest {

	@Autowired
	private AdoptionService adoptionService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<AdoptionEntity> adoptionList = new ArrayList<>();
	private List<PetEntity> petList = new ArrayList<>();
	private List<ShelterEntity> shelterList = new ArrayList<>();
	private List<AdopterEntity> adopterList = new ArrayList<>();
	private List<AdoptionRequestEntity> requestList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionRequestEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ShelterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			ShelterEntity shelterEntity = factory.manufacturePojo(ShelterEntity.class);
			entityManager.persist(shelterEntity);
			shelterList.add(shelterEntity);
		}
		for (int i = 0; i < 4; i++) {
			PetEntity petEntity = factory.manufacturePojo(PetEntity.class);
			petEntity.setShelter(shelterList.get(0));
			entityManager.persist(petEntity);
			petList.add(petEntity);
		}
		for (int i = 0; i < 3; i++) {
			AdopterEntity adopterEntity = factory.manufacturePojo(AdopterEntity.class);
			adopterEntity.setShelter(shelterList.get(0));
			entityManager.persist(adopterEntity);
			adopterList.add(adopterEntity);
		}
		// solicitudes aprobadas, cada una para una mascota distinta y sin usar todavía
		for (int i = 0; i < 3; i++) {
			AdoptionRequestEntity requestEntity = factory.manufacturePojo(AdoptionRequestEntity.class);
			requestEntity.setStatus("APPROVED");
			requestEntity.setPet(petList.get(i));
			requestEntity.setShelter(shelterList.get(0));
			requestEntity.setAdopter(adopterList.get(0));
			entityManager.persist(requestEntity);
			requestList.add(requestEntity);
		}
		// una adopción ya existente, usando la solicitud en la posición 0
		AdoptionEntity adoptionEntity = factory.manufacturePojo(AdoptionEntity.class);
		adoptionEntity.setStatus("ACTIVE");
		adoptionEntity.setPet(petList.get(0));
		adoptionEntity.setShelter(shelterList.get(0));
		adoptionEntity.setAdopter(adopterList.get(0));
		adoptionEntity.setAdoptionRequest(requestList.get(0));
		entityManager.persist(adoptionEntity);
		adoptionList.add(adoptionEntity);
	}

	@Test
	void testCreateAdoption() throws EntityNotFoundException, IllegalOperationException {
		AdoptionEntity newEntity = factory.manufacturePojo(AdoptionEntity.class);
		newEntity.setStatus("ACTIVE");
		newEntity.setPet(petList.get(1));
		newEntity.setShelter(shelterList.get(0));
		newEntity.setAdopter(adopterList.get(0));
		newEntity.setAdoptionRequest(requestList.get(1));

		AdoptionEntity result = adoptionService.createAdoption(newEntity);

		assertNotNull(result);
		AdoptionEntity entity = entityManager.find(AdoptionEntity.class, result.getId());
		assertEquals(newEntity.getStatus(), entity.getStatus());
		assertEquals(newEntity.getImportantNotes(), entity.getImportantNotes());
		assertEquals(newEntity.getPet().getId(), entity.getPet().getId());
		assertEquals(newEntity.getAdoptionRequest().getId(), entity.getAdoptionRequest().getId());
	}

	@Test
	void testCreateAdoptionWithNoValidStatus() {
		assertThrows(IllegalOperationException.class, () -> {
			AdoptionEntity newEntity = factory.manufacturePojo(AdoptionEntity.class);
			newEntity.setStatus("");
			newEntity.setPet(petList.get(1));
			newEntity.setShelter(shelterList.get(0));
			newEntity.setAdopter(adopterList.get(0));
			newEntity.setAdoptionRequest(requestList.get(1));
			adoptionService.createAdoption(newEntity);
		});
	}

	@Test
	void testCreateAdoptionWithInvalidPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			AdoptionEntity newEntity = factory.manufacturePojo(AdoptionEntity.class);
			newEntity.setStatus("ACTIVE");
			PetEntity pet = new PetEntity();
			pet.setId(0L);
			newEntity.setPet(pet);
			newEntity.setShelter(shelterList.get(0));
			newEntity.setAdopter(adopterList.get(0));
			newEntity.setAdoptionRequest(requestList.get(1));
			adoptionService.createAdoption(newEntity);
		});
	}

	@Test
	void testCreateAdoptionWithUnapprovedRequest() {
		assertThrows(IllegalOperationException.class, () -> {
			AdoptionEntity newEntity = factory.manufacturePojo(AdoptionEntity.class);
			newEntity.setStatus("ACTIVE");
			newEntity.setPet(petList.get(1));
			newEntity.setShelter(shelterList.get(0));
			newEntity.setAdopter(adopterList.get(0));

			AdoptionRequestEntity pendingRequest = factory.manufacturePojo(AdoptionRequestEntity.class);
			pendingRequest.setStatus("PENDING");
			pendingRequest.setPet(petList.get(1));
			pendingRequest.setShelter(shelterList.get(0));
			pendingRequest.setAdopter(adopterList.get(0));
			entityManager.persist(pendingRequest);

			newEntity.setAdoptionRequest(pendingRequest);
			adoptionService.createAdoption(newEntity);
		});
	}

	@Test
	void testCreateAdoptionWithAlreadyAdoptedPet() {
		assertThrows(IllegalOperationException.class, () -> {
			AdoptionEntity newEntity = factory.manufacturePojo(AdoptionEntity.class);
			newEntity.setStatus("ACTIVE");
			newEntity.setPet(petList.get(0)); // ya tiene adopción
			newEntity.setShelter(shelterList.get(0));
			newEntity.setAdopter(adopterList.get(0));

			AdoptionRequestEntity requestEntity = factory.manufacturePojo(AdoptionRequestEntity.class);
			requestEntity.setStatus("APPROVED");
			requestEntity.setPet(petList.get(0));
			requestEntity.setShelter(shelterList.get(0));
			requestEntity.setAdopter(adopterList.get(0));
			entityManager.persist(requestEntity);

			newEntity.setAdoptionRequest(requestEntity);
			adoptionService.createAdoption(newEntity);
		});
	}

	@Test
	void testGetAdoptions() {
		List<AdoptionEntity> list = adoptionService.getAdoptions();
		assertEquals(adoptionList.size(), list.size());
	}

	@Test
	void testGetAdoption() throws EntityNotFoundException, IllegalOperationException {
		AdoptionEntity entity = adoptionList.get(0);
		AdoptionEntity resultEntity = adoptionService.getAdoption(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
		assertEquals(entity.getStatus(), resultEntity.getStatus());
	}

	@Test
	void testGetAdoptionInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			adoptionService.getAdoption(0L);
		});
	}

	@Test
	void testGetNonExistentAdoption() {
		assertThrows(EntityNotFoundException.class, () -> {
			adoptionService.getAdoption(1000L);
		});
	}

	@Test
	void testUpdateAdoption() throws EntityNotFoundException, IllegalOperationException {
		AdoptionEntity entity = adoptionList.get(0);
		AdoptionEntity pojoEntity = factory.manufacturePojo(AdoptionEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setStatus("ACTIVE");

		adoptionService.updateAdoption(entity.getId(), pojoEntity);

		AdoptionEntity resp = entityManager.find(AdoptionEntity.class, entity.getId());
		assertEquals(pojoEntity.getStatus(), resp.getStatus());
		assertEquals(pojoEntity.getImportantNotes(), resp.getImportantNotes());
		assertEquals(entity.getPet().getId(), resp.getPet().getId());
	}

	@Test
	void testUpdateAdoptionInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			AdoptionEntity pojoEntity = factory.manufacturePojo(AdoptionEntity.class);
			pojoEntity.setId(0L);
			pojoEntity.setStatus("ACTIVE");
			adoptionService.updateAdoption(0L, pojoEntity);
		});
	}

	@Test
	void testUpdateNonExistentAdoption() {
		assertThrows(EntityNotFoundException.class, () -> {
			AdoptionEntity pojoEntity = factory.manufacturePojo(AdoptionEntity.class);
			pojoEntity.setId(1000L);
			pojoEntity.setStatus("ACTIVE");
			adoptionService.updateAdoption(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateFinishedAdoption() {
		assertThrows(IllegalOperationException.class, () -> {
			AdoptionEntity entity = adoptionList.get(0);
			entity.setStatus("FINISHED");
			entityManager.merge(entity);

			AdoptionEntity pojoEntity = factory.manufacturePojo(AdoptionEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setStatus("ACTIVE");
			adoptionService.updateAdoption(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeleteAdoption() throws EntityNotFoundException, IllegalOperationException {
		AdoptionEntity entity = adoptionList.get(0);
		adoptionService.deleteAdoption(entity.getId());
		AdoptionEntity deleted = entityManager.find(AdoptionEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteAdoptionInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			adoptionService.deleteAdoption(0L);
		});
	}

	@Test
	void testDeleteNonExistentAdoption() {
		assertThrows(EntityNotFoundException.class, () -> {
			adoptionService.deleteAdoption(1000L);
		});
	}
}
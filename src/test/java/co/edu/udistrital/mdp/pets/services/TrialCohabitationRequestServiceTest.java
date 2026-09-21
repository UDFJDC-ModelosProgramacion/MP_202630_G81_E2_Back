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
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationRequestEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(TrialCohabitationRequestService.class)
class TrialCohabitationRequestServiceTest {

	@Autowired
	private TrialCohabitationRequestService trialCohabitationRequestService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<TrialCohabitationRequestEntity> requestList = new ArrayList<>();
	private List<PetEntity> petList = new ArrayList<>();
	private List<ShelterEntity> shelterList = new ArrayList<>();
	private List<AdopterEntity> adopterList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationRequestEntity").executeUpdate();
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
		for (int i = 0; i < 3; i++) {
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
		for (int i = 0; i < 3; i++) {
			TrialCohabitationRequestEntity requestEntity =
					factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			requestEntity.setStatus("PENDING");
			requestEntity.setShelter(shelterList.get(0));
			requestEntity.setAdopter(adopterList.get(0));
			requestEntity.setPet(petList.get(1));
			entityManager.persist(requestEntity);
			requestList.add(requestEntity);

			// mantiene la asociación bidireccional en memoria para las pruebas de duplicados
			adopterList.get(0).getCohabitationRequests().add(requestEntity);
		}
	}

	@Test
	void testCreateTrialCohabitationRequest() throws EntityNotFoundException, IllegalOperationException {
		TrialCohabitationRequestEntity newEntity = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
		newEntity.setStatus("PENDING");
		newEntity.setShelter(shelterList.get(0));
		newEntity.setAdopter(adopterList.get(1));
		newEntity.setPet(petList.get(2));

		TrialCohabitationRequestEntity result =
				trialCohabitationRequestService.createTrialCohabitationRequest(newEntity);

		assertNotNull(result);
		TrialCohabitationRequestEntity entity =
				entityManager.find(TrialCohabitationRequestEntity.class, result.getId());
		assertEquals(newEntity.getStatus(), entity.getStatus());
		assertEquals(newEntity.getDescription(), entity.getDescription());
		assertEquals(newEntity.getPet().getId(), entity.getPet().getId());
		assertEquals(newEntity.getAdopter().getId(), entity.getAdopter().getId());
	}

	@Test
	void testCreateTrialCohabitationRequestWithNoValidStatus() {
		assertThrows(IllegalOperationException.class, () -> {
			TrialCohabitationRequestEntity newEntity = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			newEntity.setStatus("");
			newEntity.setShelter(shelterList.get(0));
			newEntity.setAdopter(adopterList.get(1));
			newEntity.setPet(petList.get(2));
			trialCohabitationRequestService.createTrialCohabitationRequest(newEntity);
		});
	}

	@Test
	void testCreateTrialCohabitationRequestWithInvalidShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			TrialCohabitationRequestEntity newEntity = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			newEntity.setStatus("PENDING");
			ShelterEntity shelter = new ShelterEntity();
			shelter.setId(0L);
			newEntity.setShelter(shelter);
			newEntity.setAdopter(adopterList.get(1));
			newEntity.setPet(petList.get(2));
			trialCohabitationRequestService.createTrialCohabitationRequest(newEntity);
		});
	}

	@Test
	void testCreateTrialCohabitationRequestWithInvalidPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			TrialCohabitationRequestEntity newEntity = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			newEntity.setStatus("PENDING");
			newEntity.setShelter(shelterList.get(0));
			newEntity.setAdopter(adopterList.get(1));
			PetEntity pet = new PetEntity();
			pet.setId(0L);
			newEntity.setPet(pet);
			trialCohabitationRequestService.createTrialCohabitationRequest(newEntity);
		});
	}

	@Test
	void testCreateTrialCohabitationRequestDuplicated() {
		assertThrows(IllegalOperationException.class, () -> {
			TrialCohabitationRequestEntity newEntity = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			newEntity.setStatus("PENDING");
			newEntity.setShelter(shelterList.get(0));
			newEntity.setAdopter(adopterList.get(0));
			newEntity.setPet(petList.get(1)); // mismo adopter + misma mascota que insertData
			trialCohabitationRequestService.createTrialCohabitationRequest(newEntity);
		});
	}

	@Test
	void testGetTrialCohabitationRequests() {
		List<TrialCohabitationRequestEntity> list = trialCohabitationRequestService.getTrialCohabitationRequests();
		assertEquals(requestList.size(), list.size());
	}

	@Test
	void testGetTrialCohabitationRequest() throws EntityNotFoundException, IllegalOperationException {
		TrialCohabitationRequestEntity entity = requestList.get(0);
		TrialCohabitationRequestEntity resultEntity =
				trialCohabitationRequestService.getTrialCohabitationRequest(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
		assertEquals(entity.getStatus(), resultEntity.getStatus());
	}

	@Test
	void testGetTrialCohabitationRequestInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			trialCohabitationRequestService.getTrialCohabitationRequest(0L);
		});
	}

	@Test
	void testGetNonExistentTrialCohabitationRequest() {
		assertThrows(EntityNotFoundException.class, () -> {
			trialCohabitationRequestService.getTrialCohabitationRequest(1000L);
		});
	}

	@Test
	void testUpdateTrialCohabitationRequest() throws EntityNotFoundException, IllegalOperationException {
		TrialCohabitationRequestEntity entity = requestList.get(0);
		TrialCohabitationRequestEntity pojoEntity = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setStatus("APPROVED");

		trialCohabitationRequestService.updateTrialCohabitationRequest(entity.getId(), pojoEntity);

		TrialCohabitationRequestEntity resp =
				entityManager.find(TrialCohabitationRequestEntity.class, entity.getId());
		assertEquals(pojoEntity.getStatus(), resp.getStatus());
		assertEquals(pojoEntity.getDescription(), resp.getDescription());
		assertEquals(entity.getPet().getId(), resp.getPet().getId());
	}

	@Test
	void testUpdateTrialCohabitationRequestInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			TrialCohabitationRequestEntity pojoEntity = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			pojoEntity.setId(0L);
			pojoEntity.setStatus("APPROVED");
			trialCohabitationRequestService.updateTrialCohabitationRequest(0L, pojoEntity);
		});
	}

	@Test
	void testUpdateNonExistentTrialCohabitationRequest() {
		assertThrows(EntityNotFoundException.class, () -> {
			TrialCohabitationRequestEntity pojoEntity = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			pojoEntity.setId(1000L);
			pojoEntity.setStatus("APPROVED");
			trialCohabitationRequestService.updateTrialCohabitationRequest(1000L, pojoEntity);
		});
	}

	@Test
	void testDeleteTrialCohabitationRequest() throws EntityNotFoundException, IllegalOperationException {
		TrialCohabitationRequestEntity entity = requestList.get(1);
		trialCohabitationRequestService.deleteTrialCohabitationRequest(entity.getId());
		TrialCohabitationRequestEntity deleted =
				entityManager.find(TrialCohabitationRequestEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteTrialCohabitationRequestInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			trialCohabitationRequestService.deleteTrialCohabitationRequest(0L);
		});
	}

	@Test
	void testDeleteNonExistentTrialCohabitationRequest() {
		assertThrows(EntityNotFoundException.class, () -> {
			trialCohabitationRequestService.deleteTrialCohabitationRequest(1000L);
		});
	}
}
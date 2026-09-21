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
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationRequestEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(TrialCohabitationRequestService.class)
class TrialCohabitationRequestServiceTest {

	private static final String APPROVED = "APPROVED";
	private static final Long NON_EXISTENT_ID = 1000L;
	private static final Long INVALID_ID = 0L;

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
			TrialCohabitationRequestEntity requestEntity = newRequest(TrialCohabitationRequestService.PENDING_STATUS,
					shelterList.get(0), adopterList.get(0), petList.get(1));
			entityManager.persist(requestEntity);
			requestList.add(requestEntity);

			// mantiene la asociación bidireccional en memoria para las pruebas de duplicados
			adopterList.get(0).getCohabitationRequests().add(requestEntity);
		}
	}

	private TrialCohabitationRequestEntity newRequest(String status, ShelterEntity shelter, AdopterEntity adopter,
			PetEntity pet) {
		TrialCohabitationRequestEntity request = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
		request.setStatus(status);
		request.setShelter(shelter);
		request.setAdopter(adopter);
		request.setPet(pet);
		return request;
	}

	/** Solicitud válida para un adoptante y una mascota que aún no tienen solicitudes pendientes. */
	private TrialCohabitationRequestEntity newValidRequest() {
		return newRequest(TrialCohabitationRequestService.PENDING_STATUS, shelterList.get(0), adopterList.get(1),
				petList.get(2));
	}

	private ShelterEntity shelterWithId(Long id) {
		ShelterEntity shelter = new ShelterEntity();
		shelter.setId(id);
		return shelter;
	}

	private AdopterEntity adopterWithId(Long id) {
		AdopterEntity adopter = new AdopterEntity();
		adopter.setId(id);
		return adopter;
	}

	private PetEntity petWithId(Long id) {
		PetEntity pet = new PetEntity();
		pet.setId(id);
		return pet;
	}

	// ---------------------------------------------------------------- create

	@Test
	void testCreateTrialCohabitationRequest() throws EntityNotFoundException, IllegalOperationException {
		TrialCohabitationRequestEntity newEntity = newValidRequest();

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
	void testCreateNullTrialCohabitationRequest() {
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.createTrialCohabitationRequest(null));
	}

	@Test
	void testCreateTrialCohabitationRequestWithNoValidStatus() {
		TrialCohabitationRequestEntity newEntity = newValidRequest();
		newEntity.setStatus("");
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.createTrialCohabitationRequest(newEntity));
	}

	@Test
	void testCreateTrialCohabitationRequestWithNullDate() {
		TrialCohabitationRequestEntity newEntity = newValidRequest();
		newEntity.setDate(null);
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.createTrialCohabitationRequest(newEntity));
	}

	@Test
	void testCreateTrialCohabitationRequestWithNullShelter() {
		TrialCohabitationRequestEntity newEntity = newValidRequest();
		newEntity.setShelter(null);
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.createTrialCohabitationRequest(newEntity));
	}

	@Test
	void testCreateTrialCohabitationRequestWithInvalidShelter() {
		TrialCohabitationRequestEntity newEntity = newValidRequest();
		newEntity.setShelter(shelterWithId(INVALID_ID));
		assertThrows(EntityNotFoundException.class,
				() -> trialCohabitationRequestService.createTrialCohabitationRequest(newEntity));
	}

	@Test
	void testCreateTrialCohabitationRequestWithNullAdopter() {
		TrialCohabitationRequestEntity newEntity = newValidRequest();
		newEntity.setAdopter(null);
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.createTrialCohabitationRequest(newEntity));
	}

	@Test
	void testCreateTrialCohabitationRequestWithInvalidAdopter() {
		TrialCohabitationRequestEntity newEntity = newValidRequest();
		newEntity.setAdopter(adopterWithId(INVALID_ID));
		assertThrows(EntityNotFoundException.class,
				() -> trialCohabitationRequestService.createTrialCohabitationRequest(newEntity));
	}

	@Test
	void testCreateTrialCohabitationRequestWithNullPet() {
		TrialCohabitationRequestEntity newEntity = newValidRequest();
		newEntity.setPet(null);
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.createTrialCohabitationRequest(newEntity));
	}

	@Test
	void testCreateTrialCohabitationRequestWithInvalidPet() {
		TrialCohabitationRequestEntity newEntity = newValidRequest();
		newEntity.setPet(petWithId(INVALID_ID));
		assertThrows(EntityNotFoundException.class,
				() -> trialCohabitationRequestService.createTrialCohabitationRequest(newEntity));
	}

	@Test
	void testCreateTrialCohabitationRequestDuplicated() {
		// mismo adopter + misma mascota que insertData
		TrialCohabitationRequestEntity newEntity = newRequest(TrialCohabitationRequestService.PENDING_STATUS,
				shelterList.get(0), adopterList.get(0), petList.get(1));
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.createTrialCohabitationRequest(newEntity));
	}

	// ------------------------------------------------------------------- get

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
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.getTrialCohabitationRequest(INVALID_ID));
	}

	@Test
	void testGetNonExistentTrialCohabitationRequest() {
		assertThrows(EntityNotFoundException.class,
				() -> trialCohabitationRequestService.getTrialCohabitationRequest(NON_EXISTENT_ID));
	}

	// ---------------------------------------------------------------- update

	@Test
	void testUpdateTrialCohabitationRequest() throws EntityNotFoundException, IllegalOperationException {
		TrialCohabitationRequestEntity entity = requestList.get(0);
		TrialCohabitationRequestEntity update = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
		update.setStatus(APPROVED);

		trialCohabitationRequestService.updateTrialCohabitationRequest(entity.getId(), update);

		TrialCohabitationRequestEntity resp =
				entityManager.find(TrialCohabitationRequestEntity.class, entity.getId());
		assertEquals(update.getStatus(), resp.getStatus());
		assertEquals(update.getDescription(), resp.getDescription());
		assertEquals(petList.get(1).getId(), resp.getPet().getId());
		assertEquals(adopterList.get(0).getId(), resp.getAdopter().getId());
		assertEquals(shelterList.get(0).getId(), resp.getShelter().getId());
	}

	@Test
	void testUpdateTrialCohabitationRequestInvalidId() {
		TrialCohabitationRequestEntity update = newValidRequest();
		update.setStatus(APPROVED);
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.updateTrialCohabitationRequest(INVALID_ID, update));
	}

	@Test
	void testUpdateNonExistentTrialCohabitationRequest() {
		TrialCohabitationRequestEntity update = newValidRequest();
		update.setStatus(APPROVED);
		assertThrows(EntityNotFoundException.class,
				() -> trialCohabitationRequestService.updateTrialCohabitationRequest(NON_EXISTENT_ID, update));
	}

	@Test
	void testUpdateTrialCohabitationRequestWithNullEntity() {
		Long id = requestList.get(0).getId();
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.updateTrialCohabitationRequest(id, null));
	}

	@Test
	void testUpdateTrialCohabitationRequestWithBlankStatus() {
		Long id = requestList.get(0).getId();
		TrialCohabitationRequestEntity update = newValidRequest();
		update.setStatus(" ");
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.updateTrialCohabitationRequest(id, update));
	}

	@Test
	void testUpdateTrialCohabitationRequestWithTrialCohabitation() {
		TrialCohabitationRequestEntity entity = requestList.get(0);
		entity.setTrialCohabitation(new TrialCohabitationEntity());
		Long id = entity.getId();
		TrialCohabitationRequestEntity update = newValidRequest();
		update.setStatus(APPROVED);
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.updateTrialCohabitationRequest(id, update));
	}

	// ---------------------------------------------------------------- delete

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
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.deleteTrialCohabitationRequest(INVALID_ID));
	}

	@Test
	void testDeleteNonExistentTrialCohabitationRequest() {
		assertThrows(EntityNotFoundException.class,
				() -> trialCohabitationRequestService.deleteTrialCohabitationRequest(NON_EXISTENT_ID));
	}

	@Test
	void testDeleteTrialCohabitationRequestWithTrialCohabitation() {
		TrialCohabitationRequestEntity entity = requestList.get(1);
		entity.setTrialCohabitation(new TrialCohabitationEntity());
		Long id = entity.getId();
		assertThrows(IllegalOperationException.class,
				() -> trialCohabitationRequestService.deleteTrialCohabitationRequest(id));
	}
}
package co.edu.udistrital.mdp.ZZZ.services;

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

import co.edu.udistrital.mdp.ZZZ.entities.AdopterEntity;
import co.edu.udistrital.mdp.ZZZ.entities.PetEntity;
import co.edu.udistrital.mdp.ZZZ.entities.ShelterEntity;
import co.edu.udistrital.mdp.ZZZ.entities.TrialCohabitationEntity;
import co.edu.udistrital.mdp.ZZZ.entities.TrialCohabitationRequestEntity;
import co.edu.udistrital.mdp.ZZZ.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.ZZZ.exceptions.IllegalOperationException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(TrialCohabitationService.class)
class TrialCohabitationServiceTest {

	private static final long DAY = 86400000L;

	@Autowired
	private TrialCohabitationService trialCohabitationService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<TrialCohabitationEntity> trialList = new ArrayList<>();
	private List<PetEntity> petList = new ArrayList<>();
	private List<AdopterEntity> adopterList = new ArrayList<>();
	private List<ShelterEntity> shelterList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ReturnEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionRequestEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationRequestEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from VeterinarianEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ShelterEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			ShelterEntity shelterEntity = factory.manufacturePojo(ShelterEntity.class);
			entityManager.persist(shelterEntity);
			shelterList.add(shelterEntity);

			PetEntity petEntity = factory.manufacturePojo(PetEntity.class);
			entityManager.persist(petEntity);
			petList.add(petEntity);

			AdopterEntity adopterEntity = factory.manufacturePojo(AdopterEntity.class);
			entityManager.persist(adopterEntity);
			adopterList.add(adopterEntity);

			TrialCohabitationRequestEntity request = buildFreshRequest(petEntity, adopterEntity, shelterEntity);

			TrialCohabitationEntity trial = buildTrial(request, adopterEntity, shelterEntity, "PENDING");
			entityManager.persist(trial);
			trialList.add(trial);
		}
	}

	private Date futureDate() {
		return new Date(System.currentTimeMillis() + 30 * DAY);
	}

	private Date pastDate() {
		return new Date(System.currentTimeMillis() - 30 * DAY);
	}

	private TrialCohabitationRequestEntity buildFreshRequest(PetEntity pet, AdopterEntity adopter,
			ShelterEntity shelter) {
		TrialCohabitationRequestEntity request = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
		request.setPet(pet);
		request.setAdopter(adopter);
		request.setShelter(shelter);
		entityManager.persist(request);
		return request;
	}

	private TrialCohabitationEntity buildTrial(TrialCohabitationRequestEntity request, AdopterEntity adopter,
			ShelterEntity shelter, String status) {
		TrialCohabitationEntity trial = factory.manufacturePojo(TrialCohabitationEntity.class);
		trial.setStartDate(futureDate());
		trial.setShelter(shelter);
		trial.setAdopter(adopter);
		trial.setTrialCohabitationRequest(request);
		trial.setStatus(status);
		return trial;
	}

	@Test
	void testCreateTrial() throws EntityNotFoundException, IllegalOperationException {
		ShelterEntity shelter = factory.manufacturePojo(ShelterEntity.class);
		entityManager.persist(shelter);
		PetEntity pet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(pet);
		AdopterEntity adopter = factory.manufacturePojo(AdopterEntity.class);
		entityManager.persist(adopter);

		TrialCohabitationRequestEntity request = buildFreshRequest(pet, adopter, shelter);

		TrialCohabitationEntity newTrial = buildTrial(request, adopter, shelter, null);

		TrialCohabitationEntity result = trialCohabitationService.createTrial(newTrial);

		assertNotNull(result);
		TrialCohabitationEntity persisted = entityManager.find(TrialCohabitationEntity.class, result.getId());
		assertNotNull(persisted);
		assertEquals("PENDING", persisted.getStatus());
		assertEquals(adopter.getId(), persisted.getAdopter().getId());
		assertEquals(shelter.getId(), persisted.getShelter().getId());
	}

	@Test
	void testCreateTrialWithPastStartDate() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity shelter = factory.manufacturePojo(ShelterEntity.class);
			entityManager.persist(shelter);
			PetEntity pet = factory.manufacturePojo(PetEntity.class);
			entityManager.persist(pet);
			AdopterEntity adopter = factory.manufacturePojo(AdopterEntity.class);
			entityManager.persist(adopter);

			TrialCohabitationRequestEntity request = buildFreshRequest(pet, adopter, shelter);
			TrialCohabitationEntity newTrial = buildTrial(request, adopter, shelter, "PENDING");
			newTrial.setStartDate(pastDate());
			trialCohabitationService.createTrial(newTrial);
		});
	}

	@Test
	void testCreateTrialWithNonExistentAdopter() {
		assertThrows(EntityNotFoundException.class, () -> {
			TrialCohabitationEntity newTrial = factory.manufacturePojo(TrialCohabitationEntity.class);
			newTrial.setStartDate(futureDate());
			AdopterEntity fakeAdopter = new AdopterEntity();
			fakeAdopter.setId(0L);
			newTrial.setAdopter(fakeAdopter);
			trialCohabitationService.createTrial(newTrial);
		});
	}

	@Test
	void testCreateTrialWithNonExistentPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			AdopterEntity adopter = factory.manufacturePojo(AdopterEntity.class);
			entityManager.persist(adopter);
			ShelterEntity shelter = factory.manufacturePojo(ShelterEntity.class);
			entityManager.persist(shelter);

			TrialCohabitationRequestEntity request = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			PetEntity fakePet = new PetEntity();
			fakePet.setId(0L);
			request.setPet(fakePet);
			request.setAdopter(adopter);
			request.setShelter(shelter);

			TrialCohabitationEntity newTrial = buildTrial(request, adopter, shelter, "PENDING");
			trialCohabitationService.createTrial(newTrial);
		});
	}

	@Test
	void testCreateTrialWithPetAlreadyInAnotherTrial() {
		assertThrows(IllegalOperationException.class, () -> {
			ShelterEntity shelter = factory.manufacturePojo(ShelterEntity.class);
			entityManager.persist(shelter);
			AdopterEntity adopter = factory.manufacturePojo(AdopterEntity.class);
			entityManager.persist(adopter);

			TrialCohabitationRequestEntity existingRequest = buildFreshRequest(petList.get(0), adopter, shelter);
			TrialCohabitationEntity existingTrial = buildTrial(existingRequest, adopter, shelter, "IN_PROGRESS");
			entityManager.persist(existingTrial);

			TrialCohabitationRequestEntity newRequest = buildFreshRequest(petList.get(0), adopter, shelter);
			TrialCohabitationEntity newTrial = buildTrial(newRequest, adopter, shelter, "PENDING");
			trialCohabitationService.createTrial(newTrial);
		});
	}

	@Test
	void testReadAllTrials() {
		List<TrialCohabitationEntity> list = trialCohabitationService.readAllTrials();
		assertEquals(trialList.size(), list.size());
	}

	@Test
	void testReadAllTrialsEmpty() {
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationEntity").executeUpdate();
		List<TrialCohabitationEntity> list = trialCohabitationService.readAllTrials();
		assertTrue(list.isEmpty());
	}

	@Test
	void testReadAllTrialsFilteredByStatus() throws IllegalOperationException {
		List<TrialCohabitationEntity> list = trialCohabitationService.readAllTrials("PENDING", null, null);
		assertEquals(trialList.size(), list.size());
		List<TrialCohabitationEntity> emptyList = trialCohabitationService.readAllTrials("IN_PROGRESS", null, null);
		assertTrue(emptyList.isEmpty());
	}

	@Test
	void testReadAllTrialsFilteredByDateRange() throws IllegalOperationException {
		Date ref = trialList.get(0).getStartDate();
		List<TrialCohabitationEntity> list = trialCohabitationService.readAllTrials(null, ref, ref);
		assertNotNull(list);
		assertTrue(list.stream().anyMatch(t -> t.getId().equals(trialList.get(0).getId())));
	}

	@Test
	void testReadTrial() throws EntityNotFoundException, IllegalOperationException {
		TrialCohabitationEntity entity = trialList.get(0);
		TrialCohabitationEntity resultEntity = trialCohabitationService.readTrial(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
	}

	@Test
	void testReadInvalidTrialId() {
		assertThrows(IllegalOperationException.class, () -> {
			trialCohabitationService.readTrial(0L);
		});
	}

	@Test
	void testReadNonExistentTrial() {
		assertThrows(EntityNotFoundException.class, () -> {
			trialCohabitationService.readTrial(1000L);
		});
	}

	@Test
	void testUpdateTrial() throws EntityNotFoundException, IllegalOperationException {
		TrialCohabitationEntity entity = trialList.get(1);
		TrialCohabitationEntity pojoEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
		pojoEntity.setStartDate(futureDate());
		pojoEntity.setEndDate(new Date(System.currentTimeMillis() + 60 * DAY));
		pojoEntity.setStatus("IN_PROGRESS");
		pojoEntity.setObservations("Observacion actualizada");

		trialCohabitationService.updateTrial(entity.getId(), pojoEntity);

		TrialCohabitationEntity resp = entityManager.find(TrialCohabitationEntity.class, entity.getId());
		assertEquals("IN_PROGRESS", resp.getStatus());
		assertEquals("Observacion actualizada", resp.getObservations());
	}

	@Test
	void testUpdateTrialInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			TrialCohabitationEntity pojoEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
			pojoEntity.setStatus("PENDING");
			trialCohabitationService.updateTrial(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateFinalizedTrialToInProgress() {
		assertThrows(IllegalOperationException.class, () -> {
			TrialCohabitationEntity entity = trialList.get(0);
			entity.setStatus("FINALIZED");
			entityManager.flush();

			TrialCohabitationEntity pojoEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
			pojoEntity.setStartDate(futureDate());
			pojoEntity.setEndDate(new Date(System.currentTimeMillis() + 60 * DAY));
			pojoEntity.setStatus("IN_PROGRESS");
			pojoEntity.setObservations("Intento de actualizacion");
			trialCohabitationService.updateTrial(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeletePendingTrial() throws EntityNotFoundException, IllegalOperationException {
		TrialCohabitationEntity entity = trialList.get(1);
		trialCohabitationService.deleteTrial(entity.getId());
		TrialCohabitationEntity deleted = entityManager.find(TrialCohabitationEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInProgressTrial() {
		assertThrows(IllegalOperationException.class, () -> {
			TrialCohabitationEntity entity = trialList.get(2);
			entity.setStatus("IN_PROGRESS");
			entityManager.flush();
			trialCohabitationService.deleteTrial(entity.getId());
		});
	}

	@Test
	void testDeleteInvalidTrial() {
		assertThrows(EntityNotFoundException.class, () -> {
			trialCohabitationService.deleteTrial(1000L);
		});
	}
}
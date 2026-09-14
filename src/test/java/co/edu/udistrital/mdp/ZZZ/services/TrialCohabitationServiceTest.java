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

	@Autowired
	private TrialCohabitationService trialCohabitationService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<TrialCohabitationEntity> trialList = new ArrayList<>();
	private List<TrialCohabitationRequestEntity> requestList = new ArrayList<>();
	private List<AdopterEntity> adopterList = new ArrayList<>();
	private List<PetEntity> petList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationRequestEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			PetEntity petEntity = factory.manufacturePojo(PetEntity.class);
			entityManager.persist(petEntity);
			petList.add(petEntity);

			AdopterEntity adopterEntity = factory.manufacturePojo(AdopterEntity.class);
			entityManager.persist(adopterEntity);
			adopterList.add(adopterEntity);

			TrialCohabitationRequestEntity requestEntity = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			requestEntity.setPet(petEntity);
			requestEntity.setAdopter(adopterEntity);
			entityManager.persist(requestEntity);
			requestList.add(requestEntity);

			TrialCohabitationEntity trialEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
			trialEntity.setStartDate(new Date(System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000)));
			trialEntity.setEndDate(new Date(System.currentTimeMillis() + (20L * 24 * 60 * 60 * 1000)));
			trialEntity.setStatus("PENDING");
			trialEntity.setAdopter(adopterEntity);
			trialEntity.setTrialCohabitationRequest(requestEntity);
			entityManager.persist(trialEntity);
			trialList.add(trialEntity);
		}
	}

	@Test
	void testCreateTrialCohabitation() throws EntityNotFoundException, IllegalOperationException {
		TrialCohabitationRequestEntity newRequest = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
		newRequest.setPet(petList.get(0));
		newRequest.setAdopter(adopterList.get(0));
		entityManager.persist(newRequest);

		TrialCohabitationEntity newEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
		newEntity.setStartDate(new Date(System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000)));
		newEntity.setEndDate(null);
		newEntity.setStatus(null);
		newEntity.setAdopter(adopterList.get(0));
		newEntity.setTrialCohabitationRequest(newRequest);

		TrialCohabitationEntity result = trialCohabitationService.createTrialCohabitation(newEntity);

		assertNotNull(result);
		assertEquals("PENDING", result.getStatus());
		assertEquals(adopterList.get(0).getId(), result.getAdopter().getId());
	}

	@Test
	void testCreateTrialWithNullStartDate() {
		assertThrows(IllegalOperationException.class, () -> {
			TrialCohabitationEntity newEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
			newEntity.setStartDate(null);
			newEntity.setAdopter(adopterList.get(0));
			newEntity.setTrialCohabitationRequest(requestList.get(0));
			trialCohabitationService.createTrialCohabitation(newEntity);
		});
	}

	@Test
	void testCreateTrialWithPastStartDate() {
		assertThrows(IllegalOperationException.class, () -> {
			TrialCohabitationEntity newEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
			newEntity.setStartDate(new Date(System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000)));
			newEntity.setAdopter(adopterList.get(0));
			newEntity.setTrialCohabitationRequest(requestList.get(0));
			trialCohabitationService.createTrialCohabitation(newEntity);
		});
	}

	@Test
	void testCreateTrialWithNonExistentAdopter() {
		assertThrows(EntityNotFoundException.class, () -> {
			TrialCohabitationEntity newEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
			newEntity.setStartDate(new Date(System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000)));
			AdopterEntity fakeAdopter = new AdopterEntity();
			fakeAdopter.setId(0L);
			newEntity.setAdopter(fakeAdopter);
			newEntity.setTrialCohabitationRequest(requestList.get(0));
			trialCohabitationService.createTrialCohabitation(newEntity);
		});
	}

	@Test
	void testCreateTrialWithNonExistentPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			PetEntity fakePet = new PetEntity();
			fakePet.setId(0L);
			TrialCohabitationRequestEntity request = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			request.setPet(fakePet);
			request.setAdopter(adopterList.get(0));

			TrialCohabitationEntity newEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
			newEntity.setStartDate(new Date(System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000)));
			newEntity.setAdopter(adopterList.get(0));
			newEntity.setTrialCohabitationRequest(request);
			trialCohabitationService.createTrialCohabitation(newEntity);
		});
	}

	@Test
	void testCreateTrialWithPetInOngoingTrial() {
		assertThrows(IllegalOperationException.class, () -> {
			TrialCohabitationRequestEntity ongoingRequest = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			ongoingRequest.setPet(petList.get(0));
			ongoingRequest.setAdopter(adopterList.get(1));
			entityManager.persist(ongoingRequest);

			TrialCohabitationEntity ongoing = factory.manufacturePojo(TrialCohabitationEntity.class);
			ongoing.setStartDate(new Date(System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000)));
			ongoing.setEndDate(new Date(System.currentTimeMillis() + (20L * 24 * 60 * 60 * 1000)));
			ongoing.setStatus("IN_PROGRESS");
			ongoing.setAdopter(adopterList.get(1));
			ongoing.setTrialCohabitationRequest(ongoingRequest);
			entityManager.persist(ongoing);
			entityManager.flush();

			TrialCohabitationRequestEntity newRequest = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			newRequest.setPet(petList.get(0));
			newRequest.setAdopter(adopterList.get(2));
			entityManager.persist(newRequest);

			TrialCohabitationEntity newEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
			newEntity.setStartDate(new Date(System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000)));
			newEntity.setAdopter(adopterList.get(2));
			newEntity.setTrialCohabitationRequest(newRequest);
			trialCohabitationService.createTrialCohabitation(newEntity);
		});
	}

	@Test
	void testGetTrials() {
		List<TrialCohabitationEntity> list = trialCohabitationService.getTrials();
		assertEquals(trialList.size(), list.size());
	}

	@Test
	void testGetTrialsEmpty() {
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationEntity").executeUpdate();
		List<TrialCohabitationEntity> list = trialCohabitationService.getTrials();
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetTrialsFilteredByStatus() {
		List<TrialCohabitationEntity> list = trialCohabitationService.getTrials("PENDING", null, null);
		assertEquals(trialList.size(), list.size());
	}

	@Test
	void testGetTrialsFilteredByStatusNoMatch() {
		List<TrialCohabitationEntity> list = trialCohabitationService.getTrials("FINALIZED", null, null);
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetTrial() throws EntityNotFoundException, IllegalOperationException {
		TrialCohabitationEntity entity = trialList.get(0);
		TrialCohabitationEntity resultEntity = trialCohabitationService.getTrialCohabitation(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
	}

	@Test
	void testGetInvalidTrialId() {
		assertThrows(IllegalOperationException.class, () -> {
			trialCohabitationService.getTrialCohabitation(0L);
		});
	}

	@Test
	void testGetNonExistentTrial() {
		assertThrows(EntityNotFoundException.class, () -> {
			trialCohabitationService.getTrialCohabitation(1000L);
		});
	}

	@Test
	void testUpdateTrial() throws EntityNotFoundException, IllegalOperationException {
		TrialCohabitationEntity entity = trialList.get(0);
		TrialCohabitationEntity pojoEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setStartDate(new Date(System.currentTimeMillis() + (15L * 24 * 60 * 60 * 1000)));
		pojoEntity.setEndDate(new Date(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)));
		pojoEntity.setStatus("IN_PROGRESS");
		pojoEntity.setObservations("Periodo de convivencia actualizado");

		trialCohabitationService.updateTrialCohabitation(entity.getId(), pojoEntity);

		TrialCohabitationEntity resp = entityManager.find(TrialCohabitationEntity.class, entity.getId());
		assertEquals("IN_PROGRESS", resp.getStatus());
		assertEquals(pojoEntity.getObservations(), resp.getObservations());
	}

	@Test
	void testUpdateTrialInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			TrialCohabitationEntity pojoEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
			pojoEntity.setId(1000L);
			trialCohabitationService.updateTrialCohabitation(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateTrialWithNullStatus() {
		assertThrows(IllegalOperationException.class, () -> {
			TrialCohabitationEntity entity = trialList.get(0);
			TrialCohabitationEntity pojoEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setStartDate(entity.getStartDate());
			pojoEntity.setEndDate(entity.getEndDate());
			pojoEntity.setStatus(null);
			trialCohabitationService.updateTrialCohabitation(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateFinalizedTrialBackToInProgress() {
		assertThrows(IllegalOperationException.class, () -> {
			TrialCohabitationEntity entity = trialList.get(0);
			entity.setStatus("FINALIZED");
			entityManager.persist(entity);
			entityManager.flush();

			TrialCohabitationEntity pojoEntity = factory.manufacturePojo(TrialCohabitationEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setStartDate(entity.getStartDate());
			pojoEntity.setEndDate(entity.getEndDate());
			pojoEntity.setStatus("IN_PROGRESS");
			trialCohabitationService.updateTrialCohabitation(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeletePendingTrial() throws EntityNotFoundException, IllegalOperationException {
		TrialCohabitationEntity entity = trialList.get(1);
		trialCohabitationService.deleteTrialCohabitation(entity.getId());
		TrialCohabitationEntity deleted = entityManager.find(TrialCohabitationEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInvalidTrial() {
		assertThrows(EntityNotFoundException.class, () -> {
			trialCohabitationService.deleteTrialCohabitation(1000L);
		});
	}

	@Test
	void testDeleteTrialInProgress() {
		assertThrows(IllegalOperationException.class, () -> {
			TrialCohabitationEntity entity = trialList.get(0);
			entity.setStatus("IN_PROGRESS");
			entityManager.persist(entity);
			entityManager.flush();
			trialCohabitationService.deleteTrialCohabitation(entity.getId());
		});
	}

	@Test
	void testDeleteFinalizedTrial() {
		assertThrows(IllegalOperationException.class, () -> {
			TrialCohabitationEntity entity = trialList.get(0);
			entity.setStatus("FINALIZED");
			entityManager.persist(entity);
			entityManager.flush();
			trialCohabitationService.deleteTrialCohabitation(entity.getId());
		});
	}
}
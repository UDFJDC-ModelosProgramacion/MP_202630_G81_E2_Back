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
import co.edu.udistrital.mdp.ZZZ.entities.AdoptionEntity;
import co.edu.udistrital.mdp.ZZZ.entities.FollowUpEntity;
import co.edu.udistrital.mdp.ZZZ.entities.PetEntity;
import co.edu.udistrital.mdp.ZZZ.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.ZZZ.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.ZZZ.exceptions.IllegalOperationException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(FollowUpService.class)
class FollowUpServiceTest {

	@Autowired
	private FollowUpService followUpService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<FollowUpEntity> followUpList = new ArrayList<>();
	private List<AdoptionEntity> adoptionList = new ArrayList<>();
	private List<AdopterEntity> adopterList = new ArrayList<>();
	private List<PetEntity> petList = new ArrayList<>();
	private List<VeterinarianEntity> veterinarianList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from FollowUpEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from VeterinarianEntity").executeUpdate();
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

			VeterinarianEntity veterinarianEntity = factory.manufacturePojo(VeterinarianEntity.class);
			entityManager.persist(veterinarianEntity);
			veterinarianList.add(veterinarianEntity);

			AdoptionEntity adoptionEntity = factory.manufacturePojo(AdoptionEntity.class);
			adoptionEntity.setPet(petEntity);
			adoptionEntity.setAdopter(adopterEntity);
			adoptionEntity.setStatus("FINALIZED");
			entityManager.persist(adoptionEntity);
			adoptionList.add(adoptionEntity);

			FollowUpEntity followUpEntity = factory.manufacturePojo(FollowUpEntity.class);
			followUpEntity.setDate(new Date(System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000)));
			followUpEntity.setAdoption(adoptionEntity);
			followUpEntity.setVeterinarian(veterinarianEntity);
			entityManager.persist(followUpEntity);
			followUpList.add(followUpEntity);
		}
	}

	@Test
	void testCreateFollowUp() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity newEntity = factory.manufacturePojo(FollowUpEntity.class);
		newEntity.setDate(new Date(System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000)));
		newEntity.setAdoption(adoptionList.get(0));
		newEntity.setVeterinarian(veterinarianList.get(0));

		FollowUpEntity result = followUpService.createFollowUp(newEntity);

		assertNotNull(result);
		FollowUpEntity entity = entityManager.find(FollowUpEntity.class, result.getId());
		assertEquals(newEntity.getObservation(), entity.getObservation());
		assertEquals(newEntity.getAdoption().getId(), entity.getAdoption().getId());
	}

	@Test
	void testCreateFollowUpWithNullDate() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity newEntity = factory.manufacturePojo(FollowUpEntity.class);
			newEntity.setDate(null);
			newEntity.setAdoption(adoptionList.get(0));
			newEntity.setVeterinarian(veterinarianList.get(0));
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testCreateFollowUpWithPastDate() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity newEntity = factory.manufacturePojo(FollowUpEntity.class);
			newEntity.setDate(new Date(System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000)));
			newEntity.setAdoption(adoptionList.get(0));
			newEntity.setVeterinarian(veterinarianList.get(0));
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testCreateFollowUpWithNullObservation() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity newEntity = factory.manufacturePojo(FollowUpEntity.class);
			newEntity.setDate(new Date(System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000)));
			newEntity.setObservation(null);
			newEntity.setAdoption(adoptionList.get(0));
			newEntity.setVeterinarian(veterinarianList.get(0));
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testCreateFollowUpWithNonExistentAdoption() {
		assertThrows(EntityNotFoundException.class, () -> {
			FollowUpEntity newEntity = factory.manufacturePojo(FollowUpEntity.class);
			newEntity.setDate(new Date(System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000)));
			AdoptionEntity fakeAdoption = new AdoptionEntity();
			fakeAdoption.setId(0L);
			newEntity.setAdoption(fakeAdoption);
			newEntity.setVeterinarian(veterinarianList.get(0));
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testCreateFollowUpWithNonFinalizedAdoption() {
		assertThrows(IllegalOperationException.class, () -> {
			AdoptionEntity notFinalized = factory.manufacturePojo(AdoptionEntity.class);
			notFinalized.setStatus("IN_PROGRESS");
			entityManager.persist(notFinalized);

			FollowUpEntity newEntity = factory.manufacturePojo(FollowUpEntity.class);
			newEntity.setDate(new Date(System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000)));
			newEntity.setAdoption(notFinalized);
			newEntity.setVeterinarian(veterinarianList.get(0));
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testGetFollowUps() {
		List<FollowUpEntity> list = followUpService.getFollowUps();
		assertEquals(followUpList.size(), list.size());
	}

	@Test
	void testGetFollowUpsEmpty() {
		entityManager.getEntityManager().createQuery("delete from FollowUpEntity").executeUpdate();
		List<FollowUpEntity> list = followUpService.getFollowUps();
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetFollowUpsByAdoption() throws EntityNotFoundException, IllegalOperationException {
		List<FollowUpEntity> list = followUpService.getFollowUps(adoptionList.get(0).getId());
		assertEquals(1, list.size());
		assertEquals(followUpList.get(0).getId(), list.get(0).getId());
	}

	@Test
	void testGetFollowUpsByAdoptionWithoutFollowUps() throws EntityNotFoundException, IllegalOperationException {
		AdoptionEntity newAdoption = factory.manufacturePojo(AdoptionEntity.class);
		newAdoption.setStatus("FINALIZED");
		entityManager.persist(newAdoption);

		List<FollowUpEntity> list = followUpService.getFollowUps(newAdoption.getId());
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetFollowUpsByNonExistentAdoption() {
		assertThrows(EntityNotFoundException.class, () -> {
			followUpService.getFollowUps(1000L);
		});
	}

	@Test
	void testGetFollowUp() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = followUpList.get(0);
		FollowUpEntity resultEntity = followUpService.getFollowUp(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
	}

	@Test
	void testGetInvalidFollowUpId() {
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.getFollowUp(0L);
		});
	}

	@Test
	void testGetNonExistentFollowUp() {
		assertThrows(EntityNotFoundException.class, () -> {
			followUpService.getFollowUp(1000L);
		});
	}

	@Test
	void testGetFollowUpAuthorizedAdopter() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = followUpList.get(0);
		Long adopterId = entity.getAdoption().getAdopter().getId();
		FollowUpEntity resultEntity = followUpService.getFollowUp(entity.getId(), adopterId);
		assertNotNull(resultEntity);
	}

	@Test
	void testGetFollowUpAuthorizedVeterinarian() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = followUpList.get(0);
		Long veterinarianId = entity.getVeterinarian().getId();
		FollowUpEntity resultEntity = followUpService.getFollowUp(entity.getId(), veterinarianId);
		assertNotNull(resultEntity);
	}

	@Test
	void testGetFollowUpUnauthorizedRequester() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity entity = followUpList.get(0);
			followUpService.getFollowUp(entity.getId(), adopterList.get(1).getId());
		});
	}

	@Test
	void testUpdateFollowUp() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = followUpList.get(0);
		FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setAdoption(entity.getAdoption());
		pojoEntity.setObservation("El adoptante reporta buena adaptación");

		followUpService.updateFollowUp(entity.getId(), pojoEntity);

		FollowUpEntity resp = entityManager.find(FollowUpEntity.class, entity.getId());
		assertEquals(pojoEntity.getObservation(), resp.getObservation());
		assertEquals(entity.getAdoption().getId(), resp.getAdoption().getId());
	}

	@Test
	void testUpdateFollowUpInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
			pojoEntity.setId(1000L);
			followUpService.updateFollowUp(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateFollowUpWithNullObservation() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity entity = followUpList.get(0);
			FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setObservation(null);
			followUpService.updateFollowUp(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateFollowUpChangeAdoption() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity entity = followUpList.get(0);
			FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setAdoption(adoptionList.get(1));
			followUpService.updateFollowUp(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeleteFollowUp() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = factory.manufacturePojo(FollowUpEntity.class);
		entity.setDate(new Date(System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000)));
		entity.setAdoption(adoptionList.get(0));
		entity.setVeterinarian(veterinarianList.get(0));
		entityManager.persist(entity);

		followUpService.deleteFollowUp(entity.getId());
		FollowUpEntity deleted = entityManager.find(FollowUpEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInvalidFollowUp() {
		assertThrows(EntityNotFoundException.class, () -> {
			followUpService.deleteFollowUp(1000L);
		});
	}

	@Test
	void testDeleteFollowUpAlreadyPerformed() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity entity = followUpList.get(0);
			entity.setDate(new Date(System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000)));
			entityManager.persist(entity);
			entityManager.flush();
			followUpService.deleteFollowUp(entity.getId());
		});
	}
}
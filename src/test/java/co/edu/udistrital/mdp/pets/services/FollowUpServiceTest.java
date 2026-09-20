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
import co.edu.udistrital.mdp.pets.entities.FollowUpEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(FollowUpService.class)
class FollowUpServiceTest {

	private static final long DAY = 86400000L;

	@Autowired
	private FollowUpService followUpService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<FollowUpEntity> followUpList = new ArrayList<>();
	private List<AdoptionEntity> adoptionList = new ArrayList<>();
	private List<VeterinarianEntity> veterinarianList = new ArrayList<>();
	private List<AdopterEntity> adopterList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from FollowUpEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from MedicalEventEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from VeterinarianEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			VeterinarianEntity veterinarianEntity = factory.manufacturePojo(VeterinarianEntity.class);
			entityManager.persist(veterinarianEntity);
			veterinarianList.add(veterinarianEntity);

			PetEntity petEntity = factory.manufacturePojo(PetEntity.class);
			entityManager.persist(petEntity);

			AdopterEntity adopterEntity = factory.manufacturePojo(AdopterEntity.class);
			entityManager.persist(adopterEntity);
			adopterList.add(adopterEntity);

			AdoptionEntity adoptionEntity = factory.manufacturePojo(AdoptionEntity.class);
			adoptionEntity.setPet(petEntity);
			adoptionEntity.setAdopter(adopterEntity);
			adoptionEntity.setStatus("FINALIZED");
			entityManager.persist(adoptionEntity);
			adoptionList.add(adoptionEntity);

			FollowUpEntity followUpEntity = buildValidFollowUp(adoptionEntity, veterinarianEntity);
			entityManager.persist(followUpEntity);
			followUpList.add(followUpEntity);
		}
	}

	private Date futureDate() {
		return new Date(System.currentTimeMillis() + 30 * DAY);
	}

	private Date pastDate() {
		return new Date(System.currentTimeMillis() - 30 * DAY);
	}

	private FollowUpEntity buildValidFollowUp(AdoptionEntity adoption, VeterinarianEntity veterinarian) {
		FollowUpEntity followUp = factory.manufacturePojo(FollowUpEntity.class);
		followUp.setDate(futureDate());
		followUp.setObservation("El adoptante reporta buena adaptacion");
		followUp.setAdoption(adoption);
		followUp.setVeterinarian(veterinarian);
		return followUp;
	}

	@Test
	void testCreateFollowUp() throws EntityNotFoundException, IllegalOperationException {
		VeterinarianEntity veterinarian = factory.manufacturePojo(VeterinarianEntity.class);
		entityManager.persist(veterinarian);

		PetEntity pet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(pet);

		AdopterEntity adopter = factory.manufacturePojo(AdopterEntity.class);
		entityManager.persist(adopter);

		AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
		adoption.setPet(pet);
		adoption.setAdopter(adopter);
		adoption.setStatus("FINALIZED");
		entityManager.persist(adoption);

		FollowUpEntity newEntity = buildValidFollowUp(adoption, veterinarian);

		FollowUpEntity result = followUpService.createFollowUp(newEntity);

		assertNotNull(result);
		FollowUpEntity persisted = entityManager.find(FollowUpEntity.class, result.getId());
		assertNotNull(persisted);
		assertEquals(newEntity.getObservation(), persisted.getObservation());
		assertEquals(adoption.getId(), persisted.getAdoption().getId());
		assertEquals(veterinarian.getId(), persisted.getVeterinarian().getId());
	}

	@Test
	void testCreateFollowUpWithNullObservation() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity newEntity = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
			newEntity.setObservation(null);
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testCreateFollowUpWithPastDate() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity newEntity = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
			newEntity.setDate(pastDate());
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testCreateFollowUpWithNonFinalizedAdoption() {
		assertThrows(IllegalOperationException.class, () -> {
			AdoptionEntity notFinalized = factory.manufacturePojo(AdoptionEntity.class);
			notFinalized.setPet(factory.manufacturePojo(PetEntity.class));
			notFinalized.setAdopter(adopterList.get(0));
			notFinalized.setStatus("IN_PROGRESS");
			entityManager.persist(notFinalized.getPet());
			entityManager.persist(notFinalized);

			FollowUpEntity newEntity = buildValidFollowUp(notFinalized, veterinarianList.get(0));
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testCreateFollowUpWithNonExistentAdoption() {
		assertThrows(EntityNotFoundException.class, () -> {
			FollowUpEntity newEntity = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
			AdoptionEntity fakeAdoption = new AdoptionEntity();
			fakeAdoption.setId(0L);
			newEntity.setAdoption(fakeAdoption);
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testCreateFollowUpWithNonExistentVeterinarian() {
		assertThrows(EntityNotFoundException.class, () -> {
			FollowUpEntity newEntity = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
			VeterinarianEntity fakeVeterinarian = new VeterinarianEntity();
			fakeVeterinarian.setId(0L);
			newEntity.setVeterinarian(fakeVeterinarian);
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
	void testGetFollowUpsByAdoption() {
		List<FollowUpEntity> list = followUpService.getFollowUps(adoptionList.get(0).getId());
		assertNotNull(list);
		assertTrue(list.stream().anyMatch(f -> f.getAdoption().getId().equals(adoptionList.get(0).getId())));
	}

	@Test
	void testGetFollowUp() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = followUpList.get(0);
		FollowUpEntity resultEntity = followUpService.getFollowUp(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
		assertEquals(entity.getObservation(), resultEntity.getObservation());
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
	void testGetFollowUpAuthorizedForAdopter() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = followUpList.get(0);
		Long adopterId = entity.getAdoption().getAdopter().getId();
		FollowUpEntity resultEntity = followUpService.getFollowUp(entity.getId(), adopterId);
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
	}

	@Test
	void testGetFollowUpAuthorizedForVeterinarian() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = followUpList.get(0);
		Long veterinarianId = entity.getVeterinarian().getId();
		FollowUpEntity resultEntity = followUpService.getFollowUp(entity.getId(), veterinarianId);
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
	}

	@Test
	void testGetFollowUpUnauthorizedRequester() {
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.getFollowUp(followUpList.get(0).getId(), 9999L);
		});
	}

	@Test
	void testUpdateFollowUp() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = followUpList.get(0);
		FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setDate(futureDate());
		pojoEntity.setObservation("Observacion actualizada");
		pojoEntity.setAdoption(entity.getAdoption());
		pojoEntity.setVeterinarian(entity.getVeterinarian());

		followUpService.updateFollowUp(entity.getId(), pojoEntity);

		FollowUpEntity resp = entityManager.find(FollowUpEntity.class, entity.getId());
		assertEquals("Observacion actualizada", resp.getObservation());
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
	void testUpdateFollowUpChangeAdoption() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity entity = followUpList.get(0);
			FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setDate(futureDate());
			pojoEntity.setObservation("Nueva observacion");
			pojoEntity.setAdoption(adoptionList.get(1));
			pojoEntity.setVeterinarian(entity.getVeterinarian());
			followUpService.updateFollowUp(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateFollowUpWithNullObservation() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity entity = followUpList.get(0);
			FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setDate(futureDate());
			pojoEntity.setObservation(null);
			pojoEntity.setAdoption(entity.getAdoption());
			followUpService.updateFollowUp(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeleteFollowUp() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = followUpList.get(1);
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
	void testDeleteClosedFollowUp() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity closed = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
			closed.setDate(pastDate());
			entityManager.persist(closed);
			followUpService.deleteFollowUp(closed.getId());
		});
	}
}
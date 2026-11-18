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
import co.edu.udistrital.mdp.pets.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.pets.entities.FollowUpEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
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
	void testGetFollowUpAuthorizedForAdmin() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = followUpList.get(0);
		FollowUpEntity resultEntity = followUpService.getFollowUp(entity.getId(), 9999L, "ADMIN");
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
	}

	@Test
	void testGetFollowUpNonAdminRoleUnauthorized() {
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.getFollowUp(followUpList.get(0).getId(), 9999L, "ADOPTER");
		});
	}

	@Test
	void testGetFollowUpWithNullRequesterAndAdminRole() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = followUpList.get(0);
		FollowUpEntity resultEntity = followUpService.getFollowUp(entity.getId(), null, "ADMIN");
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
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

	@Test
	void testCreateFollowUpWithNullDate() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity newEntity = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
			newEntity.setDate(null);
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testCreateFollowUpWithNullVeterinarian() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity newEntity = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
			newEntity.setVeterinarian(null);
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testCreateFollowUpWithNullAdoption() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity newEntity = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
			newEntity.setAdoption(null);
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testGetFollowUpsByAdoptionWithNoFollowUps() {
		List<FollowUpEntity> list = followUpService.getFollowUps(1000L);
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetFollowUpWithNullRequesterId() {
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.getFollowUp(followUpList.get(0).getId(), null);
		});
	}

	@Test
	void testGetFollowUpWithNullRequesterIdAndNonAdminRole() {
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.getFollowUp(followUpList.get(0).getId(), null, "ADOPTER");
		});
	}

	@Test
	void testUpdateFollowUpWithNullDate() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity entity = followUpList.get(0);
			FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setDate(null);
			pojoEntity.setObservation("Nueva observacion");
			pojoEntity.setAdoption(entity.getAdoption());
			pojoEntity.setVeterinarian(entity.getVeterinarian());
			followUpService.updateFollowUp(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateFollowUpWithNullAdoptionKeepsOriginal() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = followUpList.get(0);
		FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setDate(futureDate());
		pojoEntity.setObservation("Observacion tras update con adoption nula");
		pojoEntity.setAdoption(null);
		pojoEntity.setVeterinarian(entity.getVeterinarian());

		followUpService.updateFollowUp(entity.getId(), pojoEntity);

		FollowUpEntity resp = entityManager.find(FollowUpEntity.class, entity.getId());
		assertEquals("Observacion tras update con adoption nula", resp.getObservation());
		assertEquals(entity.getAdoption().getId(), resp.getAdoption().getId());
	}

	@Test
	void testCreateFollowUpWithVeterinarianNullId() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity newEntity = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
			newEntity.setVeterinarian(new VeterinarianEntity());
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testCreateFollowUpWithAdoptionNullId() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity newEntity = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
			newEntity.setAdoption(new AdoptionEntity());
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testUpdateFollowUpWithInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
			pojoEntity.setId(0L);
			pojoEntity.setDate(futureDate());
			pojoEntity.setObservation("Nueva observacion");
			followUpService.updateFollowUp(0L, pojoEntity);
		});
	}

	@Test
	void testDeleteFollowUpWithInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.deleteFollowUp(0L);
		});
	}

	@Test
	void testGetFollowUpWithNullId() {
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.getFollowUp(null);
		});
	}

	@Test
	void testGetFollowUpWithNullIdAndRequester() {
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.getFollowUp(null, 1L, "ADMIN");
		});
	}

	@Test
	void testGetNonExistentFollowUpWithRequester() {
		assertThrows(EntityNotFoundException.class, () -> {
			followUpService.getFollowUp(1000L, 1L, "ADMIN");
		});
	}

	@Test
	void testCreateFollowUpWithEmptyObservation() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity newEntity = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
			newEntity.setObservation("");
			followUpService.createFollowUp(newEntity);
		});
	}

	@Test
	void testUpdateFollowUpWithEmptyObservation() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity entity = followUpList.get(0);
			FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setDate(futureDate());
			pojoEntity.setObservation("");
			followUpService.updateFollowUp(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateFollowUpWithNullId() {
		assertThrows(IllegalOperationException.class, () -> {
			FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
			pojoEntity.setDate(futureDate());
			pojoEntity.setObservation("Observacion");
			followUpService.updateFollowUp(null, pojoEntity);
		});
	}

	@Test
	void testUpdateFollowUpWithAdoptionNullIdKeepsOriginal() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity entity = followUpList.get(0);
		FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setDate(futureDate());
		pojoEntity.setObservation("Nueva observacion");
		pojoEntity.setAdoption(new AdoptionEntity());

		followUpService.updateFollowUp(entity.getId(), pojoEntity);

		FollowUpEntity resp = entityManager.find(FollowUpEntity.class, entity.getId());
		assertEquals("Nueva observacion", resp.getObservation());
	}

	@Test
	void testDeleteFollowUpWithNullId() {
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.deleteFollowUp(null);
		});
	}

	@Test
	void testDeleteFollowUpWithNullDate() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity followUp = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
		followUp.setDate(null);
		entityManager.persist(followUp);

		followUpService.deleteFollowUp(followUp.getId());

		assertNull(entityManager.find(FollowUpEntity.class, followUp.getId()));
	}

	@Test
	void testCreateAdoptionFollowUp() throws EntityNotFoundException, IllegalOperationException {
		AdoptionEntity adoption = adoptionList.get(0);
		FollowUpEntity followUp = buildValidFollowUp(adoption, veterinarianList.get(0));

		FollowUpEntity result = followUpService.createAdoptionFollowUp(adoption.getId(), followUp);

		assertNotNull(result);
		assertEquals(adoption.getId(), result.getAdoption().getId());
	}

	@Test
	void testCreateAdoptionFollowUpWithInvalidAdoptionId() {
		FollowUpEntity followUp = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.createAdoptionFollowUp(0L, followUp);
		});
	}

	@Test
	void testCreateAdoptionFollowUpWithNonExistentAdoption() {
		FollowUpEntity followUp = buildValidFollowUp(adoptionList.get(0), veterinarianList.get(0));
		assertThrows(EntityNotFoundException.class, () -> {
			followUpService.createAdoptionFollowUp(1000L, followUp);
		});
	}

	@Test
	void testCreateAdoptionFollowUpWithPendingAdoption() {
		PetEntity pet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(pet);
		AdopterEntity adopter = factory.manufacturePojo(AdopterEntity.class);
		entityManager.persist(adopter);
		AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
		adoption.setPet(pet);
		adoption.setAdopter(adopter);
		adoption.setStatus("PENDING");
		entityManager.persist(adoption);

		FollowUpEntity followUp = buildValidFollowUp(null, veterinarianList.get(0));
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.createAdoptionFollowUp(adoption.getId(), followUp);
		});
	}

	@Test
	void testGetAdoptionFollowUps() throws EntityNotFoundException, IllegalOperationException {
		List<FollowUpEntity> list = followUpService.getAdoptionFollowUps(adoptionList.get(0).getId());
		assertEquals(1, list.size());
	}

	@Test
	void testGetAdoptionFollowUpsWithInvalidAdoptionId() {
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.getAdoptionFollowUps(0L);
		});
	}

	@Test
	void testGetAdoptionFollowUpsWithNonExistentAdoption() {
		assertThrows(EntityNotFoundException.class, () -> {
			followUpService.getAdoptionFollowUps(1000L);
		});
	}

	@Test
	void testGetAdoptionFollowUp() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity followUp = followUpList.get(0);
		FollowUpEntity result = followUpService.getAdoptionFollowUp(followUp.getAdoption().getId(), followUp.getId());
		assertEquals(followUp.getId(), result.getId());
	}

	@Test
	void testGetAdoptionFollowUpFromAnotherAdoption() {
		FollowUpEntity followUp = followUpList.get(0);
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.getAdoptionFollowUp(adoptionList.get(1).getId(), followUp.getId());
		});
	}

	@Test
	void testUpdateAdoptionFollowUp() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity followUp = followUpList.get(0);
		FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
		pojoEntity.setDate(futureDate());
		pojoEntity.setObservation("Actualizado via adopcion");

		followUpService.updateAdoptionFollowUp(followUp.getAdoption().getId(), followUp.getId(), pojoEntity);

		FollowUpEntity resp = entityManager.find(FollowUpEntity.class, followUp.getId());
		assertEquals("Actualizado via adopcion", resp.getObservation());
	}

	@Test
	void testUpdateAdoptionFollowUpFromAnotherAdoption() {
		FollowUpEntity followUp = followUpList.get(0);
		FollowUpEntity pojoEntity = factory.manufacturePojo(FollowUpEntity.class);
		pojoEntity.setDate(futureDate());
		pojoEntity.setObservation("Actualizado via adopcion");
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.updateAdoptionFollowUp(adoptionList.get(1).getId(), followUp.getId(), pojoEntity);
		});
	}

	@Test
	void testDeleteAdoptionFollowUp() throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity followUp = followUpList.get(1);
		followUpService.deleteAdoptionFollowUp(followUp.getAdoption().getId(), followUp.getId());
		assertNull(entityManager.find(FollowUpEntity.class, followUp.getId()));
	}

	@Test
	void testDeleteAdoptionFollowUpFromAnotherAdoption() {
		FollowUpEntity followUp = followUpList.get(0);
		assertThrows(IllegalOperationException.class, () -> {
			followUpService.deleteAdoptionFollowUp(adoptionList.get(1).getId(), followUp.getId());
		});
	}

	@Test
	void testSeedIntegrationTestData() {
		clearData();
		resetIdentity("shelter_entity");
		resetIdentity("user_entity");
		resetIdentity("pet_entity");
		entityManager.clear();

		ShelterEntity shelter = factory.manufacturePojo(ShelterEntity.class);
		entityManager.persist(shelter);
		VeterinarianEntity veterinarian = factory.manufacturePojo(VeterinarianEntity.class);
		entityManager.persist(veterinarian);
		AdopterEntity adopter = factory.manufacturePojo(AdopterEntity.class);
		entityManager.persist(adopter);
		PetEntity petOne = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(petOne);
		PetEntity petTwo = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(petTwo);
		entityManager.flush();

		assertEquals(1L, shelter.getId());
		assertEquals(1L, veterinarian.getId());
		assertEquals(2L, adopter.getId());
		assertEquals(1L, petOne.getId());
		assertEquals(2L, petTwo.getId());

		followUpService.seedIntegrationTestData();

		assertEquals(2L, countOf("AdoptionRequestEntity"));
		assertEquals(2L, countOf("AdoptionEntity"));
		assertEquals(3L, countOf("FollowUpEntity"));

		followUpService.seedIntegrationTestData();
		assertEquals(3L, countOf("FollowUpEntity"));
	}

	private void resetIdentity(String table) {
		entityManager.getEntityManager()
				.createNativeQuery("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH 1").executeUpdate();
	}

	private long countOf(String entity) {
		return (Long) entityManager.getEntityManager()
				.createQuery("select count(e) from " + entity + " e").getSingleResult();
	}
}
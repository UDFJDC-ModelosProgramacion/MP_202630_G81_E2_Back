package co.edu.udistrital.mdp.pets.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import co.edu.udistrital.mdp.pets.entities.ReviewEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(AdoptionService.class)
class AdoptionServiceTest {

	private static final String IN_PROGRESS = "IN_PROGRESS";
	private static final Long NON_EXISTENT_ID = 1000L;
	private static final Long INVALID_ID = 0L;

	@Autowired
	private AdoptionService adoptionService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private ShelterEntity shelter;
	private AdopterEntity adopter;

	/** Las mascotas 0, 1 y 2 ya tienen una adopción; la 3 y la 4 están libres. */
	private List<PetEntity> petList = new ArrayList<>();
	private List<AdoptionEntity> adoptionList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from ReviewEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from FollowUpEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ReturnEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionRequestEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ShelterEntity").executeUpdate();
	}

	private void insertData() {
		shelter = factory.manufacturePojo(ShelterEntity.class);
		entityManager.persist(shelter);

		adopter = factory.manufacturePojo(AdopterEntity.class);
		adopter.setShelter(shelter);
		entityManager.persist(adopter);

		for (int i = 0; i < 5; i++) {
			PetEntity pet = factory.manufacturePojo(PetEntity.class);
			pet.setShelter(shelter);
			entityManager.persist(pet);
			petList.add(pet);
		}

		for (int i = 0; i < 3; i++) {
			AdoptionEntity adoption = newAdoption(petList.get(i), null);
			entityManager.persist(adoption);
			adoptionList.add(adoption);

			// mantiene la asociación bidireccional en memoria para las reglas sobre la mascota
			petList.get(i).getAdoptions().add(adoption);
		}
	}

	private AdoptionEntity newAdoption(PetEntity pet, AdoptionRequestEntity request) {
		AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
		adoption.setStatus(IN_PROGRESS);
		adoption.setDate(new Date());
		adoption.setPet(pet);
		adoption.setShelter(shelter);
		adoption.setAdopter(adopter);
		adoption.setAdoptionRequest(request);
		return adoption;
	}

	private AdoptionRequestEntity persistRequest(PetEntity pet, String status) {
		AdoptionRequestEntity request = new AdoptionRequestEntity();
		request.setPet(pet);
		request.setShelter(shelter);
		request.setAdopter(adopter);
		request.setStatus(status);
		request.setDate(new Date());
		request.setDescription("I would like to adopt this pet");
		entityManager.persist(request);
		return request;
	}

	private AdoptionRequestEntity approvedRequestFor(PetEntity pet) {
		return persistRequest(pet, AdoptionRequestService.APPROVED_STATUS);
	}

	/** Adopción válida sobre una mascota libre (la 3) con una solicitud aprobada. */
	private AdoptionEntity newValidAdoption() {
		return newAdoption(petList.get(3), approvedRequestFor(petList.get(3)));
	}

	private AdoptionEntity newUpdate(String status, String notes) {
		AdoptionEntity update = new AdoptionEntity();
		update.setStatus(status);
		update.setImportantNotes(notes);
		return update;
	}

	// ---------------------------------------------------------------- create

	@Test
	void testCreateAdoption() throws EntityNotFoundException, IllegalOperationException {
		AdoptionEntity newEntity = newValidAdoption();

		AdoptionEntity result = adoptionService.createAdoption(newEntity);

		assertNotNull(result.getId());
		AdoptionEntity entity = entityManager.find(AdoptionEntity.class, result.getId());
		assertEquals(newEntity.getStatus(), entity.getStatus());
		assertEquals(petList.get(3).getId(), entity.getPet().getId());
		assertEquals(shelter.getId(), entity.getShelter().getId());
		assertEquals(adopter.getId(), entity.getAdopter().getId());
		assertEquals(newEntity.getAdoptionRequest().getId(), entity.getAdoptionRequest().getId());
	}

	@Test
	void testCreateAdoptionForPetWithOnlyCancelledAdoption() throws EntityNotFoundException, IllegalOperationException {
		adoptionList.get(0).setStatus(AdoptionService.CANCELLED_STATUS);
		AdoptionEntity newEntity = newAdoption(petList.get(0), approvedRequestFor(petList.get(0)));

		AdoptionEntity result = adoptionService.createAdoption(newEntity);

		assertNotNull(result.getId());
	}

	@Test
	void testCreateNullAdoption() {
		assertThrows(IllegalOperationException.class, () -> adoptionService.createAdoption(null));
	}

	@Test
	void testCreateAdoptionWithNullDate() {
		AdoptionEntity newEntity = newValidAdoption();
		newEntity.setDate(null);
		assertThrows(IllegalOperationException.class, () -> adoptionService.createAdoption(newEntity));
	}

	@Test
	void testCreateAdoptionWithBlankStatus() {
		AdoptionEntity newEntity = newValidAdoption();
		newEntity.setStatus(" ");
		assertThrows(IllegalOperationException.class, () -> adoptionService.createAdoption(newEntity));
	}

	@Test
	void testCreateAdoptionWithNullPet() {
		AdoptionEntity newEntity = newValidAdoption();
		newEntity.setPet(null);
		assertThrows(IllegalOperationException.class, () -> adoptionService.createAdoption(newEntity));
	}

	@Test
	void testCreateAdoptionWithInvalidPet() {
		AdoptionEntity newEntity = newValidAdoption();
		PetEntity pet = new PetEntity();
		pet.setId(INVALID_ID);
		newEntity.setPet(pet);
		assertThrows(EntityNotFoundException.class, () -> adoptionService.createAdoption(newEntity));
	}

	@Test
	void testCreateAdoptionWithNullShelter() {
		AdoptionEntity newEntity = newValidAdoption();
		newEntity.setShelter(null);
		assertThrows(IllegalOperationException.class, () -> adoptionService.createAdoption(newEntity));
	}

	@Test
	void testCreateAdoptionWithInvalidShelter() {
		AdoptionEntity newEntity = newValidAdoption();
		ShelterEntity invalidShelter = new ShelterEntity();
		invalidShelter.setId(INVALID_ID);
		newEntity.setShelter(invalidShelter);
		assertThrows(EntityNotFoundException.class, () -> adoptionService.createAdoption(newEntity));
	}

	@Test
	void testCreateAdoptionWithNullAdopter() {
		AdoptionEntity newEntity = newValidAdoption();
		newEntity.setAdopter(null);
		assertThrows(IllegalOperationException.class, () -> adoptionService.createAdoption(newEntity));
	}

	@Test
	void testCreateAdoptionWithInvalidAdopter() {
		AdoptionEntity newEntity = newValidAdoption();
		AdopterEntity invalidAdopter = new AdopterEntity();
		invalidAdopter.setId(INVALID_ID);
		newEntity.setAdopter(invalidAdopter);
		assertThrows(EntityNotFoundException.class, () -> adoptionService.createAdoption(newEntity));
	}

	@Test
	void testCreateAdoptionWithNullRequest() {
		AdoptionEntity newEntity = newValidAdoption();
		newEntity.setAdoptionRequest(null);
		assertThrows(IllegalOperationException.class, () -> adoptionService.createAdoption(newEntity));
	}

	@Test
	void testCreateAdoptionWithInvalidRequest() {
		AdoptionEntity newEntity = newValidAdoption();
		AdoptionRequestEntity invalidRequest = new AdoptionRequestEntity();
		invalidRequest.setId(INVALID_ID);
		newEntity.setAdoptionRequest(invalidRequest);
		assertThrows(EntityNotFoundException.class, () -> adoptionService.createAdoption(newEntity));
	}

	@Test
	void testCreateAdoptionWithRequestNotApproved() {
		AdoptionEntity newEntity = newAdoption(petList.get(3),
				persistRequest(petList.get(3), AdoptionRequestService.PENDING_STATUS));
		assertThrows(IllegalOperationException.class, () -> adoptionService.createAdoption(newEntity));
	}

	@Test
	void testCreateAdoptionWithRequestThatAlreadyHasAdoption() {
		AdoptionRequestEntity request = approvedRequestFor(petList.get(3));
		request.setAdoption(adoptionList.get(0));
		AdoptionEntity newEntity = newAdoption(petList.get(3), request);
		assertThrows(IllegalOperationException.class, () -> adoptionService.createAdoption(newEntity));
	}

	@Test
	void testCreateAdoptionForAlreadyAdoptedPet() {
		AdoptionEntity newEntity = newAdoption(petList.get(0), approvedRequestFor(petList.get(0)));
		assertThrows(IllegalOperationException.class, () -> adoptionService.createAdoption(newEntity));
	}

	// ------------------------------------------------------------------- get

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
		assertThrows(IllegalOperationException.class, () -> adoptionService.getAdoption(INVALID_ID));
	}

	@Test
	void testGetNonExistentAdoption() {
		assertThrows(EntityNotFoundException.class, () -> adoptionService.getAdoption(NON_EXISTENT_ID));
	}

	// ---------------------------------------------------------------- update

	@Test
	void testUpdateAdoption() throws EntityNotFoundException, IllegalOperationException {
		AdoptionEntity entity = adoptionList.get(0);
		AdoptionEntity update = newUpdate(AdoptionService.FINALIZED_STATUS, "Adopted and settled");

		adoptionService.updateAdoption(entity.getId(), update);

		AdoptionEntity resp = entityManager.find(AdoptionEntity.class, entity.getId());
		assertEquals(AdoptionService.FINALIZED_STATUS, resp.getStatus());
		assertEquals("Adopted and settled", resp.getImportantNotes());
		assertEquals(petList.get(0).getId(), resp.getPet().getId());
		assertEquals(adopter.getId(), resp.getAdopter().getId());
		assertEquals(shelter.getId(), resp.getShelter().getId());
	}

	@Test
	void testUpdateAdoptionInvalidId() {
		AdoptionEntity update = newUpdate(IN_PROGRESS, null);
		assertThrows(IllegalOperationException.class, () -> adoptionService.updateAdoption(INVALID_ID, update));
	}

	@Test
	void testUpdateNonExistentAdoption() {
		AdoptionEntity update = newUpdate(IN_PROGRESS, null);
		assertThrows(EntityNotFoundException.class, () -> adoptionService.updateAdoption(NON_EXISTENT_ID, update));
	}

	@Test
	void testUpdateAdoptionWithNullEntity() {
		Long id = adoptionList.get(0).getId();
		assertThrows(IllegalOperationException.class, () -> adoptionService.updateAdoption(id, null));
	}

	@Test
	void testUpdateAdoptionWithBlankStatus() {
		Long id = adoptionList.get(0).getId();
		AdoptionEntity update = newUpdate("", null);
		assertThrows(IllegalOperationException.class, () -> adoptionService.updateAdoption(id, update));
	}

	@Test
	void testUpdateFinalizedAdoption() {
		AdoptionEntity entity = adoptionList.get(1);
		entity.setStatus(AdoptionService.FINALIZED_STATUS);
		Long id = entity.getId();
		AdoptionEntity update = newUpdate(IN_PROGRESS, null);
		assertThrows(IllegalOperationException.class, () -> adoptionService.updateAdoption(id, update));
	}

	@Test
	void testUpdateCancelledAdoption() {
		AdoptionEntity entity = adoptionList.get(1);
		entity.setStatus(AdoptionService.CANCELLED_STATUS);
		Long id = entity.getId();
		AdoptionEntity update = newUpdate(IN_PROGRESS, null);
		assertThrows(IllegalOperationException.class, () -> adoptionService.updateAdoption(id, update));
	}

	// ---------------------------------------------------------------- delete

	@Test
	void testDeleteAdoption() throws EntityNotFoundException, IllegalOperationException {
		AdoptionEntity entity = adoptionList.get(0);
		adoptionService.deleteAdoption(entity.getId());
		assertNull(entityManager.find(AdoptionEntity.class, entity.getId()));
	}

	@Test
	void testDeleteAdoptionInvalidId() {
		assertThrows(IllegalOperationException.class, () -> adoptionService.deleteAdoption(INVALID_ID));
	}

	@Test
	void testDeleteNonExistentAdoption() {
		assertThrows(EntityNotFoundException.class, () -> adoptionService.deleteAdoption(NON_EXISTENT_ID));
	}

	@Test
	void testDeleteAdoptionWithReviews() {
		AdoptionEntity entity = adoptionList.get(0);
		ReviewEntity review = factory.manufacturePojo(ReviewEntity.class);
		review.setAdoption(entity);
		entityManager.persist(review);
		entity.getReviews().add(review);
		Long id = entity.getId();

		assertThrows(IllegalOperationException.class, () -> adoptionService.deleteAdoption(id));
	}

	@Test
	void testDeleteAdoptionWithFollowUps() {
		AdoptionEntity entity = adoptionList.get(0);
		FollowUpEntity followUp = factory.manufacturePojo(FollowUpEntity.class);
		followUp.setAdoption(entity);
		entityManager.persist(followUp);
		entity.getFollowUps().add(followUp);
		Long id = entity.getId();

		assertThrows(IllegalOperationException.class, () -> adoptionService.deleteAdoption(id));
	}
}
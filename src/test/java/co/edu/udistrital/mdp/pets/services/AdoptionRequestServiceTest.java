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
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(AdoptionRequestService.class)
class AdoptionRequestServiceTest {

	private static final String ADMIN = "ADMIN";
	private static final String ADOPTER_ROLE = "ADOPTER";
	private static final String DESCRIPTION = "I would like to adopt this pet";

	@Autowired
	private AdoptionRequestService adoptionRequestService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<AdoptionRequestEntity> requestList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from ReturnEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionRequestEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ShelterEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			requestList.add(buildRequest(persistAdopter(), persistPet(), AdoptionRequestService.PENDING_STATUS));
		}
	}

	private AdopterEntity persistAdopter() {
		AdopterEntity adopter = factory.manufacturePojo(AdopterEntity.class);
		entityManager.persist(adopter);
		return adopter;
	}

	private PetEntity persistPet() {
		PetEntity pet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(pet);
		return pet;
	}

	private AdoptionRequestEntity buildRequest(AdopterEntity adopter, PetEntity pet, String status) {
		AdoptionRequestEntity request = new AdoptionRequestEntity();
		request.setAdopter(adopter);
		request.setPet(pet);
		request.setStatus(status);
		request.setDate(new Date());
		request.setDescription(DESCRIPTION);
		entityManager.persist(request);
		return request;
	}

	private AdoptionEntity persistAdoption(PetEntity pet, String status) {
		AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
		adoption.setPet(pet);
		adoption.setAdopter(persistAdopter());
		adoption.setStatus(status);
		entityManager.persist(adoption);
		return adoption;
	}

	private AdoptionRequestEntity newRequest(AdopterEntity adopter, PetEntity pet) {
		AdoptionRequestEntity request = new AdoptionRequestEntity();
		request.setAdopter(adopter);
		request.setPet(pet);
		request.setDescription(DESCRIPTION);
		return request;
	}

	private AdoptionRequestEntity newUpdate(String description, String status) {
		AdoptionRequestEntity update = new AdoptionRequestEntity();
		update.setDescription(description);
		update.setStatus(status);
		return update;
	}

	// ---------------------------------------------------------------- create

	@Test
	void testCreateAdoptionRequest() throws EntityNotFoundException, IllegalOperationException {
		ShelterEntity shelter = factory.manufacturePojo(ShelterEntity.class);
		entityManager.persist(shelter);
		PetEntity pet = persistPet();
		pet.setShelter(shelter);
		AdopterEntity adopter = persistAdopter();

		AdoptionRequestEntity result = adoptionRequestService.createAdoptionRequest(newRequest(adopter, pet));

		assertNotNull(result.getId());
		assertNotNull(result.getDate());
		assertEquals(AdoptionRequestService.PENDING_STATUS, result.getStatus());
		assertEquals(adopter.getId(), result.getAdopter().getId());
		assertEquals(pet.getId(), result.getPet().getId());
		assertEquals(shelter.getId(), result.getShelter().getId());
		assertNotNull(entityManager.find(AdoptionRequestEntity.class, result.getId()));
	}

	@Test
	void testCreateAdoptionRequestWithNullRequest() {
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.createAdoptionRequest(null));
	}

	@Test
	void testCreateAdoptionRequestWithoutAdopter() {
		AdoptionRequestEntity request = newRequest(null, persistPet());
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.createAdoptionRequest(request));

		AdoptionRequestEntity withoutId = newRequest(new AdopterEntity(), persistPet());
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.createAdoptionRequest(withoutId));
	}

	@Test
	void testCreateAdoptionRequestWithoutPet() {
		AdoptionRequestEntity request = newRequest(persistAdopter(), null);
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.createAdoptionRequest(request));

		AdoptionRequestEntity withoutId = newRequest(persistAdopter(), new PetEntity());
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.createAdoptionRequest(withoutId));
	}

	@Test
	void testCreateAdoptionRequestWithBlankDescription() {
		AdoptionRequestEntity request = newRequest(persistAdopter(), persistPet());
		request.setDescription("  ");
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.createAdoptionRequest(request));

		request.setDescription(null);
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.createAdoptionRequest(request));
	}

	@Test
	void testCreateAdoptionRequestWithNonExistingAdopter() {
		AdopterEntity ghost = new AdopterEntity();
		ghost.setId(999999L);
		AdoptionRequestEntity request = newRequest(ghost, persistPet());
		assertThrows(EntityNotFoundException.class, () -> adoptionRequestService.createAdoptionRequest(request));
	}

	@Test
	void testCreateAdoptionRequestWithNonExistingPet() {
		PetEntity ghost = new PetEntity();
		ghost.setId(999999L);
		AdoptionRequestEntity request = newRequest(persistAdopter(), ghost);
		assertThrows(EntityNotFoundException.class, () -> adoptionRequestService.createAdoptionRequest(request));
	}

	@Test
	void testCreateAdoptionRequestForPetAlreadyAdopted() {
		PetEntity pet = persistPet();
		persistAdoption(pet, "FINALIZED");

		AdoptionRequestEntity request = newRequest(persistAdopter(), pet);
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.createAdoptionRequest(request));
	}

	@Test
	void testCreateAdoptionRequestForPetWithCancelledAdoption()
			throws EntityNotFoundException, IllegalOperationException {
		PetEntity pet = persistPet();
		persistAdoption(pet, "CANCELLED");

		AdoptionRequestEntity result = adoptionRequestService.createAdoptionRequest(newRequest(persistAdopter(), pet));

		assertNotNull(result.getId());
	}

	@Test
	void testCreateAdoptionRequestWithActiveRequestForSamePet() {
		AdoptionRequestEntity existing = requestList.get(0);

		AdoptionRequestEntity request = newRequest(existing.getAdopter(), existing.getPet());
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.createAdoptionRequest(request));
	}

	@Test
	void testCreateAdoptionRequestWithApprovedRequestForSamePet() {
		AdopterEntity adopter = persistAdopter();
		PetEntity pet = persistPet();
		buildRequest(adopter, pet, AdoptionRequestService.APPROVED_STATUS);

		AdoptionRequestEntity request = newRequest(adopter, pet);
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.createAdoptionRequest(request));
	}

	@Test
	void testCreateAdoptionRequestWithRejectedRequestForSamePet()
			throws EntityNotFoundException, IllegalOperationException {
		AdopterEntity adopter = persistAdopter();
		PetEntity pet = persistPet();
		buildRequest(adopter, pet, AdoptionRequestService.REJECTED_STATUS);

		AdoptionRequestEntity result = adoptionRequestService.createAdoptionRequest(newRequest(adopter, pet));

		assertNotNull(result.getId());
	}

	@Test
	void testCreateAdoptionRequestFromAnotherAdopterForSamePet()
			throws EntityNotFoundException, IllegalOperationException {
		AdoptionRequestEntity existing = requestList.get(0);

		AdoptionRequestEntity result = adoptionRequestService
				.createAdoptionRequest(newRequest(persistAdopter(), existing.getPet()));

		assertNotNull(result.getId());
	}

	// ------------------------------------------------------------------ read

	@Test
	void testReadAdoptionRequestAsCreator() throws EntityNotFoundException, IllegalOperationException {
		AdoptionRequestEntity expected = requestList.get(0);

		AdoptionRequestEntity result = adoptionRequestService.readAdoptionRequest(expected.getId(),
				expected.getAdopter().getId(), ADOPTER_ROLE);

		assertEquals(expected.getId(), result.getId());
	}

	@Test
	void testReadAdoptionRequestAsAdmin() throws EntityNotFoundException, IllegalOperationException {
		AdoptionRequestEntity expected = requestList.get(0);

		AdoptionRequestEntity result = adoptionRequestService.readAdoptionRequest(expected.getId(), 999999L, ADMIN);

		assertEquals(expected.getId(), result.getId());
	}

	@Test
	void testReadAdoptionRequestAsAnotherUser() {
		AdoptionRequestEntity target = requestList.get(0);
		Long otherUserId = requestList.get(1).getAdopter().getId();

		assertThrows(IllegalOperationException.class,
				() -> adoptionRequestService.readAdoptionRequest(target.getId(), otherUserId, ADOPTER_ROLE));
		assertThrows(IllegalOperationException.class,
				() -> adoptionRequestService.readAdoptionRequest(target.getId(), null, ADOPTER_ROLE));
	}

	@Test
	void testReadAdoptionRequestWithInvalidId() {
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.readAdoptionRequest(null, 1L, ADMIN));
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.readAdoptionRequest(0L, 1L, ADMIN));
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.readAdoptionRequest(-1L, 1L, ADMIN));
	}

	@Test
	void testReadAdoptionRequestNotFound() {
		assertThrows(EntityNotFoundException.class,
				() -> adoptionRequestService.readAdoptionRequest(999999L, 1L, ADMIN));
	}

	// -------------------------------------------------------------- readAll

	@Test
	void testReadAllAdoptionRequestsAsAdmin() throws IllegalOperationException {
		List<AdoptionRequestEntity> result = adoptionRequestService.readAllAdoptionRequests(null, null, null, 1L,
				ADMIN);
		assertEquals(requestList.size(), result.size());
	}

	@Test
	void testReadAllAdoptionRequestsAsNonAdminSeesOnlyOwn() throws IllegalOperationException {
		AdoptionRequestEntity own = requestList.get(0);

		List<AdoptionRequestEntity> result = adoptionRequestService.readAllAdoptionRequests(null, null, null,
				own.getAdopter().getId(), ADOPTER_ROLE);

		assertEquals(1, result.size());
		assertEquals(own.getId(), result.get(0).getId());
	}

	@Test
	void testReadAllAdoptionRequestsAsNonAdminFilteringByAnotherAdopter() throws IllegalOperationException {
		Long currentUserId = requestList.get(0).getAdopter().getId();
		Long otherAdopterId = requestList.get(1).getAdopter().getId();

		List<AdoptionRequestEntity> result = adoptionRequestService.readAllAdoptionRequests(otherAdopterId, null,
				null, currentUserId, ADOPTER_ROLE);

		assertTrue(result.isEmpty());
	}

	@Test
	void testReadAllAdoptionRequestsFilteredByAdopter() throws IllegalOperationException {
		AdoptionRequestEntity target = requestList.get(1);

		List<AdoptionRequestEntity> result = adoptionRequestService
				.readAllAdoptionRequests(target.getAdopter().getId(), null, null, 1L, ADMIN);

		assertEquals(1, result.size());
		assertEquals(target.getId(), result.get(0).getId());
	}

	@Test
	void testReadAllAdoptionRequestsFilteredByPet() throws IllegalOperationException {
		AdoptionRequestEntity target = requestList.get(2);

		List<AdoptionRequestEntity> result = adoptionRequestService.readAllAdoptionRequests(null,
				target.getPet().getId(), null, 1L, ADMIN);

		assertEquals(1, result.size());
		assertEquals(target.getId(), result.get(0).getId());
	}

	@Test
	void testReadAllAdoptionRequestsFilteredByStatus() throws IllegalOperationException {
		AdoptionRequestEntity approved = buildRequest(persistAdopter(), persistPet(),
				AdoptionRequestService.APPROVED_STATUS);

		List<AdoptionRequestEntity> result = adoptionRequestService.readAllAdoptionRequests(null, null, "approved",
				1L, ADMIN);

		assertEquals(1, result.size());
		assertEquals(approved.getId(), result.get(0).getId());
	}

	@Test
	void testReadAllAdoptionRequestsWithAllFiltersMatching() throws IllegalOperationException {
		AdoptionRequestEntity target = requestList.get(0);

		List<AdoptionRequestEntity> result = adoptionRequestService.readAllAdoptionRequests(
				target.getAdopter().getId(), target.getPet().getId(), AdoptionRequestService.PENDING_STATUS, 1L,
				ADMIN);

		assertEquals(1, result.size());
	}

	@Test
	void testReadAllAdoptionRequestsWithFiltersThatDoNotMatchTogether() throws IllegalOperationException {
		AdoptionRequestEntity first = requestList.get(0);
		AdoptionRequestEntity second = requestList.get(1);

		List<AdoptionRequestEntity> result = adoptionRequestService.readAllAdoptionRequests(
				first.getAdopter().getId(), second.getPet().getId(), null, 1L, ADMIN);

		assertTrue(result.isEmpty());
	}

	@Test
	void testReadAllAdoptionRequestsWithInvalidFilters() {
		assertThrows(IllegalOperationException.class,
				() -> adoptionRequestService.readAllAdoptionRequests(0L, null, null, 1L, ADMIN));
		assertThrows(IllegalOperationException.class,
				() -> adoptionRequestService.readAllAdoptionRequests(null, -5L, null, 1L, ADMIN));
		assertThrows(IllegalOperationException.class,
				() -> adoptionRequestService.readAllAdoptionRequests(null, null, "  ", 1L, ADMIN));
	}

	// ---------------------------------------------------------------- update

	@Test
	void testUpdateAdoptionRequest() throws EntityNotFoundException, IllegalOperationException {
		AdoptionRequestEntity target = requestList.get(0);

		AdoptionRequestEntity result = adoptionRequestService.updateAdoptionRequest(target.getId(),
				newUpdate("Updated description", "approved"));

		assertEquals("Updated description", result.getDescription());
		assertEquals(AdoptionRequestService.APPROVED_STATUS, result.getStatus());
		assertEquals(target.getPet().getId(), result.getPet().getId());
	}

	@Test
	void testUpdateAdoptionRequestWithSamePet() throws EntityNotFoundException, IllegalOperationException {
		AdoptionRequestEntity target = requestList.get(0);
		AdoptionRequestEntity update = newUpdate("Same pet", AdoptionRequestService.PENDING_STATUS);
		update.setPet(target.getPet());

		AdoptionRequestEntity result = adoptionRequestService.updateAdoptionRequest(target.getId(), update);

		assertEquals("Same pet", result.getDescription());
	}

	@Test
	void testUpdateAdoptionRequestChangingPet() {
		AdoptionRequestEntity target = requestList.get(0);
		AdoptionRequestEntity update = newUpdate(DESCRIPTION, AdoptionRequestService.PENDING_STATUS);
		update.setPet(requestList.get(1).getPet());

		assertThrows(IllegalOperationException.class,
				() -> adoptionRequestService.updateAdoptionRequest(target.getId(), update));
	}

	@Test
	void testUpdateFinalizedRequestBackToPending() {
		AdoptionRequestEntity finalized = buildRequest(persistAdopter(), persistPet(),
				AdoptionRequestService.FINALIZED_STATUS);

		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.updateAdoptionRequest(
				finalized.getId(), newUpdate(DESCRIPTION, AdoptionRequestService.PENDING_STATUS)));
	}

	@Test
	void testUpdateApprovedRequestBackToPending() {
		AdoptionRequestEntity approved = buildRequest(persistAdopter(), persistPet(),
				AdoptionRequestService.APPROVED_STATUS);

		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.updateAdoptionRequest(
				approved.getId(), newUpdate(DESCRIPTION, AdoptionRequestService.PENDING_STATUS)));
	}

	@Test
	void testUpdateApprovedRequestToFinalized() throws EntityNotFoundException, IllegalOperationException {
		AdoptionRequestEntity approved = buildRequest(persistAdopter(), persistPet(),
				AdoptionRequestService.APPROVED_STATUS);

		AdoptionRequestEntity result = adoptionRequestService.updateAdoptionRequest(approved.getId(),
				newUpdate(DESCRIPTION, AdoptionRequestService.FINALIZED_STATUS));

		assertEquals(AdoptionRequestService.FINALIZED_STATUS, result.getStatus());
	}

	@Test
	void testUpdateAdoptionRequestWithInvalidId() {
		AdoptionRequestEntity update = newUpdate(DESCRIPTION, AdoptionRequestService.PENDING_STATUS);
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.updateAdoptionRequest(null, update));
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.updateAdoptionRequest(0L, update));
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.updateAdoptionRequest(-1L, update));
	}

	@Test
	void testUpdateAdoptionRequestWithNullAttributes() {
		Long id = requestList.get(0).getId();
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.updateAdoptionRequest(id, null));
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.updateAdoptionRequest(id,
				newUpdate(null, AdoptionRequestService.PENDING_STATUS)));
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.updateAdoptionRequest(id,
				newUpdate("  ", AdoptionRequestService.PENDING_STATUS)));
		assertThrows(IllegalOperationException.class,
				() -> adoptionRequestService.updateAdoptionRequest(id, newUpdate(DESCRIPTION, null)));
	}

	@Test
	void testUpdateAdoptionRequestWithInvalidStatus() {
		Long id = requestList.get(0).getId();
		assertThrows(IllegalOperationException.class,
				() -> adoptionRequestService.updateAdoptionRequest(id, newUpdate(DESCRIPTION, "SOMETHING_ELSE")));
	}

	@Test
	void testUpdateAdoptionRequestNotFound() {
		AdoptionRequestEntity update = newUpdate(DESCRIPTION, AdoptionRequestService.PENDING_STATUS);
		assertThrows(EntityNotFoundException.class,
				() -> adoptionRequestService.updateAdoptionRequest(999999L, update));
	}

	// ---------------------------------------------------------------- delete

	@Test
	void testDeletePendingAdoptionRequest() throws EntityNotFoundException, IllegalOperationException {
		AdoptionRequestEntity target = requestList.get(0);

		adoptionRequestService.deleteAdoptionRequest(target.getId());
		entityManager.flush();

		assertNull(entityManager.find(AdoptionRequestEntity.class, target.getId()));
	}

	@Test
	void testDeleteProcessedAdoptionRequest() {
		for (String status : List.of(AdoptionRequestService.APPROVED_STATUS, AdoptionRequestService.REJECTED_STATUS,
				AdoptionRequestService.FINALIZED_STATUS)) {
			AdoptionRequestEntity processed = buildRequest(persistAdopter(), persistPet(), status);
			assertThrows(IllegalOperationException.class,
					() -> adoptionRequestService.deleteAdoptionRequest(processed.getId()));
			assertNotNull(entityManager.find(AdoptionRequestEntity.class, processed.getId()));
		}
	}

	@Test
	void testDeleteAdoptionRequestWithInvalidId() {
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.deleteAdoptionRequest(null));
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.deleteAdoptionRequest(0L));
		assertThrows(IllegalOperationException.class, () -> adoptionRequestService.deleteAdoptionRequest(-1L));
	}

	@Test
	void testDeleteAdoptionRequestNotFound() {
		assertThrows(EntityNotFoundException.class, () -> adoptionRequestService.deleteAdoptionRequest(999999L));
	}
}
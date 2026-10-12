package co.edu.udistrital.mdp.pets.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ReturnEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(ReturnService.class)
class ReturnServiceTest {

	private static final String REASON = "The pet did not adapt to the household";

	@Autowired
	private ReturnService returnService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<ReturnEntity> returnList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from ReturnEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ShelterEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			AdoptionEntity adoption = buildAdoption("FINALIZED");
			returnList.add(buildReturn(adoption, ReturnService.PENDING_STATUS));
		}
	}

	private AdoptionEntity buildAdoption(String status) {
		PetEntity pet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(pet);
		return buildAdoption(status, pet);
	}

	private AdoptionEntity buildAdoption(String status, PetEntity pet) {
		ShelterEntity shelter = factory.manufacturePojo(ShelterEntity.class);
		entityManager.persist(shelter);
		AdopterEntity adopter = factory.manufacturePojo(AdopterEntity.class);
		entityManager.persist(adopter);

		AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
		adoption.setPet(pet);
		adoption.setShelter(shelter);
		adoption.setAdopter(adopter);
		adoption.setStatus(status);
		entityManager.persist(adoption);
		return adoption;
	}

	private ReturnEntity buildReturn(AdoptionEntity adoption, String status) {
		ReturnEntity returnEntity = new ReturnEntity();
		returnEntity.setDate(new Date());
		returnEntity.setReason(REASON);
		returnEntity.setStatus(status);
		returnEntity.setAdoption(adoption);
		returnEntity.setShelter(adoption.getShelter());
		entityManager.persist(returnEntity);
		return returnEntity;
	}

	private ReturnEntity newReturnRequest(AdoptionEntity adoption) {
		ReturnEntity request = new ReturnEntity();
		request.setReason(REASON);
		request.setAdoption(adoption);
		return request;
	}

	private ReturnEntity newUpdate(String reason, String status) {
		ReturnEntity update = new ReturnEntity();
		update.setReason(reason);
		update.setStatus(status);
		return update;
	}

	// ---------------------------------------------------------------- create

	@Test
	void testCreateReturn() throws EntityNotFoundException, IllegalOperationException {
		AdoptionEntity adoption = buildAdoption("FINALIZED");

		ReturnEntity result = returnService.createReturn(newReturnRequest(adoption));

		assertNotNull(result.getId());
		assertNotNull(result.getDate());
		assertEquals(ReturnService.PENDING_STATUS, result.getStatus());
		assertEquals(adoption.getId(), result.getAdoption().getId());
		assertEquals(adoption.getShelter().getId(), result.getShelter().getId());

		ReturnEntity stored = entityManager.find(ReturnEntity.class, result.getId());
		assertEquals(REASON, stored.getReason());
	}

	@Test
	void testCreateReturnWithNullReturn() {
		assertThrows(IllegalOperationException.class, () -> returnService.createReturn(null));
	}

	@Test
	void testCreateReturnWithNullReason() {
		AdoptionEntity adoption = buildAdoption("FINALIZED");
		ReturnEntity request = newReturnRequest(adoption);
		request.setReason(null);
		assertThrows(IllegalOperationException.class, () -> returnService.createReturn(request));
	}

	@Test
	void testCreateReturnWithBlankReason() {
		AdoptionEntity adoption = buildAdoption("FINALIZED");
		ReturnEntity request = newReturnRequest(adoption);
		request.setReason("   ");
		assertThrows(IllegalOperationException.class, () -> returnService.createReturn(request));
	}

	@Test
	void testCreateReturnWithoutAdoption() {
		ReturnEntity request = newReturnRequest(null);
		assertThrows(IllegalOperationException.class, () -> returnService.createReturn(request));
	}

	@Test
	void testCreateReturnWithAdoptionWithoutId() {
		ReturnEntity request = newReturnRequest(new AdoptionEntity());
		assertThrows(IllegalOperationException.class, () -> returnService.createReturn(request));
	}

	@Test
	void testCreateReturnWithNonExistingAdoption() {
		AdoptionEntity ghost = new AdoptionEntity();
		ghost.setId(999999L);
		ReturnEntity request = newReturnRequest(ghost);
		assertThrows(EntityNotFoundException.class, () -> returnService.createReturn(request));
	}

	@Test
	void testCreateReturnWithNotCompletedAdoption() {
		AdoptionEntity adoption = buildAdoption("IN_PROGRESS");
		ReturnEntity request = newReturnRequest(adoption);
		assertThrows(IllegalOperationException.class, () -> returnService.createReturn(request));
	}

	@Test
	void testCreateReturnForPetAlreadyReturned() {
		// La mascota de la primera devolución se adopta de nuevo (otra adopción)
		PetEntity returnedPet = returnList.get(0).getAdoption().getPet();
		AdoptionEntity newAdoption = buildAdoption("FINALIZED", returnedPet);

		ReturnEntity request = newReturnRequest(newAdoption);
		assertThrows(IllegalOperationException.class, () -> returnService.createReturn(request));
	}

	@Test
	void testCreateReturnTwiceForSameAdoption() throws EntityNotFoundException, IllegalOperationException {
		AdoptionEntity adoption = buildAdoption("FINALIZED");
		returnService.createReturn(newReturnRequest(adoption));

		ReturnEntity second = newReturnRequest(adoption);
		assertThrows(IllegalOperationException.class, () -> returnService.createReturn(second));
	}

	// ------------------------------------------------------------------ read

	@Test
	void testReadReturn() throws EntityNotFoundException, IllegalOperationException {
		ReturnEntity expected = returnList.get(0);

		ReturnEntity result = returnService.readReturn(expected.getId());

		assertEquals(expected.getId(), result.getId());
		assertEquals(expected.getReason(), result.getReason());
	}

	@Test
	void testReadReturnWithInvalidId() {
		assertThrows(IllegalOperationException.class, () -> returnService.readReturn(null));
		assertThrows(IllegalOperationException.class, () -> returnService.readReturn(0L));
		assertThrows(IllegalOperationException.class, () -> returnService.readReturn(-1L));
	}

	@Test
	void testReadReturnNotFound() {
		assertThrows(EntityNotFoundException.class, () -> returnService.readReturn(999999L));
	}

	// -------------------------------------------------------------- readAll

	@Test
	void testReadAllReturns() {
		List<ReturnEntity> result = returnService.readAllReturns();
		assertEquals(returnList.size(), result.size());
	}

	@Test
	void testReadAllReturnsFilteredByShelter() {
		ReturnEntity target = returnList.get(0);

		List<ReturnEntity> result = returnService.readAllReturns(target.getShelter().getId(), null, null);

		assertEquals(1, result.size());
		assertEquals(target.getId(), result.get(0).getId());
	}

	@Test
	void testReadAllReturnsFilteredByPet() {
		ReturnEntity target = returnList.get(1);

		List<ReturnEntity> result = returnService.readAllReturns(null, target.getAdoption().getPet().getId(), null);

		assertEquals(1, result.size());
		assertEquals(target.getId(), result.get(0).getId());
	}

	@Test
	void testReadAllReturnsFilteredByUser() {
		ReturnEntity target = returnList.get(2);

		List<ReturnEntity> result = returnService.readAllReturns(null, null,
				target.getAdoption().getAdopter().getId());

		assertEquals(1, result.size());
		assertEquals(target.getId(), result.get(0).getId());
	}

	@Test
	void testReadAllReturnsWithAllFiltersMatching() {
		ReturnEntity target = returnList.get(0);

		List<ReturnEntity> result = returnService.readAllReturns(target.getShelter().getId(),
				target.getAdoption().getPet().getId(), target.getAdoption().getAdopter().getId());

		assertEquals(1, result.size());
		assertEquals(target.getId(), result.get(0).getId());
	}

	@Test
	void testReadAllReturnsWithFiltersThatDoNotMatchTogether() {
		// Cada filtro coincide por separado con una devolución distinta: al
		// combinarlos, todos los criterios deben cumplirse, por lo que no hay resultados
		ReturnEntity first = returnList.get(0);
		ReturnEntity second = returnList.get(1);

		List<ReturnEntity> result = returnService.readAllReturns(first.getShelter().getId(),
				second.getAdoption().getPet().getId(), null);

		assertTrue(result.isEmpty());
	}

	@Test
	void testReadAllReturnsWithUnknownFilter() {
		List<ReturnEntity> result = returnService.readAllReturns(999999L, null, null);
		assertTrue(result.isEmpty());
	}

	// ---------------------------------------------------------------- update

	@Test
	void testUpdateReturn() throws EntityNotFoundException, IllegalOperationException {
		ReturnEntity target = returnList.get(0);

		ReturnEntity result = returnService.updateReturn(target.getId(),
				newUpdate("A new justification", ReturnService.PENDING_STATUS));

		assertEquals("A new justification", result.getReason());
		ReturnEntity stored = entityManager.find(ReturnEntity.class, target.getId());
		assertEquals("A new justification", stored.getReason());
		assertEquals(ReturnService.PENDING_STATUS, stored.getStatus());
	}

	@Test
	void testUpdateReturnToFinalized() throws EntityNotFoundException, IllegalOperationException {
		ReturnEntity target = returnList.get(0);

		ReturnEntity result = returnService.updateReturn(target.getId(), newUpdate(REASON, "finalized"));

		assertEquals(ReturnService.FINALIZED_STATUS, result.getStatus());
	}

	@Test
	void testUpdateFinalizedReturn() {
		AdoptionEntity adoption = buildAdoption("FINALIZED");
		ReturnEntity finalized = buildReturn(adoption, ReturnService.FINALIZED_STATUS);

		assertThrows(IllegalOperationException.class, () -> returnService.updateReturn(finalized.getId(),
				newUpdate("Trying to change it", ReturnService.PENDING_STATUS)));

		ReturnEntity stored = entityManager.find(ReturnEntity.class, finalized.getId());
		assertEquals(REASON, stored.getReason());
	}

	@Test
	void testUpdateReturnWithInvalidId() {
		ReturnEntity update = newUpdate(REASON, ReturnService.PENDING_STATUS);
		assertThrows(IllegalOperationException.class, () -> returnService.updateReturn(null, update));
		assertThrows(IllegalOperationException.class, () -> returnService.updateReturn(0L, update));
		assertThrows(IllegalOperationException.class, () -> returnService.updateReturn(-1L, update));
	}

	@Test
	void testUpdateReturnWithNullAttributes() {
		Long id = returnList.get(0).getId();
		assertThrows(IllegalOperationException.class, () -> returnService.updateReturn(id, null));
		assertThrows(IllegalOperationException.class,
				() -> returnService.updateReturn(id, newUpdate(null, ReturnService.PENDING_STATUS)));
		assertThrows(IllegalOperationException.class,
				() -> returnService.updateReturn(id, newUpdate("  ", ReturnService.PENDING_STATUS)));
		assertThrows(IllegalOperationException.class, () -> returnService.updateReturn(id, newUpdate(REASON, null)));
	}

	@Test
	void testUpdateReturnWithInvalidStatus() {
		Long id = returnList.get(0).getId();
		assertThrows(IllegalOperationException.class,
				() -> returnService.updateReturn(id, newUpdate(REASON, "SOMETHING_ELSE")));
	}

	@Test
	void testUpdateReturnNotFound() {
		ReturnEntity update = newUpdate(REASON, ReturnService.PENDING_STATUS);
		assertThrows(EntityNotFoundException.class, () -> returnService.updateReturn(999999L, update));
	}

	// ---------------------------------------------------------------- delete

	@Test
	void testDeleteReturnIsNotAllowed() {
		Long id = returnList.get(0).getId();

		assertThrows(IllegalOperationException.class, () -> returnService.deleteReturn(id));

		assertNotNull(entityManager.find(ReturnEntity.class, id));
	}

	@Test
	void testDeleteReturnWithInvalidId() {
		assertThrows(IllegalOperationException.class, () -> returnService.deleteReturn(null));
		assertThrows(IllegalOperationException.class, () -> returnService.deleteReturn(0L));
		assertThrows(IllegalOperationException.class, () -> returnService.deleteReturn(-1L));
	}

	@Test
	void testDeleteReturnNotFound() {
		assertThrows(EntityNotFoundException.class, () -> returnService.deleteReturn(999999L));
	}
}
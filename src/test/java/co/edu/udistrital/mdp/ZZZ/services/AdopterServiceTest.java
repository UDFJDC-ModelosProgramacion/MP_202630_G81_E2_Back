package co.edu.udistrital.mdp.ZZZ.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.ZZZ.entities.AdopterEntity;
import co.edu.udistrital.mdp.ZZZ.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.ZZZ.entities.PetEntity;
import co.edu.udistrital.mdp.ZZZ.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.ZZZ.exceptions.IllegalOperationException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(AdopterService.class)
class AdopterServiceTest {

	@Autowired
	private AdopterService adopterService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<AdopterEntity> adopterList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from AdoptionRequestEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			AdopterEntity adopterEntity = factory.manufacturePojo(AdopterEntity.class);
			entityManager.persist(adopterEntity);
			adopterList.add(adopterEntity);
		}
	}

	@Test
	void testCreateAdopter() throws IllegalOperationException {
		AdopterEntity newEntity = factory.manufacturePojo(AdopterEntity.class);

		AdopterEntity result = adopterService.createAdopter(newEntity);

		assertNotNull(result);
		AdopterEntity entity = entityManager.find(AdopterEntity.class, result.getId());
		assertEquals(newEntity.getFirstName(), entity.getFirstName());
		assertEquals(newEntity.getEmail(), entity.getEmail());
		assertEquals(newEntity.getNationalId(), entity.getNationalId());
	}

	@Test
	void testCreateAdopterWithNullFirstName() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity newEntity = factory.manufacturePojo(AdopterEntity.class);
			newEntity.setFirstName(null);
			adopterService.createAdopter(newEntity);
		});
	}

	@Test
	void testCreateAdopterWithEmptyNationalId() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity newEntity = factory.manufacturePojo(AdopterEntity.class);
			newEntity.setNationalId("");
			adopterService.createAdopter(newEntity);
		});
	}

	@Test
	void testCreateAdopterWithEmptyHousingType() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity newEntity = factory.manufacturePojo(AdopterEntity.class);
			newEntity.setHousingType("");
			adopterService.createAdopter(newEntity);
		});
	}

	@Test
	void testCreateAdopterDuplicatedNationalId() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity newEntity = factory.manufacturePojo(AdopterEntity.class);
			newEntity.setNationalId(adopterList.get(0).getNationalId());
			adopterService.createAdopter(newEntity);
		});
	}

	@Test
	void testGetAdopters() {
		List<AdopterEntity> list = adopterService.getAdopters();
		assertEquals(adopterList.size(), list.size());
	}

	@Test
	void testGetAdoptersEmpty() {
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		List<AdopterEntity> list = adopterService.getAdopters();
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetAdoptersFilteredByFirstName() {
		AdopterEntity entity = adopterList.get(0);
		List<AdopterEntity> list = adopterService.getAdopters(entity.getFirstName(), null, null, null);
		assertTrue(list.stream().anyMatch(a -> a.getId().equals(entity.getId())));
	}

	@Test
	void testGetAdoptersFilteredByCombinedCriteria() {
		AdopterEntity entity = adopterList.get(0);
		List<AdopterEntity> list = adopterService.getAdopters(entity.getFirstName(), entity.getLastName(),
				entity.getNationalId(), entity.getHousingType());
		assertEquals(1, list.size());
	}

	@Test
	void testGetAdoptersFilteredNoMatch() {
		AdopterEntity entity = adopterList.get(0);
		List<AdopterEntity> list = adopterService.getAdopters(entity.getFirstName(), "ThisSurnameDoesNotExist", null,
				null);
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetAdopter() throws EntityNotFoundException, IllegalOperationException {
		AdopterEntity entity = adopterList.get(0);
		AdopterEntity resultEntity = adopterService.getAdopter(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
	}

	@Test
	void testGetInvalidAdopterId() {
		assertThrows(IllegalOperationException.class, () -> {
			adopterService.getAdopter(0L);
		});
	}

	@Test
	void testGetNonExistentAdopter() {
		assertThrows(EntityNotFoundException.class, () -> {
			adopterService.getAdopter(1000L);
		});
	}

	@Test
	void testUpdateAdopter() throws EntityNotFoundException, IllegalOperationException {
		AdopterEntity entity = adopterList.get(0);
		AdopterEntity pojoEntity = factory.manufacturePojo(AdopterEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setNationalId(entity.getNationalId());
		pojoEntity.setOccupation("Ingeniero");

		adopterService.updateAdopter(entity.getId(), pojoEntity);

		AdopterEntity resp = entityManager.find(AdopterEntity.class, entity.getId());
		assertEquals(pojoEntity.getOccupation(), resp.getOccupation());
		assertEquals(pojoEntity.getFirstName(), resp.getFirstName());
		assertEquals(entity.getNationalId(), resp.getNationalId());
	}

	@Test
	void testUpdateAdopterInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			AdopterEntity pojoEntity = factory.manufacturePojo(AdopterEntity.class);
			pojoEntity.setId(1000L);
			adopterService.updateAdopter(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateAdopterWithNullEmail() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity entity = adopterList.get(0);
			AdopterEntity pojoEntity = factory.manufacturePojo(AdopterEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setNationalId(entity.getNationalId());
			pojoEntity.setEmail(null);
			adopterService.updateAdopter(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateAdopterChangeNationalId() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity entity = adopterList.get(0);
			AdopterEntity pojoEntity = factory.manufacturePojo(AdopterEntity.class);
			pojoEntity.setId(entity.getId());
			adopterService.updateAdopter(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeleteAdopter() throws EntityNotFoundException, IllegalOperationException {
		AdopterEntity entity = adopterList.get(1);
		adopterService.deleteAdopter(entity.getId());
		AdopterEntity deleted = entityManager.find(AdopterEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInvalidAdopter() {
		assertThrows(EntityNotFoundException.class, () -> {
			adopterService.deleteAdopter(1000L);
		});
	}

	@Test
	void testDeleteAdopterWithApprovedRequest() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity entity = adopterList.get(0);
			AdoptionRequestEntity request = factory.manufacturePojo(AdoptionRequestEntity.class);
			PetEntity pet = factory.manufacturePojo(PetEntity.class);
			entityManager.persist(pet);
			request.setAdopter(entity);
			request.setPet(pet);
			request.setStatus("APPROVED");
			entity.getAdoptionRequests().add(request);
			entityManager.persist(request);
			entityManager.flush();

			adopterService.deleteAdopter(entity.getId());
		});
	}

	@Test
	void testDeleteAdopterWithInProgressRequest() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity entity = adopterList.get(0);
			AdoptionRequestEntity request = factory.manufacturePojo(AdoptionRequestEntity.class);
			PetEntity pet = factory.manufacturePojo(PetEntity.class);
			entityManager.persist(pet);
			request.setAdopter(entity);
			request.setPet(pet);
			request.setStatus("IN_PROGRESS");
			entity.getAdoptionRequests().add(request);
			entityManager.persist(request);
			entityManager.flush();

			adopterService.deleteAdopter(entity.getId());
		});
	}

	@Test
	void testDeleteAdopterWithPendingRequest() throws EntityNotFoundException, IllegalOperationException {
		AdopterEntity entity = adopterList.get(0);
		AdoptionRequestEntity request = factory.manufacturePojo(AdoptionRequestEntity.class);
		PetEntity pet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(pet);
		request.setAdopter(entity);
		request.setPet(pet);
		request.setStatus("PENDING");
		entity.getAdoptionRequests().add(request);
		entityManager.persist(request);
		entityManager.flush();

		adopterService.deleteAdopter(entity.getId());
		AdopterEntity deleted = entityManager.find(AdopterEntity.class, entity.getId());
		assertNull(deleted);
	}
}
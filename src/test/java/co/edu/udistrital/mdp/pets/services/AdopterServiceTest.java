package co.edu.udistrital.mdp.pets.services;
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

import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
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
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationRequestEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			AdopterEntity adopterEntity = buildValidAdopter();
			entityManager.persist(adopterEntity);
			adopterList.add(adopterEntity);
		}
	}

	private AdopterEntity buildValidAdopter() {
		AdopterEntity adopter = factory.manufacturePojo(AdopterEntity.class);
		adopter.setFirstName("Carlos");
		adopter.setLastName("Perez");
		adopter.setEmail("adopter" + System.nanoTime() + "@mail.com");
		adopter.setPassword("secret123");
		adopter.setPhone("300" + System.nanoTime() % 10000000);
		adopter.setAddress("Calle " + System.nanoTime() % 1000);
		adopter.setNationalId("NID-" + System.nanoTime());
		adopter.setOccupation("Ingeniero");
		adopter.setEarnings(2500000.0);
		adopter.setHousingType("Casa");
		adopter.setAllergies("Ninguna");
		adopter.setHasChildren(false);
		adopter.setHasOtherPets(true);
		return adopter;
	}

	@Test
	void testCreateAdopter() throws IllegalOperationException {
		AdopterEntity newEntity = buildValidAdopter();

		AdopterEntity result = adopterService.createAdopter(newEntity);

		assertNotNull(result);
		AdopterEntity persisted = entityManager.find(AdopterEntity.class, result.getId());
		assertNotNull(persisted);
		assertEquals(newEntity.getEmail(), persisted.getEmail());
		assertEquals(newEntity.getNationalId(), persisted.getNationalId());
		assertEquals(newEntity.getAddress(), persisted.getAddress());
	}

	@Test
	void testCreateAdopterWithNullAddress() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity newEntity = buildValidAdopter();
			newEntity.setAddress(null);
			adopterService.createAdopter(newEntity);
		});
	}

	@Test
	void testCreateAdopterWithEmptyHousingType() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity newEntity = buildValidAdopter();
			newEntity.setHousingType("");
			adopterService.createAdopter(newEntity);
		});
	}

	@Test
	void testCreateAdopterWithExistingUserEmail() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity newEntity = buildValidAdopter();
			newEntity.setEmail(adopterList.get(0).getEmail());
			adopterService.createAdopter(newEntity);
		});
	}

	@Test
	void testCreateAdopterWithDuplicateNationalId() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity newEntity = buildValidAdopter();
			newEntity.setNationalId(adopterList.get(0).getNationalId());
			adopterService.createAdopter(newEntity);
		});
	}

	@Test
	void testReadAllAdopters() {
		List<AdopterEntity> list = adopterService.readAllAdopters();
		assertEquals(adopterList.size(), list.size());
	}

	@Test
	void testReadAllAdoptersEmpty() {
		entityManager.getEntityManager().createQuery("delete from AdoptionRequestEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationRequestEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from TrialCohabitationEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		List<AdopterEntity> list = adopterService.readAllAdopters();
		assertTrue(list.isEmpty());
	}

	@Test
	void testReadAllAdoptersFiltered() throws IllegalOperationException {
		AdopterEntity entity = adopterList.get(0);
		List<AdopterEntity> list = adopterService.readAllAdopters(entity.getNationalId(), entity.getHousingType(),
				entity.getOccupation());
		assertNotNull(list);
		assertTrue(list.stream().anyMatch(a -> a.getId().equals(entity.getId())));

		List<AdopterEntity> emptyList = adopterService.readAllAdopters("NONEXISTENT-NID", null, null);
		assertTrue(emptyList.isEmpty());
	}

	@Test
	void testReadAllAdoptersWithEmptyFilter() {
		assertThrows(IllegalOperationException.class, () -> {
			adopterService.readAllAdopters("", null, null);
		});
	}

	@Test
	void testReadAdopter() throws EntityNotFoundException, IllegalOperationException {
		AdopterEntity entity = adopterList.get(0);
		AdopterEntity resultEntity = adopterService.readAdopter(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
		assertEquals(entity.getNationalId(), resultEntity.getNationalId());
	}

	@Test
	void testReadInvalidAdopterId() {
		assertThrows(IllegalOperationException.class, () -> {
			adopterService.readAdopter(0L);
		});
	}

	@Test
	void testReadNonExistentAdopter() {
		assertThrows(EntityNotFoundException.class, () -> {
			adopterService.readAdopter(1000L);
		});
	}

	@Test
	void testUpdateAdopter() throws EntityNotFoundException, IllegalOperationException {
		AdopterEntity entity = adopterList.get(0);
		AdopterEntity pojoEntity = buildValidAdopter();
		pojoEntity.setId(entity.getId());
		pojoEntity.setAddress("Avenida 9");
		pojoEntity.setOccupation("Medico");
		pojoEntity.setEarnings(3500000.0);
		pojoEntity.setHousingType("Apartamento");
		pojoEntity.setAllergies("Polen");
		pojoEntity.setHasChildren(true);
		pojoEntity.setHasOtherPets(false);

		adopterService.updateAdopter(entity.getId(), pojoEntity);

		AdopterEntity resp = entityManager.find(AdopterEntity.class, entity.getId());
		assertEquals("Avenida 9", resp.getAddress());
		assertEquals("Medico", resp.getOccupation());
		assertEquals("Apartamento", resp.getHousingType());
		assertEquals(entity.getEmail(), resp.getEmail());
		assertEquals(entity.getNationalId(), resp.getNationalId());
	}

	@Test
	void testUpdateAdopterInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			AdopterEntity pojoEntity = buildValidAdopter();
			pojoEntity.setId(1000L);
			adopterService.updateAdopter(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateAdopterWithNullAddress() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity entity = adopterList.get(0);
			AdopterEntity pojoEntity = buildValidAdopter();
			pojoEntity.setId(entity.getId());
			pojoEntity.setAddress(null);
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
	void testDeleteAdopterWithActiveAdoptionRequest() {
		assertThrows(IllegalOperationException.class, () -> {
			AdopterEntity entity = adopterList.get(0);
			AdoptionRequestEntity request = factory.manufacturePojo(AdoptionRequestEntity.class);
			request.setAdopter(entity);
			request.setStatus("APPROVED");
			entityManager.persist(request);
			entity.getAdoptionRequests().add(request);

			adopterService.deleteAdopter(entity.getId());
		});
	}
}
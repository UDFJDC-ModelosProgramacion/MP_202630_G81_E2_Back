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

import co.edu.udistrital.mdp.pets.entities.FollowUpEntity;
import co.edu.udistrital.mdp.pets.entities.MedicalEventEntity;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.VeterinarianService;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(VeterinarianService.class)
class VeterinarianServiceTest {

	@Autowired
	private VeterinarianService veterinarianService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<VeterinarianEntity> veterinarianList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from MedicalEventEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from FollowUpEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from VeterinarianEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			VeterinarianEntity veterinarianEntity = factory.manufacturePojo(VeterinarianEntity.class);
			entityManager.persist(veterinarianEntity);
			veterinarianList.add(veterinarianEntity);
		}
	}

	@Test
	void testCreateVeterinarian() throws IllegalOperationException {
		VeterinarianEntity newEntity = factory.manufacturePojo(VeterinarianEntity.class);

		VeterinarianEntity result = veterinarianService.createVeterinarian(newEntity);

		assertNotNull(result);
		VeterinarianEntity entity = entityManager.find(VeterinarianEntity.class, result.getId());
		assertEquals(newEntity.getFirstName(), entity.getFirstName());
		assertEquals(newEntity.getEmail(), entity.getEmail());
		assertEquals(newEntity.getSpecialization(), entity.getSpecialization());
	}

	@Test
	void testCreateVeterinarianWithNullSpecialization() {
		assertThrows(IllegalOperationException.class, () -> {
			VeterinarianEntity newEntity = factory.manufacturePojo(VeterinarianEntity.class);
			newEntity.setSpecialization(null);
			veterinarianService.createVeterinarian(newEntity);
		});
	}

	@Test
	void testCreateVeterinarianWithEmptyAvailability() {
		assertThrows(IllegalOperationException.class, () -> {
			VeterinarianEntity newEntity = factory.manufacturePojo(VeterinarianEntity.class);
			newEntity.setAvailability("");
			veterinarianService.createVeterinarian(newEntity);
		});
	}

	@Test
	void testCreateVeterinarianWithExistingUserEmail() {
		assertThrows(IllegalOperationException.class, () -> {
			VeterinarianEntity newEntity = factory.manufacturePojo(VeterinarianEntity.class);
			newEntity.setEmail(veterinarianList.get(0).getEmail());
			veterinarianService.createVeterinarian(newEntity);
		});
	}

	@Test
	void testGetVeterinarians() {
		List<VeterinarianEntity> list = veterinarianService.getVeterinarians();
		assertEquals(veterinarianList.size(), list.size());
	}

	@Test
	void testGetVeterinariansEmpty() {
		entityManager.getEntityManager().createQuery("delete from VeterinarianEntity").executeUpdate();
		List<VeterinarianEntity> list = veterinarianService.getVeterinarians();
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetVeterinariansFilteredBySpecializationAndAvailability() {
		VeterinarianEntity entity = veterinarianList.get(0);
		List<VeterinarianEntity> list = veterinarianService.getVeterinarians(entity.getSpecialization(),
				entity.getAvailability());
		assertTrue(list.stream().anyMatch(v -> v.getId().equals(entity.getId())));
	}

	@Test
	void testGetVeterinarian() throws EntityNotFoundException, IllegalOperationException {
		VeterinarianEntity entity = veterinarianList.get(0);
		VeterinarianEntity resultEntity = veterinarianService.getVeterinarian(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
	}

	@Test
	void testGetInvalidVeterinarianId() {
		assertThrows(IllegalOperationException.class, () -> {
			veterinarianService.getVeterinarian(0L);
		});
	}

	@Test
	void testGetNonExistentVeterinarian() {
		assertThrows(EntityNotFoundException.class, () -> {
			veterinarianService.getVeterinarian(1000L);
		});
	}

	@Test
	void testUpdateVeterinarian() throws EntityNotFoundException, IllegalOperationException {
		VeterinarianEntity entity = veterinarianList.get(0);
		VeterinarianEntity pojoEntity = factory.manufacturePojo(VeterinarianEntity.class);
		pojoEntity.setId(entity.getId());

		veterinarianService.updateVeterinarian(entity.getId(), pojoEntity);

		VeterinarianEntity resp = entityManager.find(VeterinarianEntity.class, entity.getId());
		assertEquals(pojoEntity.getSpecialization(), resp.getSpecialization());
		assertEquals(pojoEntity.getAvailability(), resp.getAvailability());
		assertEquals(entity.getEmail(), resp.getEmail());
	}

	@Test
	void testUpdateVeterinarianInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			VeterinarianEntity pojoEntity = factory.manufacturePojo(VeterinarianEntity.class);
			pojoEntity.setId(1000L);
			veterinarianService.updateVeterinarian(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateVeterinarianWithNullAvailability() {
		assertThrows(IllegalOperationException.class, () -> {
			VeterinarianEntity entity = veterinarianList.get(0);
			VeterinarianEntity pojoEntity = factory.manufacturePojo(VeterinarianEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setAvailability(null);
			veterinarianService.updateVeterinarian(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeleteVeterinarian() throws EntityNotFoundException, IllegalOperationException {
		VeterinarianEntity entity = veterinarianList.get(1);
		veterinarianService.deleteVeterinarian(entity.getId());
		VeterinarianEntity deleted = entityManager.find(VeterinarianEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInvalidVeterinarian() {
		assertThrows(EntityNotFoundException.class, () -> {
			veterinarianService.deleteVeterinarian(1000L);
		});
	}

	@Test
	void testDeleteVeterinarianWithMedicalEvents() {
		assertThrows(IllegalOperationException.class, () -> {
			VeterinarianEntity entity = veterinarianList.get(0);
			MedicalEventEntity medicalEvent = factory.manufacturePojo(MedicalEventEntity.class);
			medicalEvent.setVeterinarian(entity);
			entityManager.persist(medicalEvent);
			entity.getMedicalEvents().add(medicalEvent);

			veterinarianService.deleteVeterinarian(entity.getId());
		});
	}

	@Test
	void testDeleteVeterinarianWithFollowUps() {
		assertThrows(IllegalOperationException.class, () -> {
			VeterinarianEntity entity = veterinarianList.get(0);
			FollowUpEntity followUp = factory.manufacturePojo(FollowUpEntity.class);
			followUp.setVeterinarian(entity);
			entityManager.persist(followUp);
			entity.getFollowUps().add(followUp);

			veterinarianService.deleteVeterinarian(entity.getId());
		});
	}
}

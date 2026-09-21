package co.edu.udistrital.mdp.pets.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.MedicalEventEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(MedicalEventService.class)
class MedicalEventServiceTest {

	@Autowired
	private MedicalEventService medicalEventService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<MedicalEventEntity> medicalEventList = new ArrayList<>();
	private List<PetEntity> petList = new ArrayList<>();
	private List<ShelterEntity> shelterList = new ArrayList<>();
	private List<VeterinarianEntity> veterinarianList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from MedicalEventEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ShelterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from VeterinarianEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			ShelterEntity shelterEntity = factory.manufacturePojo(ShelterEntity.class);
			entityManager.persist(shelterEntity);
			shelterList.add(shelterEntity);
		}
		for (int i = 0; i < 3; i++) {
			PetEntity petEntity = factory.manufacturePojo(PetEntity.class);
			petEntity.setShelter(shelterList.get(0));
			entityManager.persist(petEntity);
			petList.add(petEntity);
		}
		for (int i = 0; i < 3; i++) {
			VeterinarianEntity veterinarianEntity = factory.manufacturePojo(VeterinarianEntity.class);
			veterinarianEntity.setShelter(shelterList.get(0));
			entityManager.persist(veterinarianEntity);
			veterinarianList.add(veterinarianEntity);
		}
		for (int i = 0; i < 3; i++) {
			MedicalEventEntity medicalEventEntity = factory.manufacturePojo(MedicalEventEntity.class);
			medicalEventEntity.setDate(pastDate());
			medicalEventEntity.setPet(petList.get(0));
			medicalEventEntity.setVeterinarian(veterinarianList.get(0));
			entityManager.persist(medicalEventEntity);
			medicalEventList.add(medicalEventEntity);
		}
	}

	private Date pastDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -5);
		return calendar.getTime();
	}

	private Date futureDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, 5);
		return calendar.getTime();
	}

	@Test
	void testCreateMedicalEvent() throws EntityNotFoundException, IllegalOperationException {
		MedicalEventEntity newEntity = factory.manufacturePojo(MedicalEventEntity.class);
		newEntity.setDate(pastDate());
		newEntity.setPet(petList.get(1));
		newEntity.setVeterinarian(veterinarianList.get(1));

		MedicalEventEntity result = medicalEventService.createMedicalEvent(newEntity);

		assertNotNull(result);
		MedicalEventEntity entity = entityManager.find(MedicalEventEntity.class, result.getId());
		assertEquals(newEntity.getType(), entity.getType());
		assertEquals(newEntity.getDescription(), entity.getDescription());
		assertEquals(newEntity.getPet().getId(), entity.getPet().getId());
		assertEquals(newEntity.getVeterinarian().getId(), entity.getVeterinarian().getId());
	}

	@Test
	void testCreateMedicalEventWithFutureDate() {
		assertThrows(IllegalOperationException.class, () -> {
			MedicalEventEntity newEntity = factory.manufacturePojo(MedicalEventEntity.class);
			newEntity.setDate(futureDate());
			newEntity.setPet(petList.get(1));
			newEntity.setVeterinarian(veterinarianList.get(1));
			medicalEventService.createMedicalEvent(newEntity);
		});
	}

	@Test
	void testCreateMedicalEventWithNullDate() {
		assertThrows(IllegalOperationException.class, () -> {
			MedicalEventEntity newEntity = factory.manufacturePojo(MedicalEventEntity.class);
			newEntity.setDate(null);
			newEntity.setPet(petList.get(1));
			newEntity.setVeterinarian(veterinarianList.get(1));
			medicalEventService.createMedicalEvent(newEntity);
		});
	}

	@Test
	void testCreateMedicalEventWithInvalidPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			MedicalEventEntity newEntity = factory.manufacturePojo(MedicalEventEntity.class);
			newEntity.setDate(pastDate());
			PetEntity pet = new PetEntity();
			pet.setId(0L);
			newEntity.setPet(pet);
			newEntity.setVeterinarian(veterinarianList.get(1));
			medicalEventService.createMedicalEvent(newEntity);
		});
	}

	@Test
	void testCreateMedicalEventWithInvalidVeterinarian() {
		assertThrows(EntityNotFoundException.class, () -> {
			MedicalEventEntity newEntity = factory.manufacturePojo(MedicalEventEntity.class);
			newEntity.setDate(pastDate());
			newEntity.setPet(petList.get(1));
			VeterinarianEntity vet = new VeterinarianEntity();
			vet.setId(0L);
			newEntity.setVeterinarian(vet);
			medicalEventService.createMedicalEvent(newEntity);
		});
	}

	@Test
	void testGetMedicalEvents() {
		List<MedicalEventEntity> list = medicalEventService.getMedicalEvents();
		assertEquals(medicalEventList.size(), list.size());
	}

	@Test
	void testGetMedicalEvent() throws EntityNotFoundException, IllegalOperationException {
		MedicalEventEntity entity = medicalEventList.get(0);
		MedicalEventEntity resultEntity = medicalEventService.getMedicalEvent(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
		assertEquals(entity.getType(), resultEntity.getType());
	}

	@Test
	void testGetMedicalEventInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			medicalEventService.getMedicalEvent(0L);
		});
	}

	@Test
	void testGetNonExistentMedicalEvent() {
		assertThrows(EntityNotFoundException.class, () -> {
			medicalEventService.getMedicalEvent(1000L);
		});
	}

	@Test
	void testUpdateMedicalEvent() throws EntityNotFoundException, IllegalOperationException {
		MedicalEventEntity entity = medicalEventList.get(0);
		MedicalEventEntity pojoEntity = factory.manufacturePojo(MedicalEventEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setDate(pastDate());
		pojoEntity.setVeterinarian(entity.getVeterinarian());

		medicalEventService.updateMedicalEvent(entity.getId(), pojoEntity);

		MedicalEventEntity resp = entityManager.find(MedicalEventEntity.class, entity.getId());
		assertEquals(pojoEntity.getType(), resp.getType());
		assertEquals(pojoEntity.getDescription(), resp.getDescription());
		assertEquals(entity.getPet().getId(), resp.getPet().getId());
	}

	@Test
	void testUpdateMedicalEventInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			MedicalEventEntity pojoEntity = factory.manufacturePojo(MedicalEventEntity.class);
			pojoEntity.setId(0L);
			pojoEntity.setDate(pastDate());
			medicalEventService.updateMedicalEvent(0L, pojoEntity);
		});
	}

	@Test
	void testUpdateNonExistentMedicalEvent() {
		assertThrows(EntityNotFoundException.class, () -> {
			MedicalEventEntity pojoEntity = factory.manufacturePojo(MedicalEventEntity.class);
			pojoEntity.setId(1000L);
			pojoEntity.setDate(pastDate());
			medicalEventService.updateMedicalEvent(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateMedicalEventWithFutureDate() {
		assertThrows(IllegalOperationException.class, () -> {
			MedicalEventEntity entity = medicalEventList.get(0);
			MedicalEventEntity pojoEntity = factory.manufacturePojo(MedicalEventEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setDate(futureDate());
			medicalEventService.updateMedicalEvent(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeleteMedicalEvent() throws EntityNotFoundException, IllegalOperationException {
		MedicalEventEntity entity = medicalEventList.get(0);
		medicalEventService.deleteMedicalEvent(entity.getId());
		MedicalEventEntity deleted = entityManager.find(MedicalEventEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteMedicalEventInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			medicalEventService.deleteMedicalEvent(0L);
		});
	}

	@Test
	void testDeleteNonExistentMedicalEvent() {
		assertThrows(EntityNotFoundException.class, () -> {
			medicalEventService.deleteMedicalEvent(1000L);
		});
	}
}
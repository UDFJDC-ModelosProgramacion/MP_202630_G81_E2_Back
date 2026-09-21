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

	private static final Long NON_EXISTENT_ID = 1000L;
	private static final Long INVALID_ID = 0L;

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
			MedicalEventEntity medicalEventEntity = newMedicalEvent(pastDate(), petList.get(0),
					veterinarianList.get(0));
			entityManager.persist(medicalEventEntity);
			medicalEventList.add(medicalEventEntity);
		}
	}

	private MedicalEventEntity newMedicalEvent(Date date, PetEntity pet, VeterinarianEntity veterinarian) {
		MedicalEventEntity medicalEvent = factory.manufacturePojo(MedicalEventEntity.class);
		medicalEvent.setDate(date);
		medicalEvent.setPet(pet);
		medicalEvent.setVeterinarian(veterinarian);
		return medicalEvent;
	}

	private PetEntity petWithId(Long id) {
		PetEntity pet = new PetEntity();
		pet.setId(id);
		return pet;
	}

	private VeterinarianEntity veterinarianWithId(Long id) {
		VeterinarianEntity veterinarian = new VeterinarianEntity();
		veterinarian.setId(id);
		return veterinarian;
	}

	private Date pastDate() {
		return daysFromNow(-5);
	}

	private Date futureDate() {
		return daysFromNow(5);
	}

	private Date daysFromNow(int days) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, days);
		return calendar.getTime();
	}

	// ---------------------------------------------------------------- create

	@Test
	void testCreateMedicalEvent() throws EntityNotFoundException, IllegalOperationException {
		MedicalEventEntity newEntity = newMedicalEvent(pastDate(), petList.get(1), veterinarianList.get(1));

		MedicalEventEntity result = medicalEventService.createMedicalEvent(newEntity);

		assertNotNull(result);
		MedicalEventEntity entity = entityManager.find(MedicalEventEntity.class, result.getId());
		assertEquals(newEntity.getType(), entity.getType());
		assertEquals(newEntity.getDescription(), entity.getDescription());
		assertEquals(newEntity.getPet().getId(), entity.getPet().getId());
		assertEquals(newEntity.getVeterinarian().getId(), entity.getVeterinarian().getId());
	}

	@Test
	void testCreateNullMedicalEvent() {
		assertThrows(IllegalOperationException.class, () -> medicalEventService.createMedicalEvent(null));
	}

	@Test
	void testCreateMedicalEventWithFutureDate() {
		MedicalEventEntity newEntity = newMedicalEvent(futureDate(), petList.get(1), veterinarianList.get(1));
		assertThrows(IllegalOperationException.class, () -> medicalEventService.createMedicalEvent(newEntity));
	}

	@Test
	void testCreateMedicalEventWithNullDate() {
		MedicalEventEntity newEntity = newMedicalEvent(null, petList.get(1), veterinarianList.get(1));
		assertThrows(IllegalOperationException.class, () -> medicalEventService.createMedicalEvent(newEntity));
	}

	@Test
	void testCreateMedicalEventWithBlankType() {
		MedicalEventEntity newEntity = newMedicalEvent(pastDate(), petList.get(1), veterinarianList.get(1));
		newEntity.setType("   ");
		assertThrows(IllegalOperationException.class, () -> medicalEventService.createMedicalEvent(newEntity));
	}

	@Test
	void testCreateMedicalEventWithNullPet() {
		MedicalEventEntity newEntity = newMedicalEvent(pastDate(), null, veterinarianList.get(1));
		assertThrows(IllegalOperationException.class, () -> medicalEventService.createMedicalEvent(newEntity));
	}

	@Test
	void testCreateMedicalEventWithPetWithoutId() {
		MedicalEventEntity newEntity = newMedicalEvent(pastDate(), new PetEntity(), veterinarianList.get(1));
		assertThrows(IllegalOperationException.class, () -> medicalEventService.createMedicalEvent(newEntity));
	}

	@Test
	void testCreateMedicalEventWithInvalidPet() {
		MedicalEventEntity newEntity = newMedicalEvent(pastDate(), petWithId(INVALID_ID), veterinarianList.get(1));
		assertThrows(EntityNotFoundException.class, () -> medicalEventService.createMedicalEvent(newEntity));
	}

	@Test
	void testCreateMedicalEventWithNullVeterinarian() {
		MedicalEventEntity newEntity = newMedicalEvent(pastDate(), petList.get(1), null);
		assertThrows(IllegalOperationException.class, () -> medicalEventService.createMedicalEvent(newEntity));
	}

	@Test
	void testCreateMedicalEventWithInvalidVeterinarian() {
		MedicalEventEntity newEntity = newMedicalEvent(pastDate(), petList.get(1), veterinarianWithId(INVALID_ID));
		assertThrows(EntityNotFoundException.class, () -> medicalEventService.createMedicalEvent(newEntity));
	}

	// ------------------------------------------------------------------- get

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
		assertThrows(IllegalOperationException.class, () -> medicalEventService.getMedicalEvent(INVALID_ID));
	}

	@Test
	void testGetNonExistentMedicalEvent() {
		assertThrows(EntityNotFoundException.class, () -> medicalEventService.getMedicalEvent(NON_EXISTENT_ID));
	}

	// ---------------------------------------------------------------- update

	@Test
	void testUpdateMedicalEvent() throws EntityNotFoundException, IllegalOperationException {
		MedicalEventEntity entity = medicalEventList.get(0);
		MedicalEventEntity update = newMedicalEvent(pastDate(), null, veterinarianList.get(1));

		medicalEventService.updateMedicalEvent(entity.getId(), update);

		MedicalEventEntity resp = entityManager.find(MedicalEventEntity.class, entity.getId());
		assertEquals(update.getType(), resp.getType());
		assertEquals(update.getDescription(), resp.getDescription());
		assertEquals(petList.get(0).getId(), resp.getPet().getId());
		assertEquals(veterinarianList.get(1).getId(), resp.getVeterinarian().getId());
	}

	@Test
	void testUpdateMedicalEventKeepsVeterinarianWhenNotProvided()
			throws EntityNotFoundException, IllegalOperationException {
		MedicalEventEntity entity = medicalEventList.get(0);
		MedicalEventEntity update = newMedicalEvent(pastDate(), null, null);

		medicalEventService.updateMedicalEvent(entity.getId(), update);

		MedicalEventEntity resp = entityManager.find(MedicalEventEntity.class, entity.getId());
		assertEquals(veterinarianList.get(0).getId(), resp.getVeterinarian().getId());
	}

	@Test
	void testUpdateMedicalEventInvalidId() {
		MedicalEventEntity update = newMedicalEvent(pastDate(), null, null);
		assertThrows(IllegalOperationException.class, () -> medicalEventService.updateMedicalEvent(INVALID_ID, update));
	}

	@Test
	void testUpdateNonExistentMedicalEvent() {
		MedicalEventEntity update = newMedicalEvent(pastDate(), null, null);
		assertThrows(EntityNotFoundException.class,
				() -> medicalEventService.updateMedicalEvent(NON_EXISTENT_ID, update));
	}

	@Test
	void testUpdateMedicalEventWithNullEntity() {
		Long id = medicalEventList.get(0).getId();
		assertThrows(IllegalOperationException.class, () -> medicalEventService.updateMedicalEvent(id, null));
	}

	@Test
	void testUpdateMedicalEventWithFutureDate() {
		Long id = medicalEventList.get(0).getId();
		MedicalEventEntity update = newMedicalEvent(futureDate(), null, null);
		assertThrows(IllegalOperationException.class, () -> medicalEventService.updateMedicalEvent(id, update));
	}

	@Test
	void testUpdateMedicalEventWithBlankType() {
		Long id = medicalEventList.get(0).getId();
		MedicalEventEntity update = newMedicalEvent(pastDate(), null, null);
		update.setType("");
		assertThrows(IllegalOperationException.class, () -> medicalEventService.updateMedicalEvent(id, update));
	}

	@Test
	void testUpdateMedicalEventWithInvalidVeterinarian() {
		Long id = medicalEventList.get(0).getId();
		MedicalEventEntity update = newMedicalEvent(pastDate(), null, veterinarianWithId(INVALID_ID));
		assertThrows(EntityNotFoundException.class, () -> medicalEventService.updateMedicalEvent(id, update));
	}

	// ---------------------------------------------------------------- delete

	@Test
	void testDeleteMedicalEvent() throws EntityNotFoundException, IllegalOperationException {
		MedicalEventEntity entity = medicalEventList.get(0);
		medicalEventService.deleteMedicalEvent(entity.getId());
		MedicalEventEntity deleted = entityManager.find(MedicalEventEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteMedicalEventInvalidId() {
		assertThrows(IllegalOperationException.class, () -> medicalEventService.deleteMedicalEvent(INVALID_ID));
	}

	@Test
	void testDeleteNonExistentMedicalEvent() {
		assertThrows(EntityNotFoundException.class, () -> medicalEventService.deleteMedicalEvent(NON_EXISTENT_ID));
	}
}
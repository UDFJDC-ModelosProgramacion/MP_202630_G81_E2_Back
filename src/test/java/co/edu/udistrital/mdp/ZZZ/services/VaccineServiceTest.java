package co.edu.udistrital.mdp.ZZZ.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import co.edu.udistrital.mdp.ZZZ.entities.AdoptionEntity;
import co.edu.udistrital.mdp.ZZZ.entities.PetEntity;
import co.edu.udistrital.mdp.ZZZ.entities.VaccinationRecordEntity;
import co.edu.udistrital.mdp.ZZZ.entities.VaccineEntity;
import co.edu.udistrital.mdp.ZZZ.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.ZZZ.exceptions.IllegalOperationException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(VaccineService.class)
class VaccineServiceTest {

	@Autowired
	private VaccineService vaccineService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<VaccineEntity> vaccineList = new ArrayList<>();
	private VaccinationRecordEntity record = new VaccinationRecordEntity();
	private PetEntity pet = new PetEntity();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from VaccineEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from VaccinationRecordEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
	}

	private void insertData() {
		pet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(pet);

		record = factory.manufacturePojo(VaccinationRecordEntity.class);
		record.setPet(pet);
		entityManager.persist(record);

		for (int i = 0; i < 3; i++) {
			VaccineEntity vaccineEntity = factory.manufacturePojo(VaccineEntity.class);
			vaccineEntity.setAdministrationDate(pastDate(30 + i));
			vaccineEntity.setNextAdministration(futureDate(30 + i));
			vaccineEntity.setStatus(true);
			vaccineEntity.setVaccinationRecord(record);
			entityManager.persist(vaccineEntity);
			vaccineList.add(vaccineEntity);
		}
	}

	private Date pastDate(int daysAgo) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, -daysAgo);
		return c.getTime();
	}

	private Date futureDate(int daysFromNow) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, daysFromNow);
		return c.getTime();
	}

	private VaccineEntity newValidVaccine() {
		VaccineEntity vaccine = factory.manufacturePojo(VaccineEntity.class);
		vaccine.setAdministrationDate(pastDate(1));
		vaccine.setNextAdministration(futureDate(100));
		vaccine.setStatus(true);
		vaccine.setVaccinationRecord(record);
		return vaccine;
	}

	@Test
	void testCreateVaccine() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity newEntity = newValidVaccine();

		VaccineEntity result = vaccineService.createVaccine(newEntity);

		assertNotNull(result);
		VaccineEntity entity = entityManager.find(VaccineEntity.class, result.getId());
		assertEquals(newEntity.getName(), entity.getName());
		assertEquals(record.getId(), entity.getVaccinationRecord().getId());
	}

	@Test
	void testCreateVaccineWithNullName() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setName(null);
			vaccineService.createVaccine(newEntity);
		});
	}

	@Test
	void testCreateVaccineWithNullStatus() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setStatus(null);
			vaccineService.createVaccine(newEntity);
		});
	}

	@Test
	void testCreateVaccineWithFutureAdministrationDate() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setAdministrationDate(futureDate(5));
			vaccineService.createVaccine(newEntity);
		});
	}

	@Test
	void testCreateVaccineWithInvalidNextAdministration() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setNextAdministration(pastDate(10));
			vaccineService.createVaccine(newEntity);
		});
	}

	@Test
	void testCreateVaccineWithNonExistentRecord() {
		assertThrows(EntityNotFoundException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			VaccinationRecordEntity fakeRecord = new VaccinationRecordEntity();
			fakeRecord.setId(0L);
			newEntity.setVaccinationRecord(fakeRecord);
			vaccineService.createVaccine(newEntity);
		});
	}

	@Test
	void testCreateDuplicatedVaccine() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity existing = vaccineList.get(0);
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setName(existing.getName());
			newEntity.setAdministrationDate(existing.getAdministrationDate());
			vaccineService.createVaccine(newEntity);
		});
	}

	@Test
	void testGetVaccines() {
		List<VaccineEntity> list = vaccineService.getVaccines();
		assertEquals(vaccineList.size(), list.size());
	}

	@Test
	void testGetVaccinesEmpty() {
		entityManager.getEntityManager().createQuery("delete from VaccineEntity").executeUpdate();
		List<VaccineEntity> list = vaccineService.getVaccines();
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetVaccinesFilteredByRecordAndPet() {
		List<VaccineEntity> list = vaccineService.getVaccines(record.getId(), pet.getId(), null);
		assertEquals(vaccineList.size(), list.size());

		List<VaccineEntity> emptyList = vaccineService.getVaccines(1000L, null, null);
		assertTrue(emptyList.isEmpty());
	}

	@Test
	void testGetVaccine() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		VaccineEntity result = vaccineService.getVaccine(entity.getId());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testGetVaccineInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.getVaccine(0L);
		});
	}

	@Test
	void testGetNonExistentVaccine() {
		assertThrows(EntityNotFoundException.class, () -> {
			vaccineService.getVaccine(1000L);
		});
	}

	@Test
	void testGetVaccineByNameAndRecord() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		VaccineEntity result = vaccineService.getVaccine(null, entity.getName(), record.getId());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testAutomaticStatusUpdateWhenNextAdministrationPassed() {
		VaccineEntity expired = factory.manufacturePojo(VaccineEntity.class);
		expired.setAdministrationDate(pastDate(60));
		expired.setNextAdministration(pastDate(1));
		expired.setStatus(true);
		expired.setVaccinationRecord(record);
		entityManager.persist(expired);

		List<VaccineEntity> list = vaccineService.getVaccines();
		VaccineEntity updated = list.stream().filter(v -> v.getId().equals(expired.getId())).findFirst().orElseThrow();
		assertFalse(updated.getStatus());
	}

	@Test
	void testUpdateVaccine() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setStatus(true);
		pojoEntity.setNextAdministration(futureDate(200));
		pojoEntity.setAdministrationDate(entity.getAdministrationDate());

		vaccineService.updateVaccine(entity.getId(), pojoEntity);

		VaccineEntity resp = entityManager.find(VaccineEntity.class, entity.getId());
		assertEquals(pojoEntity.getName(), resp.getName());
		assertEquals(entity.getAdministrationDate(), resp.getAdministrationDate());
	}

	@Test
	void testUpdateVaccineInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
			pojoEntity.setStatus(true);
			vaccineService.updateVaccine(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateVaccineChangingAdministrationDate() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setStatus(true);
			pojoEntity.setAdministrationDate(pastDate(1));
			vaccineService.updateVaccine(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testDeleteVaccine() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		vaccineService.deleteVaccine(entity.getId());
		VaccineEntity deleted = entityManager.find(VaccineEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInvalidVaccine() {
		assertThrows(EntityNotFoundException.class, () -> {
			vaccineService.deleteVaccine(1000L);
		});
	}

	@Test
	void testDeleteVaccineOfAdoptedPet() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
			adoption.setPet(pet);
			adoption.setStatus("FINALIZED");
			entityManager.persist(adoption);
			vaccineService.deleteVaccine(entity.getId());
		});
	}
}
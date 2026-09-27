package co.edu.udistrital.mdp.pets.services;

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

import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.VaccinationRecordEntity;
import co.edu.udistrital.mdp.pets.entities.VaccineEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
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
	private VaccinationRecordEntity vaccinationRecord = new VaccinationRecordEntity();
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

		vaccinationRecord = factory.manufacturePojo(VaccinationRecordEntity.class);
		vaccinationRecord.setPet(pet);
		entityManager.persist(vaccinationRecord);

		for (int i = 0; i < 3; i++) {
			VaccineEntity vaccineEntity = factory.manufacturePojo(VaccineEntity.class);
			vaccineEntity.setAdministrationDate(pastDate(30 + i));
			vaccineEntity.setNextAdministration(futureDate(30 + i));
			vaccineEntity.setStatus(true);
			vaccineEntity.setVaccinationRecord(vaccinationRecord);
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
		vaccine.setVaccinationRecord(vaccinationRecord);
		return vaccine;
	}

	// Test for createVaccine

	@Test
	void testCreateVaccine() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity newEntity = newValidVaccine();

		VaccineEntity result = vaccineService.createVaccine(newEntity);

		assertNotNull(result);
		VaccineEntity entity = entityManager.find(VaccineEntity.class, result.getId());
		assertEquals(newEntity.getName(), entity.getName());
		assertEquals(vaccinationRecord.getId(), entity.getVaccinationRecord().getId());
	}

	@Test
	void testCreateVaccineWithoutNextAdministration() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity newEntity = newValidVaccine();
		newEntity.setNextAdministration(null);

		VaccineEntity result = vaccineService.createVaccine(newEntity);

		assertNotNull(result);
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
	void testCreateVaccineWithBlankName() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setName("   ");
			vaccineService.createVaccine(newEntity);
		});
	}

	@Test
	void testCreateVaccineWithNullAdministrationDate() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setAdministrationDate(null);
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
	void testCreateVaccineWithNextAdministrationEqualToAdministrationDate() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setNextAdministration(newEntity.getAdministrationDate());
			vaccineService.createVaccine(newEntity);
		});
	}

	@Test
	void testCreateVaccineWithNullVaccinationRecord() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setVaccinationRecord(null);
			vaccineService.createVaccine(newEntity);
		});
	}

	@Test
	void testCreateVaccineWithVaccinationRecordWithoutId() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			VaccinationRecordEntity recordNoId = new VaccinationRecordEntity();
			// id intentionally left null
			newEntity.setVaccinationRecord(recordNoId);
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
	void testCreateVaccineSameNameAndDateDifferentRecordIsNotDuplicate()
			throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity existing = vaccineList.get(0);

		PetEntity otherPet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(otherPet);
		VaccinationRecordEntity otherRecord = factory.manufacturePojo(VaccinationRecordEntity.class);
		otherRecord.setPet(otherPet);
		entityManager.persist(otherRecord);

		VaccineEntity newEntity = newValidVaccine();
		newEntity.setName(existing.getName());
		newEntity.setAdministrationDate(existing.getAdministrationDate());
		newEntity.setVaccinationRecord(otherRecord);

		VaccineEntity result = vaccineService.createVaccine(newEntity);

		assertNotNull(result);
	}

	// Test for getVaccines 

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
	void testGetVaccinesDoesNotDowngradeValidStatus() {
		List<VaccineEntity> list = vaccineService.getVaccines();
		VaccineEntity entity = list.stream().filter(v -> v.getId().equals(vaccineList.get(0).getId())).findFirst()
				.orElseThrow();
		assertTrue(entity.getStatus());
	}

	@Test
	void testGetVaccinesDoesNotDowngradeWhenNextAdministrationNull() {
		VaccineEntity noNext = factory.manufacturePojo(VaccineEntity.class);
		noNext.setAdministrationDate(pastDate(10));
		noNext.setNextAdministration(null);
		noNext.setStatus(true);
		noNext.setVaccinationRecord(vaccinationRecord);
		entityManager.persist(noNext);

		List<VaccineEntity> list = vaccineService.getVaccines();
		VaccineEntity updated = list.stream().filter(v -> v.getId().equals(noNext.getId())).findFirst().orElseThrow();
		assertTrue(updated.getStatus());
	}

	@Test
	void testAutomaticStatusUpdateWhenNextAdministrationPassed() {
		VaccineEntity expired = factory.manufacturePojo(VaccineEntity.class);
		expired.setAdministrationDate(pastDate(60));
		expired.setNextAdministration(pastDate(1));
		expired.setStatus(true);
		expired.setVaccinationRecord(vaccinationRecord);
		entityManager.persist(expired);

		List<VaccineEntity> list = vaccineService.getVaccines();
		VaccineEntity updated = list.stream().filter(v -> v.getId().equals(expired.getId())).findFirst().orElseThrow();
		assertFalse(updated.getStatus());
	}

	@Test
	void testGetVaccinesFilteredByRecordAndPet() {
		List<VaccineEntity> list = vaccineService.getVaccines(vaccinationRecord.getId(), pet.getId(), null);
		assertEquals(vaccineList.size(), list.size());

		List<VaccineEntity> emptyList = vaccineService.getVaccines(1000L, null, null);
		assertTrue(emptyList.isEmpty());
	}

	@Test
	void testGetVaccinesFilteredByPetOnly() {
		List<VaccineEntity> list = vaccineService.getVaccines(null, pet.getId(), null);
		assertEquals(vaccineList.size(), list.size());
	}

	@Test
	void testGetVaccinesFilteredByPetMismatch() {
		List<VaccineEntity> list = vaccineService.getVaccines(null, 1000L, null);
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetVaccinesFilteredByStatusTrue() {
		List<VaccineEntity> list = vaccineService.getVaccines(null, null, true);
		assertEquals(vaccineList.size(), list.size());
	}

	@Test
	void testGetVaccinesFilteredByStatusReflectsRefresh() {
		VaccineEntity expired = factory.manufacturePojo(VaccineEntity.class);
		expired.setAdministrationDate(pastDate(60));
		expired.setNextAdministration(pastDate(1));
		expired.setStatus(true);
		expired.setVaccinationRecord(vaccinationRecord);
		entityManager.persist(expired);

		List<VaccineEntity> falseList = vaccineService.getVaccines(null, null, false);
		assertTrue(falseList.stream().anyMatch(v -> v.getId().equals(expired.getId())));
	}

	// Test for getVaccine

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
	void testGetVaccineNegativeId() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.getVaccine(-1L);
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
		VaccineEntity result = vaccineService.getVaccine(null, entity.getName(), vaccinationRecord.getId());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testGetVaccineFilteredByRecordOnly() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity result = vaccineService.getVaccine(null, null, vaccinationRecord.getId());
		assertNotNull(result);
	}

	@Test
	void testGetVaccineCombinedInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			vaccineService.getVaccine(0L, entity.getName(), vaccinationRecord.getId());
		});
	}

	@Test
	void testGetVaccineCombinedNameMismatch() {
		assertThrows(EntityNotFoundException.class, () -> {
			vaccineService.getVaccine(null, "unmatched-name-xyz", vaccinationRecord.getId());
		});
	}

	@Test
	void testGetVaccineCombinedRecordMismatch() {
		assertThrows(EntityNotFoundException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			vaccineService.getVaccine(null, entity.getName(), 1000L);
		});
	}

	// Test for updateVaccine

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
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
			pojoEntity.setStatus(true);
			vaccineService.updateVaccine(0L, pojoEntity);
		});
	}

	@Test
	void testUpdateNonExistentVaccine() {
		assertThrows(EntityNotFoundException.class, () -> {
			VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
			pojoEntity.setStatus(true);
			vaccineService.updateVaccine(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateVaccineWithNullName() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setStatus(true);
			pojoEntity.setAdministrationDate(entity.getAdministrationDate());
			pojoEntity.setName(null);
			vaccineService.updateVaccine(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateVaccineWithBlankName() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setStatus(true);
			pojoEntity.setAdministrationDate(entity.getAdministrationDate());
			pojoEntity.setName("   ");
			vaccineService.updateVaccine(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateVaccineWithNullStatus() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setAdministrationDate(entity.getAdministrationDate());
			pojoEntity.setStatus(null);
			vaccineService.updateVaccine(entity.getId(), pojoEntity);
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
	void testUpdateVaccineWithInvalidNextAdministration() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setStatus(true);
			pojoEntity.setAdministrationDate(entity.getAdministrationDate());
			// earlier than entity's administrationDate (pastDate(30))
			pojoEntity.setNextAdministration(pastDate(100));
			vaccineService.updateVaccine(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateVaccinePreservesVaccinationRecord() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);

		PetEntity otherPet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(otherPet);
		VaccinationRecordEntity otherRecord = factory.manufacturePojo(VaccinationRecordEntity.class);
		otherRecord.setPet(otherPet);
		entityManager.persist(otherRecord);

		VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setStatus(true);
		pojoEntity.setAdministrationDate(entity.getAdministrationDate());
		pojoEntity.setVaccinationRecord(otherRecord);

		vaccineService.updateVaccine(entity.getId(), pojoEntity);

		VaccineEntity resp = entityManager.find(VaccineEntity.class, entity.getId());
		assertEquals(vaccinationRecord.getId(), resp.getVaccinationRecord().getId());
	}

	@Test
	void testUpdateVaccineWithNullAdministrationDateKeepsCurrent()
			throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setStatus(true);
		pojoEntity.setAdministrationDate(null);
		pojoEntity.setNextAdministration(futureDate(200));

		vaccineService.updateVaccine(entity.getId(), pojoEntity);

		VaccineEntity resp = entityManager.find(VaccineEntity.class, entity.getId());
		assertEquals(entity.getAdministrationDate(), resp.getAdministrationDate());
	}

	@Test
	void testUpdateVaccineRefreshesStatusWhenNextAdministrationAlreadyPassed()
			throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0); // administrationDate = pastDate(30)
		VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setStatus(true);
		pojoEntity.setAdministrationDate(entity.getAdministrationDate());
		// after administrationDate (30 days ago), but already passed relative to "now"
		pojoEntity.setNextAdministration(pastDate(1));

		VaccineEntity result = vaccineService.updateVaccine(entity.getId(), pojoEntity);

		assertFalse(result.getStatus());
	}

	// Test for deleteVaccine

	@Test
	void testDeleteVaccine() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		vaccineService.deleteVaccine(entity.getId());
		VaccineEntity deleted = entityManager.find(VaccineEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteVaccineInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.deleteVaccine(0L);
		});
	}

	@Test
	void testDeleteNonExistentVaccine() {
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

	@Test
	void testDeleteVaccineOfPetWithNonFinalizedAdoption() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
		adoption.setPet(pet);
		adoption.setStatus("IN_PROGRESS");
		entityManager.persist(adoption);

		vaccineService.deleteVaccine(entity.getId());

		VaccineEntity deleted = entityManager.find(VaccineEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteVaccineWithoutVaccinationRecord() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity orphan = factory.manufacturePojo(VaccineEntity.class);
		orphan.setAdministrationDate(pastDate(5));
		orphan.setStatus(true);
		orphan.setVaccinationRecord(null);
		entityManager.persist(orphan);

		vaccineService.deleteVaccine(orphan.getId());

		VaccineEntity deleted = entityManager.find(VaccineEntity.class, orphan.getId());
		assertNull(deleted);
	}
}
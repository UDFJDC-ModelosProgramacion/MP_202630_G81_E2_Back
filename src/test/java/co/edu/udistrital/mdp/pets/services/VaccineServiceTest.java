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
	private VaccinationRecordEntity otherRecord = new VaccinationRecordEntity();
	private PetEntity pet = new PetEntity();
	private PetEntity otherPet = new PetEntity();

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

		otherPet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(otherPet);

		otherRecord = factory.manufacturePojo(VaccinationRecordEntity.class);
		otherRecord.setPet(otherPet);
		entityManager.persist(otherRecord);

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
		return vaccine;
	}

	private VaccineEntity newUpdateVaccine(VaccineEntity current) {
		VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
		pojoEntity.setId(current.getId());
		pojoEntity.setStatus(true);
		pojoEntity.setAdministrationDate(current.getAdministrationDate());
		pojoEntity.setNextAdministration(futureDate(200));
		return pojoEntity;
	}

	private VaccineEntity persistExpiredVaccine() {
		VaccineEntity expired = factory.manufacturePojo(VaccineEntity.class);
		expired.setAdministrationDate(pastDate(60));
		expired.setNextAdministration(pastDate(1));
		expired.setStatus(true);
		expired.setVaccinationRecord(vaccinationRecord);
		entityManager.persist(expired);
		return expired;
	}

	private void persistAdoption(PetEntity adoptedPet, String status) {
		AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
		adoption.setPet(adoptedPet);
		adoption.setStatus(status);
		entityManager.persist(adoption);
	}

	// Test for createVaccine

	@Test
	void testCreateVaccine() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity newEntity = newValidVaccine();

		VaccineEntity result = vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);

		assertNotNull(result);
		VaccineEntity entity = entityManager.find(VaccineEntity.class, result.getId());
		assertEquals(newEntity.getName(), entity.getName());
		assertEquals(vaccinationRecord.getId(), entity.getVaccinationRecord().getId());
	}

	@Test
	void testCreateVaccineWithoutNextAdministration() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity newEntity = newValidVaccine();
		newEntity.setNextAdministration(null);

		VaccineEntity result = vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);

		assertNotNull(result);
	}

	@Test
	void testCreateVaccineIgnoresRecordSetInEntity() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity newEntity = newValidVaccine();
		newEntity.setVaccinationRecord(otherRecord);

		VaccineEntity result = vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);

		VaccineEntity entity = entityManager.find(VaccineEntity.class, result.getId());
		assertEquals(vaccinationRecord.getId(), entity.getVaccinationRecord().getId());
	}

	@Test
	void testCreateVaccineWithNullRecordId() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.createVaccine(null, newValidVaccine());
		});
	}

	@Test
	void testCreateVaccineWithInvalidRecordId() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.createVaccine(0L, newValidVaccine());
		});
	}

	@Test
	void testCreateVaccineWithNonExistentRecord() {
		assertThrows(EntityNotFoundException.class, () -> {
			vaccineService.createVaccine(1000L, newValidVaccine());
		});
	}

	@Test
	void testCreateVaccineWithNullName() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setName(null);
			vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);
		});
	}

	@Test
	void testCreateVaccineWithBlankName() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setName("   ");
			vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);
		});
	}

	@Test
	void testCreateVaccineWithNullAdministrationDate() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setAdministrationDate(null);
			vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);
		});
	}

	@Test
	void testCreateVaccineWithNullStatus() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setStatus(null);
			vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);
		});
	}

	@Test
	void testCreateVaccineWithFutureAdministrationDate() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setAdministrationDate(futureDate(5));
			vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);
		});
	}

	@Test
	void testCreateVaccineWithNextAdministrationBeforeAdministrationDate() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setNextAdministration(pastDate(10));
			vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);
		});
	}

	@Test
	void testCreateVaccineWithNextAdministrationEqualToAdministrationDate() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setNextAdministration(newEntity.getAdministrationDate());
			vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);
		});
	}

	@Test
	void testCreateDuplicatedVaccine() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity existing = vaccineList.get(0);
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setName(existing.getName());
			newEntity.setAdministrationDate(existing.getAdministrationDate());
			vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);
		});
	}

	@Test
	void testCreateDuplicatedVaccineIgnoringCase() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity existing = vaccineList.get(0);
			VaccineEntity newEntity = newValidVaccine();
			newEntity.setName(existing.getName().toUpperCase());
			newEntity.setAdministrationDate(existing.getAdministrationDate());
			vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);
		});
	}

	@Test
	void testCreateVaccineSameNameDifferentDateIsNotDuplicate()
			throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity existing = vaccineList.get(0);
		VaccineEntity newEntity = newValidVaccine();
		newEntity.setName(existing.getName());
		newEntity.setAdministrationDate(pastDate(5));

		VaccineEntity result = vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);

		assertNotNull(result);
	}

	@Test
	void testCreateVaccineSameDateDifferentNameIsNotDuplicate()
			throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity existing = vaccineList.get(0);
		VaccineEntity newEntity = newValidVaccine();
		newEntity.setAdministrationDate(existing.getAdministrationDate());

		VaccineEntity result = vaccineService.createVaccine(vaccinationRecord.getId(), newEntity);

		assertNotNull(result);
	}

	@Test
	void testCreateVaccineSameNameAndDateDifferentRecordIsNotDuplicate()
			throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity existing = vaccineList.get(0);
		VaccineEntity newEntity = newValidVaccine();
		newEntity.setName(existing.getName());
		newEntity.setAdministrationDate(existing.getAdministrationDate());

		VaccineEntity result = vaccineService.createVaccine(otherRecord.getId(), newEntity);

		assertNotNull(result);
	}

	// Test for getVaccines

	@Test
	void testGetVaccines() throws EntityNotFoundException, IllegalOperationException {
		List<VaccineEntity> list = vaccineService.getVaccines(vaccinationRecord.getId());
		assertEquals(vaccineList.size(), list.size());
	}

	@Test
	void testGetVaccinesEmpty() throws EntityNotFoundException, IllegalOperationException {
		List<VaccineEntity> list = vaccineService.getVaccines(otherRecord.getId());
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetVaccinesOnlyFromRequestedRecord() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity other = newValidVaccine();
		other.setVaccinationRecord(otherRecord);
		entityManager.persist(other);

		List<VaccineEntity> list = vaccineService.getVaccines(vaccinationRecord.getId());

		assertEquals(vaccineList.size(), list.size());
		assertTrue(list.stream().noneMatch(v -> v.getId().equals(other.getId())));
	}

	@Test
	void testGetVaccinesWithInvalidRecordId() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.getVaccines(0L);
		});
	}

	@Test
	void testGetVaccinesWithNonExistentRecord() {
		assertThrows(EntityNotFoundException.class, () -> {
			vaccineService.getVaccines(1000L);
		});
	}

	@Test
	void testGetVaccinesDoesNotDowngradeValidStatus() throws EntityNotFoundException, IllegalOperationException {
		List<VaccineEntity> list = vaccineService.getVaccines(vaccinationRecord.getId());
		VaccineEntity entity = list.stream().filter(v -> v.getId().equals(vaccineList.get(0).getId())).findFirst()
				.orElseThrow();
		assertTrue(entity.getStatus());
	}

	@Test
	void testGetVaccinesDoesNotDowngradeWhenNextAdministrationNull()
			throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity noNext = factory.manufacturePojo(VaccineEntity.class);
		noNext.setAdministrationDate(pastDate(10));
		noNext.setNextAdministration(null);
		noNext.setStatus(true);
		noNext.setVaccinationRecord(vaccinationRecord);
		entityManager.persist(noNext);

		List<VaccineEntity> list = vaccineService.getVaccines(vaccinationRecord.getId());
		VaccineEntity updated = list.stream().filter(v -> v.getId().equals(noNext.getId())).findFirst().orElseThrow();
		assertTrue(updated.getStatus());
	}

	@Test
	void testGetVaccinesKeepsFalseStatusWhenNextAdministrationPassed()
			throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity inactive = factory.manufacturePojo(VaccineEntity.class);
		inactive.setAdministrationDate(pastDate(60));
		inactive.setNextAdministration(pastDate(1));
		inactive.setStatus(false);
		inactive.setVaccinationRecord(vaccinationRecord);
		entityManager.persist(inactive);

		List<VaccineEntity> list = vaccineService.getVaccines(vaccinationRecord.getId());
		VaccineEntity updated = list.stream().filter(v -> v.getId().equals(inactive.getId())).findFirst()
				.orElseThrow();
		assertFalse(updated.getStatus());
	}

	@Test
	void testAutomaticStatusUpdateWhenNextAdministrationPassed()
			throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity expired = persistExpiredVaccine();

		List<VaccineEntity> list = vaccineService.getVaccines(vaccinationRecord.getId());
		VaccineEntity updated = list.stream().filter(v -> v.getId().equals(expired.getId())).findFirst().orElseThrow();
		assertFalse(updated.getStatus());
	}

	// Test for getVaccine

	@Test
	void testGetVaccine() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		VaccineEntity result = vaccineService.getVaccine(vaccinationRecord.getId(), entity.getId());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testGetVaccineWithInvalidRecordId() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.getVaccine(0L, vaccineList.get(0).getId());
		});
	}

	@Test
	void testGetVaccineWithNonExistentRecord() {
		assertThrows(EntityNotFoundException.class, () -> {
			vaccineService.getVaccine(1000L, vaccineList.get(0).getId());
		});
	}

	@Test
	void testGetVaccineWithNullId() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.getVaccine(vaccinationRecord.getId(), null);
		});
	}

	@Test
	void testGetVaccineWithInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.getVaccine(vaccinationRecord.getId(), 0L);
		});
	}

	@Test
	void testGetVaccineWithNegativeId() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.getVaccine(vaccinationRecord.getId(), -1L);
		});
	}

	@Test
	void testGetNonExistentVaccine() {
		assertThrows(EntityNotFoundException.class, () -> {
			vaccineService.getVaccine(vaccinationRecord.getId(), 1000L);
		});
	}

	@Test
	void testGetVaccineNotBelongingToRecord() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.getVaccine(otherRecord.getId(), vaccineList.get(0).getId());
		});
	}

	@Test
	void testGetVaccineWithoutVaccinationRecord() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity orphan = factory.manufacturePojo(VaccineEntity.class);
			orphan.setAdministrationDate(pastDate(5));
			orphan.setStatus(true);
			orphan.setVaccinationRecord(null);
			entityManager.persist(orphan);
			vaccineService.getVaccine(vaccinationRecord.getId(), orphan.getId());
		});
	}

	@Test
	void testGetVaccineRefreshesExpiredStatus() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity expired = persistExpiredVaccine();

		VaccineEntity result = vaccineService.getVaccine(vaccinationRecord.getId(), expired.getId());

		assertFalse(result.getStatus());
	}

	// Test for updateVaccine

	@Test
	void testUpdateVaccine() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		VaccineEntity pojoEntity = newUpdateVaccine(entity);

		vaccineService.updateVaccine(vaccinationRecord.getId(), entity.getId(), pojoEntity);

		VaccineEntity resp = entityManager.find(VaccineEntity.class, entity.getId());
		assertEquals(pojoEntity.getName(), resp.getName());
		assertEquals(entity.getAdministrationDate(), resp.getAdministrationDate());
	}

	@Test
	void testUpdateVaccineWithoutNextAdministration() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		VaccineEntity pojoEntity = newUpdateVaccine(entity);
		pojoEntity.setNextAdministration(null);

		VaccineEntity result = vaccineService.updateVaccine(vaccinationRecord.getId(), entity.getId(), pojoEntity);

		assertNotNull(result);
		assertNull(result.getNextAdministration());
	}

	@Test
	void testUpdateVaccineWithInvalidRecordId() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			vaccineService.updateVaccine(0L, entity.getId(), newUpdateVaccine(entity));
		});
	}

	@Test
	void testUpdateVaccineWithNonExistentRecord() {
		assertThrows(EntityNotFoundException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			vaccineService.updateVaccine(1000L, entity.getId(), newUpdateVaccine(entity));
		});
	}

	@Test
	void testUpdateVaccineInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
			pojoEntity.setStatus(true);
			vaccineService.updateVaccine(vaccinationRecord.getId(), 0L, pojoEntity);
		});
	}

	@Test
	void testUpdateNonExistentVaccine() {
		assertThrows(EntityNotFoundException.class, () -> {
			VaccineEntity pojoEntity = factory.manufacturePojo(VaccineEntity.class);
			pojoEntity.setStatus(true);
			vaccineService.updateVaccine(vaccinationRecord.getId(), 1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateVaccineNotBelongingToRecord() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			vaccineService.updateVaccine(otherRecord.getId(), entity.getId(), newUpdateVaccine(entity));
		});
	}

	@Test
	void testUpdateVaccineWithNullName() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			VaccineEntity pojoEntity = newUpdateVaccine(entity);
			pojoEntity.setName(null);
			vaccineService.updateVaccine(vaccinationRecord.getId(), entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateVaccineWithBlankName() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			VaccineEntity pojoEntity = newUpdateVaccine(entity);
			pojoEntity.setName("   ");
			vaccineService.updateVaccine(vaccinationRecord.getId(), entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateVaccineWithNullStatus() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			VaccineEntity pojoEntity = newUpdateVaccine(entity);
			pojoEntity.setStatus(null);
			vaccineService.updateVaccine(vaccinationRecord.getId(), entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateVaccineChangingAdministrationDate() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			VaccineEntity pojoEntity = newUpdateVaccine(entity);
			pojoEntity.setAdministrationDate(pastDate(1));
			vaccineService.updateVaccine(vaccinationRecord.getId(), entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateVaccineWithNullAdministrationDateKeepsCurrent()
			throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		VaccineEntity pojoEntity = newUpdateVaccine(entity);
		pojoEntity.setAdministrationDate(null);

		vaccineService.updateVaccine(vaccinationRecord.getId(), entity.getId(), pojoEntity);

		VaccineEntity resp = entityManager.find(VaccineEntity.class, entity.getId());
		assertEquals(entity.getAdministrationDate(), resp.getAdministrationDate());
	}

	@Test
	void testUpdateVaccineWithNextAdministrationBeforeAdministrationDate() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			VaccineEntity pojoEntity = newUpdateVaccine(entity);
			pojoEntity.setNextAdministration(pastDate(100));
			vaccineService.updateVaccine(vaccinationRecord.getId(), entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateVaccineWithNextAdministrationEqualToAdministrationDate() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccineEntity entity = vaccineList.get(0);
			VaccineEntity pojoEntity = newUpdateVaccine(entity);
			pojoEntity.setNextAdministration(entity.getAdministrationDate());
			vaccineService.updateVaccine(vaccinationRecord.getId(), entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdateVaccinePreservesVaccinationRecord() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		VaccineEntity pojoEntity = newUpdateVaccine(entity);
		pojoEntity.setVaccinationRecord(otherRecord);

		vaccineService.updateVaccine(vaccinationRecord.getId(), entity.getId(), pojoEntity);

		VaccineEntity resp = entityManager.find(VaccineEntity.class, entity.getId());
		assertEquals(vaccinationRecord.getId(), resp.getVaccinationRecord().getId());
	}

	@Test
	void testUpdateVaccineRefreshesStatusWhenNextAdministrationAlreadyPassed()
			throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		VaccineEntity pojoEntity = newUpdateVaccine(entity);
		pojoEntity.setNextAdministration(pastDate(1)); // despues de la administracion (hace 30 dias) pero ya vencida

		VaccineEntity result = vaccineService.updateVaccine(vaccinationRecord.getId(), entity.getId(), pojoEntity);

		assertFalse(result.getStatus());
	}

	// Test for deleteVaccine

	@Test
	void testDeleteVaccine() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		vaccineService.deleteVaccine(vaccinationRecord.getId(), entity.getId());
		VaccineEntity deleted = entityManager.find(VaccineEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteVaccineWithInvalidRecordId() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.deleteVaccine(0L, vaccineList.get(0).getId());
		});
	}

	@Test
	void testDeleteVaccineWithNonExistentRecord() {
		assertThrows(EntityNotFoundException.class, () -> {
			vaccineService.deleteVaccine(1000L, vaccineList.get(0).getId());
		});
	}

	@Test
	void testDeleteVaccineInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.deleteVaccine(vaccinationRecord.getId(), 0L);
		});
	}

	@Test
	void testDeleteNonExistentVaccine() {
		assertThrows(EntityNotFoundException.class, () -> {
			vaccineService.deleteVaccine(vaccinationRecord.getId(), 1000L);
		});
	}

	@Test
	void testDeleteVaccineNotBelongingToRecord() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccineService.deleteVaccine(otherRecord.getId(), vaccineList.get(0).getId());
		});
	}

	@Test
	void testDeleteVaccineOfAdoptedPet() {
		assertThrows(IllegalOperationException.class, () -> {
			persistAdoption(pet, "FINALIZED");
			vaccineService.deleteVaccine(vaccinationRecord.getId(), vaccineList.get(0).getId());
		});
	}

	@Test
	void testDeleteVaccineOfAdoptedPetIgnoringCase() {
		assertThrows(IllegalOperationException.class, () -> {
			persistAdoption(pet, "finalized");
			vaccineService.deleteVaccine(vaccinationRecord.getId(), vaccineList.get(0).getId());
		});
	}

	@Test
	void testDeleteVaccineOfPetWithNonFinalizedAdoption() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		persistAdoption(pet, "IN_PROGRESS");

		vaccineService.deleteVaccine(vaccinationRecord.getId(), entity.getId());

		assertNull(entityManager.find(VaccineEntity.class, entity.getId()));
	}

	@Test
	void testDeleteVaccineWhenOnlyAnotherPetWasAdopted() throws EntityNotFoundException, IllegalOperationException {
		VaccineEntity entity = vaccineList.get(0);
		persistAdoption(otherPet, "FINALIZED");

		vaccineService.deleteVaccine(vaccinationRecord.getId(), entity.getId());

		assertNull(entityManager.find(VaccineEntity.class, entity.getId()));
	}

	@Test
	void testDeleteVaccineOfRecordWithoutPet() throws EntityNotFoundException, IllegalOperationException {
		VaccinationRecordEntity recordNoPet = factory.manufacturePojo(VaccinationRecordEntity.class);
		recordNoPet.setPet(null);
		entityManager.persist(recordNoPet);

		VaccineEntity vaccine = newValidVaccine();
		vaccine.setVaccinationRecord(recordNoPet);
		entityManager.persist(vaccine);

		vaccineService.deleteVaccine(recordNoPet.getId(), vaccine.getId());

		assertNull(entityManager.find(VaccineEntity.class, vaccine.getId()));
	}
}
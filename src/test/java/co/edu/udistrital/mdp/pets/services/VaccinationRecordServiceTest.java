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
@Import(VaccinationRecordService.class)
class VaccinationRecordServiceTest {

	@Autowired
	private VaccinationRecordService vaccinationRecordService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<VaccinationRecordEntity> recordList = new ArrayList<>();
	private List<PetEntity> petList = new ArrayList<>();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from VaccineEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from VaccinationRecordEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdopterEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
	}

	private void insertData() {
		for (int i = 0; i < 3; i++) {
			PetEntity petEntity = factory.manufacturePojo(PetEntity.class);
			entityManager.persist(petEntity);
			petList.add(petEntity);

			VaccinationRecordEntity vaccinationRecord = factory.manufacturePojo(VaccinationRecordEntity.class);
			vaccinationRecord.setPet(petEntity);
			entityManager.persist(vaccinationRecord);
			recordList.add(vaccinationRecord);
		}
	}

	@Test
	void testCreateVaccinationRecord() throws EntityNotFoundException, IllegalOperationException {
		PetEntity newPet = factory.manufacturePojo(PetEntity.class);
		entityManager.persist(newPet);

		VaccinationRecordEntity newEntity = factory.manufacturePojo(VaccinationRecordEntity.class);
		newEntity.setPet(newPet);

		VaccinationRecordEntity result = vaccinationRecordService.createVaccinationRecord(newEntity);

		assertNotNull(result);
		VaccinationRecordEntity entity = entityManager.find(VaccinationRecordEntity.class, result.getId());
		assertEquals(newPet.getId(), entity.getPet().getId());
	}

	@Test
	void testCreateVaccinationRecordWithNullPet() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccinationRecordEntity newEntity = factory.manufacturePojo(VaccinationRecordEntity.class);
			newEntity.setPet(null);
			vaccinationRecordService.createVaccinationRecord(newEntity);
		});
	}

	@Test
	void testCreateVaccinationRecordWithNonExistentPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			VaccinationRecordEntity newEntity = factory.manufacturePojo(VaccinationRecordEntity.class);
			PetEntity fakePet = new PetEntity();
			fakePet.setId(0L);
			newEntity.setPet(fakePet);
			vaccinationRecordService.createVaccinationRecord(newEntity);
		});
	}

	@Test
	void testCreateVaccinationRecordDuplicatedForPet() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccinationRecordEntity newEntity = factory.manufacturePojo(VaccinationRecordEntity.class);
			newEntity.setPet(petList.get(0));
			vaccinationRecordService.createVaccinationRecord(newEntity);
		});
	}

	@Test
	void testGetVaccinationRecords() {
		List<VaccinationRecordEntity> list = vaccinationRecordService.getVaccinationRecords();
		assertEquals(recordList.size(), list.size());
	}

	@Test
	void testGetVaccinationRecordsEmpty() {
		entityManager.getEntityManager().createQuery("delete from VaccineEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from VaccinationRecordEntity").executeUpdate();
		List<VaccinationRecordEntity> list = vaccinationRecordService.getVaccinationRecords();
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetVaccinationRecordByPet() throws EntityNotFoundException {
		VaccinationRecordEntity result = vaccinationRecordService.getVaccinationRecordByPet(petList.get(0).getId());
		assertNotNull(result);
		assertEquals(recordList.get(0).getId(), result.getId());
	}

	@Test
	void testGetVaccinationRecordByNonExistentPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			vaccinationRecordService.getVaccinationRecordByPet(1000L);
		});
	}

	@Test
	void testGetVaccinationRecord() throws EntityNotFoundException, IllegalOperationException {
		VaccinationRecordEntity entity = recordList.get(0);
		VaccinationRecordEntity resultEntity = vaccinationRecordService.getVaccinationRecord(entity.getId());
		assertNotNull(resultEntity);
		assertEquals(entity.getId(), resultEntity.getId());
	}

	@Test
	void testGetInvalidVaccinationRecordId() {
		assertThrows(IllegalOperationException.class, () -> {
			vaccinationRecordService.getVaccinationRecord(0L);
		});
	}

	@Test
	void testGetNonExistentVaccinationRecord() {
		assertThrows(EntityNotFoundException.class, () -> {
			vaccinationRecordService.getVaccinationRecord(1000L);
		});
	}

	@Test
	void testUpdateVaccinationRecord() throws EntityNotFoundException, IllegalOperationException {
		VaccinationRecordEntity entity = recordList.get(0);
		VaccinationRecordEntity pojoEntity = factory.manufacturePojo(VaccinationRecordEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setPet(entity.getPet());

		vaccinationRecordService.updateVaccinationRecord(entity.getId(), pojoEntity);

		VaccinationRecordEntity resp = entityManager.find(VaccinationRecordEntity.class, entity.getId());
		assertEquals(entity.getPet().getId(), resp.getPet().getId());
	}

	@Test
	void testUpdateVaccinationRecordInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			VaccinationRecordEntity pojoEntity = factory.manufacturePojo(VaccinationRecordEntity.class);
			pojoEntity.setId(1000L);
			vaccinationRecordService.updateVaccinationRecord(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdateVaccinationRecordReassignPet() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccinationRecordEntity entity = recordList.get(0);
			VaccinationRecordEntity pojoEntity = factory.manufacturePojo(VaccinationRecordEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setPet(petList.get(1));
			vaccinationRecordService.updateVaccinationRecord(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testAddVaccine() throws EntityNotFoundException, IllegalOperationException {
		VaccinationRecordEntity entity = recordList.get(0);
		VaccineEntity vaccine = factory.manufacturePojo(VaccineEntity.class);

		VaccinationRecordEntity result = vaccinationRecordService.addVaccine(entity.getId(), vaccine);

		assertNotNull(result);
		assertTrue(result.getVaccines().stream().anyMatch(v -> v.getName().equals(vaccine.getName())));
	}

	@Test
	void testAddNullVaccine() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccinationRecordEntity entity = recordList.get(0);
			vaccinationRecordService.addVaccine(entity.getId(), null);
		});
	}

	@Test
	void testAddVaccineToNonExistentRecord() {
		assertThrows(EntityNotFoundException.class, () -> {
			VaccineEntity vaccine = factory.manufacturePojo(VaccineEntity.class);
			vaccinationRecordService.addVaccine(1000L, vaccine);
		});
	}

	@Test
	void testDeleteVaccinationRecordOfInactivePet() throws EntityNotFoundException, IllegalOperationException {
		PetEntity pet = petList.get(0);
		AdopterEntity adopter = factory.manufacturePojo(AdopterEntity.class);
		entityManager.persist(adopter);
		AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
		adoption.setPet(pet);
		adoption.setAdopter(adopter);
		adoption.setStatus("FINALIZED");
		entityManager.persist(adoption);
		pet.getAdoptions().add(adoption);

		VaccinationRecordEntity entity = recordList.get(0);
		vaccinationRecordService.deleteVaccinationRecord(entity.getId());

		VaccinationRecordEntity deleted = entityManager.find(VaccinationRecordEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInvalidVaccinationRecord() {
		assertThrows(EntityNotFoundException.class, () -> {
			vaccinationRecordService.deleteVaccinationRecord(1000L);
		});
	}

	@Test
	void testDeleteVaccinationRecordOfActivePet() {
		assertThrows(IllegalOperationException.class, () -> {
			VaccinationRecordEntity entity = recordList.get(0);
			vaccinationRecordService.deleteVaccinationRecord(entity.getId());
		});
	}
}

package co.edu.udistrital.mdp.pets.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import co.edu.udistrital.mdp.pets.entities.MedicalEventEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ReturnEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationRequestEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@DataJpaTest
@Transactional
@Import(PetService.class)
class PetServiceTest {

	@Autowired
	private PetService petService;

	@Autowired
	private TestEntityManager entityManager;

	private PodamFactory factory = new PodamFactoryImpl();

	private List<PetEntity> petList = new ArrayList<>();
	private ShelterEntity shelter = new ShelterEntity();

	@BeforeEach
	void setUp() {
		clearData();
		insertData();
	}

	private void clearData() {
		entityManager.getEntityManager().createQuery("delete from MedicalEventEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from AdoptionEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from ShelterEntity").executeUpdate();
	}

	private void insertData() {
		shelter = factory.manufacturePojo(ShelterEntity.class);
		entityManager.persist(shelter);

		for (int i = 0; i < 3; i++) {
			PetEntity petEntity = factory.manufacturePojo(PetEntity.class);
			petEntity.setAdmissionDate(pastDate(30));
			petEntity.setCompatibilityChildren(true);
			petEntity.setCompatibilityOtherPets(true);
			petEntity.setShelter(shelter);
			entityManager.persist(petEntity);

			MedicalEventEntity arrival = factory.manufacturePojo(MedicalEventEntity.class);
			arrival.setPet(petEntity);
			entityManager.persist(arrival);
			petEntity.getMedicalEvents().add(arrival);

			petList.add(petEntity);
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

	private PetEntity newValidPet() {
		PetEntity pet = factory.manufacturePojo(PetEntity.class);
		pet.setAdmissionDate(pastDate(1));
		pet.setCompatibilityChildren(true);
		pet.setCompatibilityOtherPets(true);
		pet.setShelter(shelter);
		MedicalEventEntity arrival = factory.manufacturePojo(MedicalEventEntity.class);
		List<MedicalEventEntity> events = new ArrayList<>();
		events.add(arrival);
		pet.setMedicalEvents(events);
		return pet;
	}

	// Test for createPet

	@Test
	void testCreatePet() throws EntityNotFoundException, IllegalOperationException {
		PetEntity newEntity = newValidPet();

		PetEntity result = petService.createPet(newEntity);

		assertNotNull(result);
		PetEntity entity = entityManager.find(PetEntity.class, result.getId());
		assertEquals(newEntity.getName(), entity.getName());
		assertEquals(shelter.getId(), entity.getShelter().getId());
		assertEquals(1, entity.getMedicalEvents().size());
	}

	@Test
	void testCreatePetWithMultipleArrivalEvents() throws EntityNotFoundException, IllegalOperationException {
		PetEntity newEntity = newValidPet();
		MedicalEventEntity secondArrival = factory.manufacturePojo(MedicalEventEntity.class);
		newEntity.getMedicalEvents().add(secondArrival);

		PetEntity result = petService.createPet(newEntity);

		PetEntity entity = entityManager.find(PetEntity.class, result.getId());
		assertEquals(2, entity.getMedicalEvents().size());
		assertTrue(entity.getMedicalEvents().stream().allMatch(e -> entity.getId().equals(e.getPet().getId())));
	}

	@Test
	void testCreatePetWithNullName() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setName(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithBlankName() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setName("   ");
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNullSpecies() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setSpecies(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithBlankSpecies() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setSpecies("   ");
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNullBreed() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setBreed(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithBlankBreed() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setBreed("   ");
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNullAge() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setAge(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNullSex() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setSex(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithBlankSex() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setSex("   ");
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNullSize() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setSize(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithBlankSize() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setSize("   ");
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNullHealthStatus() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setHealthStatus(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithBlankHealthStatus() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setHealthStatus("   ");
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNullAdmissionDate() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setAdmissionDate(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNullCompatibilityChildren() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setCompatibilityChildren(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNullCompatibilityOtherPets() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setCompatibilityOtherPets(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNullActivityLevel() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setActivityLevel(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithBlankActivityLevel() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setActivityLevel("   ");
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNullRequiredSpace() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setRequiredSpace(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithBlankRequiredSpace() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setRequiredSpace("   ");
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNullShelter() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setShelter(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithShelterWithoutId() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			ShelterEntity shelterNoId = new ShelterEntity();
			// id intentionally left null
			newEntity.setShelter(shelterNoId);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			PetEntity newEntity = newValidPet();
			ShelterEntity fakeShelter = new ShelterEntity();
			fakeShelter.setId(0L);
			newEntity.setShelter(fakeShelter);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithFutureAdmissionDate() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setAdmissionDate(futureDate(5));
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithoutArrivalMedicalEvent() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setMedicalEvents(new ArrayList<>());
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreatePetWithNullMedicalEventsList() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setMedicalEvents(null);
			petService.createPet(newEntity);
		});
	}

	@Test
	void testCreateDuplicatedPet() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity existing = petList.get(0);
			PetEntity newEntity = newValidPet();
			newEntity.setName(existing.getName());
			newEntity.setSpecies(existing.getSpecies());
			newEntity.setAdmissionDate(existing.getAdmissionDate());
			petService.createPet(newEntity);
		});
	}

	//Test for getPets

	@Test
	void testGetPets() {
		List<PetEntity> list = petService.getPets();
		assertEquals(petList.size(), list.size());
	}

	@Test
	void testGetPetsEmpty() {
		entityManager.getEntityManager().createQuery("delete from MedicalEventEntity").executeUpdate();
		entityManager.getEntityManager().createQuery("delete from PetEntity").executeUpdate();
		List<PetEntity> list = petService.getPets();
		assertTrue(list.isEmpty());
	}

	@Test
	void testGetPetsFilteredBySpecies() {
		PetEntity target = petList.get(0);
		List<PetEntity> list = petService.getPets(target.getSpecies(), null, null, null, null, null, null);
		assertTrue(list.stream().anyMatch(p -> p.getId().equals(target.getId())));

		List<PetEntity> emptyList = petService.getPets("unmatched-species-xyz", null, null, null, null, null, null);
		assertTrue(emptyList.isEmpty());
	}

	@Test
	void testGetPetsFilteredByAge() {
		PetEntity target = petList.get(0);
		List<PetEntity> list = petService.getPets(null, target.getAge(), null, null, null, null, null);
		assertTrue(list.stream().anyMatch(p -> p.getId().equals(target.getId())));

		List<PetEntity> emptyList = petService.getPets(null, -999, null, null, null, null, null);
		assertTrue(emptyList.isEmpty());
	}

	@Test
	void testGetPetsFilteredBySize() {
		PetEntity target = petList.get(0);
		List<PetEntity> list = petService.getPets(null, null, target.getSize(), null, null, null, null);
		assertTrue(list.stream().anyMatch(p -> p.getId().equals(target.getId())));

		List<PetEntity> emptyList = petService.getPets(null, null, "unmatched-size-xyz", null, null, null, null);
		assertTrue(emptyList.isEmpty());
	}

	@Test
	void testGetPetsFilteredByRequiredSpace() {
		PetEntity target = petList.get(0);
		List<PetEntity> list = petService.getPets(null, null, null, target.getRequiredSpace(), null, null, null);
		assertTrue(list.stream().anyMatch(p -> p.getId().equals(target.getId())));

		List<PetEntity> emptyList = petService.getPets(null, null, null, "unmatched-space-xyz", null, null, null);
		assertTrue(emptyList.isEmpty());
	}

	@Test
	void testGetPetsFilteredByCompatibilityChildren() {
		PetEntity target = petList.get(0);
		List<PetEntity> list = petService.getPets(null, null, null, null, true, null, null);
		assertTrue(list.stream().anyMatch(p -> p.getId().equals(target.getId())));

		List<PetEntity> emptyList = petService.getPets(null, null, null, null, false, null, null);
		assertTrue(emptyList.isEmpty());
	}

	@Test
	void testGetPetsFilteredByCompatibilityOtherPets() {
		PetEntity target = petList.get(0);
		List<PetEntity> list = petService.getPets(null, null, null, null, null, true, null);
		assertTrue(list.stream().anyMatch(p -> p.getId().equals(target.getId())));

		List<PetEntity> emptyList = petService.getPets(null, null, null, null, null, false, null);
		assertTrue(emptyList.isEmpty());
	}

	@Test
	void testGetPetsFilteredByActivityLevel() {
		PetEntity target = petList.get(0);
		List<PetEntity> list = petService.getPets(null, null, null, null, null, null, target.getActivityLevel());
		assertTrue(list.stream().anyMatch(p -> p.getId().equals(target.getId())));

		List<PetEntity> emptyList = petService.getPets(null, null, null, null, null, null, "unmatched-activity-xyz");
		assertTrue(emptyList.isEmpty());
	}

	// Test for getPet

	@Test
	void testGetPet() throws EntityNotFoundException, IllegalOperationException {
		PetEntity entity = petList.get(0);
		PetEntity result = petService.getPet(entity.getId());
		assertNotNull(result);
		assertEquals(entity.getId(), result.getId());
	}

	@Test
	void testGetInvalidPetId() {
		assertThrows(IllegalOperationException.class, () -> {
			petService.getPet(0L);
		});
	}

	@Test
	void testGetPetNegativeId() {
		assertThrows(IllegalOperationException.class, () -> {
			petService.getPet(-1L);
		});
	}

	@Test
	void testGetNonExistentPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			petService.getPet(1000L);
		});
	}

	// Test for updatePet
	@Test
	void testUpdatePet() throws EntityNotFoundException, IllegalOperationException {
		PetEntity entity = petList.get(0);
		PetEntity pojoEntity = factory.manufacturePojo(PetEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setCompatibilityChildren(true);
		pojoEntity.setCompatibilityOtherPets(true);
		pojoEntity.setAdmissionDate(entity.getAdmissionDate());

		petService.updatePet(entity.getId(), pojoEntity);

		PetEntity resp = entityManager.find(PetEntity.class, entity.getId());
		assertEquals(pojoEntity.getName(), resp.getName());
		assertEquals(entity.getShelter().getId(), resp.getShelter().getId());
	}

	@Test
	void testUpdatePetPreservesAssociations() throws EntityNotFoundException, IllegalOperationException {
		PetEntity entity = petList.get(0);
		PetEntity pojoEntity = factory.manufacturePojo(PetEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setCompatibilityChildren(true);
		pojoEntity.setCompatibilityOtherPets(true);
		pojoEntity.setAdmissionDate(entity.getAdmissionDate());

		petService.updatePet(entity.getId(), pojoEntity);

		PetEntity resp = entityManager.find(PetEntity.class, entity.getId());
		assertEquals(1, resp.getMedicalEvents().size());
	}

	@Test
	void testUpdatePetInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity pojoEntity = factory.manufacturePojo(PetEntity.class);
			pojoEntity.setCompatibilityChildren(true);
			pojoEntity.setCompatibilityOtherPets(true);
			petService.updatePet(0L, pojoEntity);
		});
	}

	@Test
	void testUpdateNonExistentPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			PetEntity pojoEntity = factory.manufacturePojo(PetEntity.class);
			pojoEntity.setCompatibilityChildren(true);
			pojoEntity.setCompatibilityOtherPets(true);
			petService.updatePet(1000L, pojoEntity);
		});
	}

	@Test
	void testUpdatePetWithNullName() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity entity = petList.get(0);
			PetEntity pojoEntity = factory.manufacturePojo(PetEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setCompatibilityChildren(true);
			pojoEntity.setCompatibilityOtherPets(true);
			pojoEntity.setAdmissionDate(entity.getAdmissionDate());
			pojoEntity.setName(null);
			petService.updatePet(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdatePetChangingAdmissionDate() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity entity = petList.get(0);
			PetEntity pojoEntity = factory.manufacturePojo(PetEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setCompatibilityChildren(true);
			pojoEntity.setCompatibilityOtherPets(true);
			pojoEntity.setAdmissionDate(futureDate(1));
			petService.updatePet(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdatePetWithNonExistentShelter() {
		assertThrows(EntityNotFoundException.class, () -> {
			PetEntity entity = petList.get(0);
			PetEntity pojoEntity = factory.manufacturePojo(PetEntity.class);
			pojoEntity.setId(entity.getId());
			pojoEntity.setCompatibilityChildren(true);
			pojoEntity.setCompatibilityOtherPets(true);
			pojoEntity.setAdmissionDate(entity.getAdmissionDate());
			ShelterEntity fakeShelter = new ShelterEntity();
			fakeShelter.setId(0L);
			pojoEntity.setShelter(fakeShelter);
			petService.updatePet(entity.getId(), pojoEntity);
		});
	}

	@Test
	void testUpdatePetChangingShelterValid() throws EntityNotFoundException, IllegalOperationException {
		PetEntity entity = petList.get(0);
		ShelterEntity newShelter = factory.manufacturePojo(ShelterEntity.class);
		entityManager.persist(newShelter);

		PetEntity pojoEntity = factory.manufacturePojo(PetEntity.class);
		pojoEntity.setId(entity.getId());
		pojoEntity.setCompatibilityChildren(true);
		pojoEntity.setCompatibilityOtherPets(true);
		pojoEntity.setAdmissionDate(entity.getAdmissionDate());
		pojoEntity.setShelter(newShelter);

		petService.updatePet(entity.getId(), pojoEntity);

		PetEntity resp = entityManager.find(PetEntity.class, entity.getId());
		assertEquals(newShelter.getId(), resp.getShelter().getId());
	}

	@Test
	void testUpdatePetDuplicated() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity target = petList.get(1);
			PetEntity other = petList.get(0);

			PetEntity pojoEntity = factory.manufacturePojo(PetEntity.class);
			pojoEntity.setId(target.getId());
			pojoEntity.setCompatibilityChildren(true);
			pojoEntity.setCompatibilityOtherPets(true);
			pojoEntity.setAdmissionDate(target.getAdmissionDate());
			pojoEntity.setName(other.getName());
			pojoEntity.setSpecies(other.getSpecies());
			pojoEntity.setAdmissionDate(other.getAdmissionDate());

			petService.updatePet(target.getId(), pojoEntity);
		});
	}

	// Test for deletePet

	@Test
	void testDeletePet() throws EntityNotFoundException, IllegalOperationException {
		PetEntity entity = petList.get(0);
		petService.deletePet(entity.getId());
		PetEntity deleted = entityManager.find(PetEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeletePetInvalidId() {
		assertThrows(IllegalOperationException.class, () -> {
			petService.deletePet(0L);
		});
	}

	@Test
	void testDeleteNonExistentPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			petService.deletePet(1000L);
		});
	}

	@Test
	void testDeletePetWithActiveAdoption() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity entity = petList.get(0);
			AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
			adoption.setPet(entity);
			adoption.setShelter(shelter);
			adoption.setReturnAfterAdoption(null);
			entityManager.persist(adoption);
			petService.deletePet(entity.getId());
		});
	}

	@Test
	void testDeletePetWithCompletedAdoptionHistory() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity entity = petList.get(0);
			AdoptionEntity adoption = factory.manufacturePojo(AdoptionEntity.class);
			adoption.setPet(entity);
			adoption.setShelter(shelter);
			entityManager.persist(adoption);

			ReturnEntity returnRecord = factory.manufacturePojo(ReturnEntity.class);
			returnRecord.setAdoption(adoption);
			returnRecord.setShelter(shelter);
			entityManager.persist(returnRecord);

			adoption.setReturnAfterAdoption(returnRecord);

			petService.deletePet(entity.getId());
		});
	}

	@Test
	void testDeletePetWithMoreThanOneMedicalEvent() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity entity = petList.get(0);
			List<MedicalEventEntity> events = new ArrayList<>();
			for (int i = 0; i < 2; i++) {
				MedicalEventEntity event = factory.manufacturePojo(MedicalEventEntity.class);
				event.setPet(entity);
				events.add(event);
			}
			entity.setMedicalEvents(events);
			petService.deletePet(entity.getId());
		});
	}
	
	@Test
	void testDeletePetWithActiveTrialCohabitation() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity entity = petList.get(0);

			TrialCohabitationRequestEntity request = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			request.setPet(entity);
			request.setShelter(shelter);
			entityManager.persist(request);

			TrialCohabitationEntity cohabitation = factory.manufacturePojo(TrialCohabitationEntity.class);
			cohabitation.setShelter(shelter);
			cohabitation.setTrialCohabitationRequest(request);
			cohabitation.setReturnDuringTrial(null);
			entityManager.persist(cohabitation);

			petService.deletePet(entity.getId());
		});
	}

	@Test
	void testDeletePetWithCompletedTrialCohabitation() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity entity = petList.get(0);

			TrialCohabitationRequestEntity request = factory.manufacturePojo(TrialCohabitationRequestEntity.class);
			request.setPet(entity);
			request.setShelter(shelter);
			entityManager.persist(request);

			TrialCohabitationEntity cohabitation = factory.manufacturePojo(TrialCohabitationEntity.class);
			cohabitation.setShelter(shelter);
			cohabitation.setTrialCohabitationRequest(request);
			entityManager.persist(cohabitation);

			ReturnEntity returnRecord = factory.manufacturePojo(ReturnEntity.class);
			returnRecord.setTrialCohabitation(cohabitation);
			returnRecord.setShelter(shelter);
			entityManager.persist(returnRecord);

			cohabitation.setReturnDuringTrial(returnRecord);

			petService.deletePet(entity.getId());
		});
	}
}
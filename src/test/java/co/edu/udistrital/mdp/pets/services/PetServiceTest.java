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
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
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
	void testCreatePetWithNullName() {
		assertThrows(IllegalOperationException.class, () -> {
			PetEntity newEntity = newValidPet();
			newEntity.setName(null);
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
	void testGetPetsFiltered() {
		PetEntity target = petList.get(0);
		List<PetEntity> list = petService.getPets(target.getSpecies(), null, null, null, null, null, null);
		assertTrue(list.stream().anyMatch(p -> p.getId().equals(target.getId())));

		List<PetEntity> emptyList = petService.getPets("unmatched-species-xyz", null, null, null, null, null, null);
		assertTrue(emptyList.isEmpty());
	}

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
	void testGetNonExistentPet() {
		assertThrows(EntityNotFoundException.class, () -> {
			petService.getPet(1000L);
		});
	}

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
	void testUpdatePetInvalidId() {
		assertThrows(EntityNotFoundException.class, () -> {
			PetEntity pojoEntity = factory.manufacturePojo(PetEntity.class);
			pojoEntity.setCompatibilityChildren(true);
			pojoEntity.setCompatibilityOtherPets(true);
			petService.updatePet(1000L, pojoEntity);
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
	void testDeletePet() throws EntityNotFoundException, IllegalOperationException {
		PetEntity entity = petList.get(0);
		petService.deletePet(entity.getId());
		PetEntity deleted = entityManager.find(PetEntity.class, entity.getId());
		assertNull(deleted);
	}

	@Test
	void testDeleteInvalidPet() {
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
			entityManager.persist(adoption);
			petService.deletePet(entity.getId());
		});
	}
}
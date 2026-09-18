package co.edu.udistrital.mdp.pets.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.MedicalEventEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.MedicalEventRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.ShelterRepository;
import co.edu.udistrital.mdp.pets.repositories.TrialCohabitationRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PetService {

	@Autowired
	PetRepository petRepository;

	@Autowired
	ShelterRepository shelterRepository;

	@Autowired
	MedicalEventRepository medicalEventRepository;

	@Autowired
	TrialCohabitationRepository trialCohabitationRepository;

	@Autowired
	AdoptionRepository adoptionRepository;

	@Transactional
	public PetEntity createPet(PetEntity pet) throws EntityNotFoundException, IllegalOperationException {
		log.info("Start the pet creation process");

		validateMandatoryAttributes(pet);

		if (pet.getShelter() == null || pet.getShelter().getId() == null)
			throw new IllegalOperationException("Pet must be associated with an existing shelter");
		Optional<ShelterEntity> shelter = shelterRepository.findById(pet.getShelter().getId());
		if (shelter.isEmpty())
			throw new EntityNotFoundException("Shelter not found");

		if (pet.getAdmissionDate().after(new Date()))
			throw new IllegalOperationException("Admission date cannot be a future date");

		List<MedicalEventEntity> arrivalEvents = pet.getMedicalEvents() == null ? new ArrayList<>()
				: new ArrayList<>(pet.getMedicalEvents());
		if (arrivalEvents.isEmpty())
			throw new IllegalOperationException(
					"A pet must be registered along with at least one medical event describing how it arrived at the shelter");

		pet.setShelter(shelter.get());
		pet.setMedicalEvents(new ArrayList<>());

		boolean duplicated = petRepository.findAll().stream().anyMatch(p -> isSamePet(p, pet));
		if (duplicated)
			throw new IllegalOperationException(
					"A pet already exists for this shelter with the same name, species and admission date");

		PetEntity savedPet = petRepository.save(pet);

		for (MedicalEventEntity arrivalEvent : arrivalEvents) {
			arrivalEvent.setPet(savedPet);
			MedicalEventEntity savedEvent = medicalEventRepository.save(arrivalEvent);
			savedPet.getMedicalEvents().add(savedEvent);
		}

		log.info("Pet creation process ends");
		return savedPet;
	}

	@Transactional
	public List<PetEntity> getPets() {
		log.info("Start the process of consulting all pets");
		List<PetEntity> pets = petRepository.findAll();
		if (pets.isEmpty())
			log.info("There are no registered pets");
		return pets;
	}

	@Transactional
	public List<PetEntity> getPets(String species, Integer age, String size, String requiredSpace,
			Boolean compatibilityChildren, Boolean compatibilityOtherPets, String activityLevel) {
		log.info("Start the process of consulting leaked pets");
		List<PetEntity> pets = petRepository.findAll().stream()
				.filter(p -> species == null || species.equalsIgnoreCase(p.getSpecies()))
				.filter(p -> age == null || age.equals(p.getAge()))
				.filter(p -> size == null || size.equalsIgnoreCase(p.getSize()))
				.filter(p -> requiredSpace == null || requiredSpace.equalsIgnoreCase(p.getRequiredSpace()))
				.filter(p -> compatibilityChildren == null || compatibilityChildren.equals(p.getCompatibilityChildren()))
				.filter(p -> compatibilityOtherPets == null || compatibilityOtherPets.equals(p.getCompatibilityOtherPets()))
				.filter(p -> activityLevel == null || activityLevel.equalsIgnoreCase(p.getActivityLevel()))
				.toList();
		if (pets.isEmpty())
			log.info("There are no registered pets that meet the filters");
		return pets;
	}

	@Transactional
	public PetEntity getPet(Long petId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Start the process of consulting the pet with id = {}", petId);
		if (petId == null || petId <= 0)
			throw new IllegalOperationException("Pet id is not valid");

		Optional<PetEntity> pet = petRepository.findById(petId);
		if (pet.isEmpty())
			throw new EntityNotFoundException("Pet not found");

		log.info("Finish the process of consulting the pet with id = {}", petId);
		return pet.get();
	}

	@Transactional
	public PetEntity updatePet(Long petId, PetEntity pet) throws EntityNotFoundException, IllegalOperationException {
		log.info("Starts process of updating the pet with id = {}", petId);
		if (petId == null || petId <= 0)
			throw new IllegalOperationException("Pet id is not valid");

		Optional<PetEntity> existing = petRepository.findById(petId);
		if (existing.isEmpty())
			throw new EntityNotFoundException("Pet not found");

		validateMandatoryAttributes(pet);

		PetEntity current = existing.get();

		if (pet.getAdmissionDate() != null && current.getAdmissionDate() != null
				&& !sameDay(pet.getAdmissionDate(), current.getAdmissionDate()))
			throw new IllegalOperationException(
					"Admission date cannot be modified once the pet has been created");

		ShelterEntity shelter = current.getShelter();
		if (pet.getShelter() != null && pet.getShelter().getId() != null
				&& (current.getShelter() == null || !pet.getShelter().getId().equals(current.getShelter().getId()))) {
			Optional<ShelterEntity> newShelter = shelterRepository.findById(pet.getShelter().getId());
			if (newShelter.isEmpty())
				throw new EntityNotFoundException("Shelter not found");
			shelter = newShelter.get();
		}

		pet.setShelter(shelter);
		pet.setAdmissionDate(current.getAdmissionDate());

		boolean duplicated = petRepository.findAll().stream()
				.filter(p -> !p.getId().equals(petId))
				.anyMatch(p -> isSamePet(p, pet));
		if (duplicated)
			throw new IllegalOperationException(
					"A pet already exists for this shelter with the same name, species and admission date");

		pet.setId(petId);
		pet.setMedicalEvents(current.getMedicalEvents());
		pet.setVaccinationRecord(current.getVaccinationRecord());
		pet.setAdoptionRequests(current.getAdoptionRequests());
		pet.setAdoptions(current.getAdoptions());
		pet.setReviews(current.getReviews());
		pet.setPhotos(current.getPhotos());

		log.info("Finish process of updating pet with id = {}", petId);
		return petRepository.save(pet);
	}

	@Transactional
	public void deletePet(Long petId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Start the process of deleting the pet with id = {}", petId);
		if (petId == null || petId <= 0)
			throw new IllegalOperationException("Pet id is not valid");

		Optional<PetEntity> pet = petRepository.findById(petId);
		if (pet.isEmpty())
			throw new EntityNotFoundException("Pet not found");

		PetEntity petEntity = pet.get();

		if (hasActiveAdoptionOrCohabitation(petEntity))
			throw new IllegalOperationException(
					"A pet with an active adoption or trial cohabitation cannot be deleted");

		if (hasAssociatedHistory(petEntity))
			throw new IllegalOperationException(
					"A pet with associated history cannot be deleted, in order to maintain traceability");

		petRepository.deleteById(petId);
		log.info("Finish the process of deleting the pet with id = {}", petId);
	}

	private boolean hasActiveAdoptionOrCohabitation(PetEntity pet) {
		boolean activeAdoption = adoptionRepository.findAll().stream()
				.filter(a -> a.getPet() != null && a.getPet().getId().equals(pet.getId()))
				.anyMatch(a -> a.getReturnAfterAdoption() == null);
		boolean activeCohabitation = trialCohabitationRepository.findAll().stream()
				.filter(t -> t.getTrialCohabitationRequest() != null && t.getTrialCohabitationRequest().getPet() != null
						&& t.getTrialCohabitationRequest().getPet().getId().equals(pet.getId()))
				.anyMatch(t -> t.getReturnDuringTrial() == null);
		return activeAdoption || activeCohabitation;
	}

	private boolean hasAssociatedHistory(PetEntity pet) {
		List<AdoptionEntity> adoptions = adoptionRepository.findAll().stream()
				.filter(a -> a.getPet() != null && a.getPet().getId().equals(pet.getId()))
				.toList();
		boolean moreThanOneMedicalEvent = pet.getMedicalEvents() != null && pet.getMedicalEvents().size() > 1;
		boolean hasVaccinationRecord = pet.getVaccinationRecord() != null
				&& pet.getVaccinationRecord().getVaccines() != null
				&& !pet.getVaccinationRecord().getVaccines().isEmpty();
		boolean hasAdoption = !adoptions.isEmpty();
		boolean hasReturn = adoptions.stream().anyMatch(a -> a.getReturnAfterAdoption() != null);
		boolean hasCohabitationReturn = trialCohabitationRepository.findAll().stream()
				.filter(t -> t.getTrialCohabitationRequest() != null && t.getTrialCohabitationRequest().getPet() != null
						&& t.getTrialCohabitationRequest().getPet().getId().equals(pet.getId()))
				.anyMatch(t -> t.getReturnDuringTrial() != null);
		return moreThanOneMedicalEvent || hasVaccinationRecord || hasAdoption || hasReturn || hasCohabitationReturn;
	}

	private void validateMandatoryAttributes(PetEntity pet) throws IllegalOperationException {
		if (pet.getName() == null || pet.getName().isBlank())
			throw new IllegalOperationException("Name cannot be null or empty");
		if (pet.getSpecies() == null || pet.getSpecies().isBlank())
			throw new IllegalOperationException("Species cannot be null or empty");
		if (pet.getBreed() == null || pet.getBreed().isBlank())
			throw new IllegalOperationException("Breed cannot be null or empty");
		if (pet.getAge() == null)
			throw new IllegalOperationException("Age cannot be null");
		if (pet.getSex() == null || pet.getSex().isBlank())
			throw new IllegalOperationException("Sex cannot be null or empty");
		if (pet.getSize() == null || pet.getSize().isBlank())
			throw new IllegalOperationException("Size cannot be null or empty");
		if (pet.getHealthStatus() == null || pet.getHealthStatus().isBlank())
			throw new IllegalOperationException("Health status cannot be null or empty");
		if (pet.getAdmissionDate() == null)
			throw new IllegalOperationException("Admission date cannot be null");
		if (pet.getCompatibilityChildren() == null)
			throw new IllegalOperationException("Compatibility with children cannot be null");
		if (pet.getCompatibilityOtherPets() == null)
			throw new IllegalOperationException("Compatibility with other pets cannot be null");
		if (pet.getActivityLevel() == null || pet.getActivityLevel().isBlank())
			throw new IllegalOperationException("Activity level cannot be null or empty");
		if (pet.getRequiredSpace() == null || pet.getRequiredSpace().isBlank())
			throw new IllegalOperationException("Required space cannot be null or empty");
	}

	private boolean isSamePet(PetEntity p1, PetEntity p2) {
		return p1.getShelter() != null && p2.getShelter() != null
				&& p1.getShelter().getId() != null
				&& p1.getShelter().getId().equals(p2.getShelter().getId())
				&& p1.getName() != null && p1.getName().equalsIgnoreCase(p2.getName())
				&& p1.getSpecies() != null && p1.getSpecies().equalsIgnoreCase(p2.getSpecies())
				&& p1.getAdmissionDate() != null && p2.getAdmissionDate() != null
				&& sameDay(p1.getAdmissionDate(), p2.getAdmissionDate());
	}

	private boolean sameDay(Date d1, Date d2) {
		return truncate(d1).equals(truncate(d2));
	}

	private Date truncate(Date date) {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.setTime(date);
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		return c.getTime();
	}
}
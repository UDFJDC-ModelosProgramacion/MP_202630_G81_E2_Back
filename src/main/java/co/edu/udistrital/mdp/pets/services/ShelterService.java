package co.edu.udistrital.mdp.pets.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.ShelterRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ShelterService {

	@Autowired
	ShelterRepository shelterRepository;

	@Autowired
	AdoptionRepository adoptionRepository;

	@Autowired
	PetRepository petRepository;

	@Transactional
	public ShelterEntity createShelter(ShelterEntity shelter) throws IllegalOperationException {
		log.info("Refuge creation process begins");

		validateMandatoryAttributes(shelter);

		boolean nitAlreadyUsed = shelterRepository.findAll().stream()
				.anyMatch(s -> s.getNit() != null && s.getNit().equalsIgnoreCase(shelter.getNit()));
		if (nitAlreadyUsed)
			throw new IllegalOperationException("The nit is already registered for another shelter");

		boolean duplicated = shelterRepository.findAll().stream().anyMatch(s -> isSameShelter(s, shelter));
		if (duplicated)
			throw new IllegalOperationException(
					"A shelter already exists with the same name, city and location");

		List<UserEntity> users = shelter.getUsers() == null ? new ArrayList<>() : shelter.getUsers();
		if (users.isEmpty())
			throw new IllegalOperationException(
					"A shelter must be associated with at least one user responsible for its administration");

		log.info("Shelter creation process ends");
		return shelterRepository.save(shelter);
	}

	@Transactional
	public List<ShelterEntity> readShelter(String city, String location) {
		log.info("The process of consulting leaked shelters begins");
		List<ShelterEntity> shelters = shelterRepository.findAll().stream()
				.filter(s -> city == null || city.equalsIgnoreCase(s.getCity()))
				.filter(s -> location == null || location.equalsIgnoreCase(s.getLocation()))
				.toList();
		if (shelters.isEmpty())
			log.info("There are no registered shelters that meet the filters");
		return shelters;
	}

	@Transactional
	public List<ShelterEntity> readShelter() {
		return readShelter(null, null);
	}

	@Transactional
	public ShelterEntity readAllShelters(Long shelterId) throws EntityNotFoundException, IllegalOperationException {
		return readAllShelters(shelterId, null, null);
	}

	@Transactional
	public ShelterEntity readAllShelters(Long shelterId, String name, String nit)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("The process of consulting a shelter through filters begins");

		if (shelterId != null && shelterId <= 0)
			throw new IllegalOperationException("Shelter id is not valid");

		Optional<ShelterEntity> shelter = shelterRepository.findAll().stream()
				.filter(s -> shelterId == null || shelterId.equals(s.getId()))
				.filter(s -> name == null || name.equalsIgnoreCase(s.getName()))
				.filter(s -> nit == null || nit.equalsIgnoreCase(s.getNit()))
				.findFirst();

		if (shelter.isEmpty())
			throw new EntityNotFoundException("Shelter not found");

		log.info("The process of consulting a shelter through filters ends");
		return shelter.get();
	}

	@Transactional
	public ShelterEntity updateShelter(Long shelterId, ShelterEntity shelter)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Starts process of updating the shelter with id = {}", shelterId);
		if (shelterId == null || shelterId <= 0)
			throw new IllegalOperationException("Shelter id is not valid");

		Optional<ShelterEntity> existing = shelterRepository.findById(shelterId);
		if (existing.isEmpty())
			throw new EntityNotFoundException("Shelter not found");

		validateMandatoryAttributes(shelter);

		ShelterEntity current = existing.get();

		if (shelter.getNit() != null && !shelter.getNit().equalsIgnoreCase(current.getNit()))
			throw new IllegalOperationException(
					"The nit cannot be modified once the shelter has been created");

		boolean duplicated = shelterRepository.findAll().stream()
				.filter(s -> !s.getId().equals(shelterId))
				.anyMatch(s -> isSameShelter(s, shelter));
		if (duplicated)
			throw new IllegalOperationException(
					"A shelter already exists with the same name, city and location");

		shelter.setId(shelterId);
		shelter.setNit(current.getNit());
		shelter.setPets(current.getPets());
		shelter.setPhotos(current.getPhotos());
		shelter.setCohabitationRequests(current.getCohabitationRequests());
		shelter.setAdoptionRequests(current.getAdoptionRequests());
		shelter.setTrialCohabitations(current.getTrialCohabitations());
		shelter.setAdoptions(current.getAdoptions());
		shelter.setReturnsDuringTrial(current.getReturnsDuringTrial());
		shelter.setEvents(current.getEvents());
		shelter.setUsers(current.getUsers());
		shelter.setAdopters(current.getAdopters());
		shelter.setVeterinarians(current.getVeterinarians());

		log.info("Finish process of updating the shelter with id = {}", shelterId);
		return shelterRepository.save(shelter);
	}

	@Transactional
	public void deleteShelter(Long shelterId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Starts process of deleting the shelter with id = {}", shelterId);
		if (shelterId == null || shelterId <= 0)
			throw new IllegalOperationException("Shelter id is not valid");

		Optional<ShelterEntity> shelter = shelterRepository.findById(shelterId);
		if (shelter.isEmpty())
			throw new EntityNotFoundException("Shelter not found");

		ShelterEntity current = shelter.get();

		List<PetEntity> pets = petRepository.findAll().stream()
				.filter(p -> p.getShelter() != null && p.getShelter().getId().equals(shelterId))
				.toList();
		if (!pets.isEmpty())
			throw new IllegalOperationException("A shelter with pets currently under its care cannot be deleted");

		List<AdoptionEntity> adoptions = adoptionRepository.findAll().stream()
				.filter(a -> a.getShelter() != null && a.getShelter().getId().equals(shelterId))
				.toList();

		boolean hasActiveProcesses = (current.getAdoptionRequests() != null && !current.getAdoptionRequests().isEmpty())
				|| (current.getCohabitationRequests() != null && !current.getCohabitationRequests().isEmpty())
				|| (current.getTrialCohabitations() != null && !current.getTrialCohabitations().isEmpty())
				|| adoptions.stream().anyMatch(a -> a.getReturnAfterAdoption() == null);
		if (hasActiveProcesses)
			throw new IllegalOperationException(
					"A shelter with active associated processes in progress cannot be deleted");

		boolean hasHistory = !adoptions.isEmpty()
				|| (current.getEvents() != null && !current.getEvents().isEmpty())
				|| (current.getVeterinarians() != null && !current.getVeterinarians().isEmpty())
				|| (current.getReturnsDuringTrial() != null && !current.getReturnsDuringTrial().isEmpty());
		if (hasHistory)
			throw new IllegalOperationException(
					"A shelter with associated history cannot be deleted, in order to maintain traceability");
		
		shelterRepository.deleteById(shelterId);
		log.info("Finish process of deleting the shelter with id = {}", shelterId);
	}

	private void validateMandatoryAttributes(ShelterEntity shelter) throws IllegalOperationException {
		if (shelter.getName() == null || shelter.getName().isBlank())
			throw new IllegalOperationException("Name cannot be null or empty");
		if (shelter.getCity() == null || shelter.getCity().isBlank())
			throw new IllegalOperationException("City cannot be null or empty");
		if (shelter.getLocation() == null || shelter.getLocation().isBlank())
			throw new IllegalOperationException("Location cannot be null or empty");
		if (shelter.getNit() == null || shelter.getNit().isBlank())
			throw new IllegalOperationException("Nit cannot be null or empty");
	}

	private boolean isSameShelter(ShelterEntity s1, ShelterEntity s2) {
		return s1.getName() != null && s1.getName().equalsIgnoreCase(s2.getName())
				&& s1.getCity() != null && s1.getCity().equalsIgnoreCase(s2.getCity())
				&& s1.getLocation() != null && s1.getLocation().equalsIgnoreCase(s2.getLocation());
	}
}
package co.edu.udistrital.mdp.pets.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;
import co.edu.udistrital.mdp.pets.repositories.VeterinarianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VeterinarianService {

	private static final String VETERINARIAN_ID_NOT_VALID = "Veterinarian id is not valid";
	private static final String VETERINARIAN_NOT_FOUND = "Veterinarian not found";

	private final VeterinarianRepository veterinarianRepository;
	private final UserRepository userRepository;

	@Transactional
	public VeterinarianEntity createVeterinarian(VeterinarianEntity veterinarian)
			throws IllegalOperationException {
		log.info("Starting process to create a veterinarian");

		if (veterinarian.getFirstName() == null || veterinarian.getFirstName().isBlank())
			throw new IllegalOperationException("First name cannot be null or empty");
		if (veterinarian.getLastName() == null || veterinarian.getLastName().isBlank())
			throw new IllegalOperationException("Last name cannot be null or empty");
		if (veterinarian.getEmail() == null || veterinarian.getEmail().isBlank())
			throw new IllegalOperationException("Email cannot be null or empty");
		if (veterinarian.getPhone() == null || veterinarian.getPhone().isBlank())
			throw new IllegalOperationException("Phone cannot be null or empty");
		if (veterinarian.getSpecialization() == null || veterinarian.getSpecialization().isBlank())
			throw new IllegalOperationException("Specialization cannot be null or empty");
		if (veterinarian.getAvailability() == null || veterinarian.getAvailability().isBlank())
			throw new IllegalOperationException("Availability cannot be null or empty");

		boolean emailAlreadyUsed = userRepository.findAll().stream()
				.anyMatch(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(veterinarian.getEmail()));
		if (emailAlreadyUsed)
			throw new IllegalOperationException(
					"Two veterinarians cannot be associated with the same user; this user already exists");

		log.info("Ending process to create a veterinarian");
		return veterinarianRepository.save(veterinarian);
	}

	@Transactional
	public List<VeterinarianEntity> getVeterinarians() {
		log.info("Starting process to retrieve all veterinarians");
		List<VeterinarianEntity> veterinarians = veterinarianRepository.findAll();
		if (veterinarians.isEmpty())
			log.info("No veterinarians found");
		return veterinarians;
	}

	@Transactional
	public List<VeterinarianEntity> getVeterinarians(String specialization, String availability) {
		log.info("Starting process to retrieve filtered veterinarians");
		List<VeterinarianEntity> veterinarians = veterinarianRepository.findAll().stream()
				.filter(v -> specialization == null || specialization.equalsIgnoreCase(v.getSpecialization()))
				.filter(v -> availability == null || availability.equalsIgnoreCase(v.getAvailability()))
				.toList();
		if (veterinarians.isEmpty())
			log.info("No filtered veterinarians found");
		return veterinarians;
	}

	@Transactional
	public VeterinarianEntity getVeterinarian(Long veterinarianId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Starting process to retrieve a veterinarian");
		if (veterinarianId == null || veterinarianId <= 0)
			throw new IllegalOperationException(VETERINARIAN_ID_NOT_VALID);

		Optional<VeterinarianEntity> veterinarian = veterinarianRepository.findById(veterinarianId);
		if (veterinarian.isEmpty())
			throw new EntityNotFoundException(VETERINARIAN_NOT_FOUND);

		log.info("Ending process to retrieve a veterinarian");
		return veterinarian.get();
	}

	@Transactional
	public VeterinarianEntity updateVeterinarian(Long veterinarianId, VeterinarianEntity veterinarian)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Starting process to update a veterinarian");
		if (veterinarianId == null || veterinarianId <= 0)
			throw new IllegalOperationException(VETERINARIAN_ID_NOT_VALID);

		Optional<VeterinarianEntity> existing = veterinarianRepository.findById(veterinarianId);
		if (existing.isEmpty())
			throw new EntityNotFoundException(VETERINARIAN_NOT_FOUND);

		if (veterinarian.getSpecialization() == null || veterinarian.getSpecialization().isBlank())
			throw new IllegalOperationException("Specialization cannot be null or empty");
		if (veterinarian.getAvailability() == null || veterinarian.getAvailability().isBlank())
			throw new IllegalOperationException("Availability cannot be null or empty");

		VeterinarianEntity current = existing.get();
		current.setSpecialization(veterinarian.getSpecialization());
		current.setAvailability(veterinarian.getAvailability());

		log.info("Ending process to update a veterinarian");
		return veterinarianRepository.save(current);
	}

	@Transactional
	public void deleteVeterinarian(Long veterinarianId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Starting process to delete a veterinarian");
		if (veterinarianId == null || veterinarianId <= 0)
			throw new IllegalOperationException(VETERINARIAN_ID_NOT_VALID);

		Optional<VeterinarianEntity> veterinarian = veterinarianRepository.findById(veterinarianId);
		if (veterinarian.isEmpty())
			throw new EntityNotFoundException(VETERINARIAN_NOT_FOUND);
		VeterinarianEntity current = veterinarian.get();
		if (!current.getMedicalEvents().isEmpty() || !current.getFollowUps().isEmpty())
			throw new IllegalOperationException(
					"A veterinarian with active or past medical events or follow-ups cannot be deleted");

		veterinarianRepository.deleteById(veterinarianId);
		log.info("Ending process to delete a veterinarian");
	}
}

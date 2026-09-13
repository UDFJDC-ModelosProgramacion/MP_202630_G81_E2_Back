package co.edu.udistrital.mdp.ZZZ.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.ZZZ.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.ZZZ.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.ZZZ.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.ZZZ.repositories.UserRepository;
import co.edu.udistrital.mdp.ZZZ.repositories.VeterinarianRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VeterinarianService {

	@Autowired
	VeterinarianRepository veterinarianRepository;

	@Autowired
	UserRepository userRepository;

	/**
	 * Crea un nuevo veterinario.
	 */
	@Transactional
	public VeterinarianEntity createVeterinarian(VeterinarianEntity veterinarian)
			throws IllegalOperationException {
		log.info("Inicia proceso de creación del veterinario");

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

		log.info("Termina proceso de creación del veterinario");
		return veterinarianRepository.save(veterinarian);
	}

	/**
	 * Obtiene todos los veterinarios registrados.
	 */
	@Transactional
	public List<VeterinarianEntity> getVeterinarians() {
		log.info("Inicia proceso de consultar todos los veterinarios");
		List<VeterinarianEntity> veterinarians = veterinarianRepository.findAll();
		if (veterinarians.isEmpty())
			log.info("No hay veterinarios registrados");
		return veterinarians;
	}

	/**
	 * Obtiene los veterinarios filtrando, de forma opcional, por especialización
	 * y/o disponibilidad.
	 */
	@Transactional
	public List<VeterinarianEntity> getVeterinarians(String specialization, String availability) {
		log.info("Inicia proceso de consultar veterinarios filtrados");
		List<VeterinarianEntity> veterinarians = veterinarianRepository.findAll().stream()
				.filter(v -> specialization == null || specialization.equalsIgnoreCase(v.getSpecialization()))
				.filter(v -> availability == null || availability.equalsIgnoreCase(v.getAvailability()))
				.toList();
		if (veterinarians.isEmpty())
			log.info("No hay veterinarios registrados que cumplan los filtros");
		return veterinarians;
	}

	/**
	 * Obtiene un veterinario a partir de su id.
	 */
	@Transactional
	public VeterinarianEntity getVeterinarian(Long veterinarianId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar el veterinario con id = {}", veterinarianId);
		if (veterinarianId == null || veterinarianId <= 0)
			throw new IllegalOperationException("Veterinarian id is not valid");

		Optional<VeterinarianEntity> veterinarian = veterinarianRepository.findById(veterinarianId);
		if (veterinarian.isEmpty())
			throw new EntityNotFoundException("Veterinarian not found");

		log.info("Termina proceso de consultar el veterinario con id = {}", veterinarianId);
		return veterinarian.get();
	}

	/**
	 * Actualiza un veterinario existente. Solo se pueden modificar los atributos
	 * specialization y availability.
	 */
	@Transactional
	public VeterinarianEntity updateVeterinarian(Long veterinarianId, VeterinarianEntity veterinarian)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar el veterinario con id = {}", veterinarianId);
		if (veterinarianId == null || veterinarianId <= 0)
			throw new IllegalOperationException("Veterinarian id is not valid");

		Optional<VeterinarianEntity> existing = veterinarianRepository.findById(veterinarianId);
		if (existing.isEmpty())
			throw new EntityNotFoundException("Veterinarian not found");

		if (veterinarian.getSpecialization() == null || veterinarian.getSpecialization().isBlank())
			throw new IllegalOperationException("Specialization cannot be null or empty");
		if (veterinarian.getAvailability() == null || veterinarian.getAvailability().isBlank())
			throw new IllegalOperationException("Availability cannot be null or empty");

		VeterinarianEntity current = existing.get();
		current.setSpecialization(veterinarian.getSpecialization());
		current.setAvailability(veterinarian.getAvailability());

		log.info("Termina proceso de actualizar el veterinario con id = {}", veterinarianId);
		return veterinarianRepository.save(current);
	}

	/**
	 * Borra un veterinario a partir de su id.
	 */
	@Transactional
	public void deleteVeterinarian(Long veterinarianId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar el veterinario con id = {}", veterinarianId);
		if (veterinarianId == null || veterinarianId <= 0)
			throw new IllegalOperationException("Veterinarian id is not valid");

		Optional<VeterinarianEntity> veterinarian = veterinarianRepository.findById(veterinarianId);
		if (veterinarian.isEmpty())
			throw new EntityNotFoundException("Veterinarian not found");

		VeterinarianEntity current = veterinarian.get();
		if (!current.getMedicalEvents().isEmpty() || !current.getFollowUps().isEmpty())
			throw new IllegalOperationException(
					"A veterinarian with active or past medical events or follow-ups cannot be deleted");

		veterinarianRepository.deleteById(veterinarianId);
		log.info("Termina proceso de borrar el veterinario con id = {}", veterinarianId);
	}
}

package co.edu.udistrital.mdp.ZZZ.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.ZZZ.entities.AdopterEntity;
import co.edu.udistrital.mdp.ZZZ.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.ZZZ.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.ZZZ.repositories.AdopterRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AdopterService {

	private static final String APPROVED_STATUS = "APPROVED";
	private static final String IN_PROGRESS_STATUS = "IN_PROGRESS";

	@Autowired
	AdopterRepository adopterRepository;

	/**
	 * Crea un nuevo adoptante.
	 */
	@Transactional
	public AdopterEntity createAdopter(AdopterEntity adopter) throws IllegalOperationException {
		log.info("Inicia proceso de creación del adoptante");

		validateRequiredAttributes(adopter);

		boolean duplicateNationalId = adopterRepository.findAll().stream()
				.anyMatch(a -> a.getNationalId() != null && a.getNationalId().equalsIgnoreCase(adopter.getNationalId()));
		if (duplicateNationalId)
			throw new IllegalOperationException("An adopter with the same national id already exists");

		log.info("Termina proceso de creación del adoptante");
		return adopterRepository.save(adopter);
	}

	/**
	 * Obtiene un adoptante a partir de su id.
	 */
	@Transactional
	public AdopterEntity getAdopter(Long adopterId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar el adoptante con id = {}", adopterId);
		if (adopterId == null || adopterId <= 0)
			throw new IllegalOperationException("Adopter id is not valid");

		Optional<AdopterEntity> adopter = adopterRepository.findById(adopterId);
		if (adopter.isEmpty())
			throw new EntityNotFoundException("Adopter not found");

		log.info("Termina proceso de consultar el adoptante con id = {}", adopterId);
		return adopter.get();
	}

	/**
	 * Obtiene todos los adoptantes registrados.
	 */
	@Transactional
	public List<AdopterEntity> getAdopters() {
		log.info("Inicia proceso de consultar todos los adoptantes");
		List<AdopterEntity> adopters = adopterRepository.findAll();
		if (adopters.isEmpty())
			log.info("No hay adoptantes registrados");
		return adopters;
	}

	/**
	 * Obtiene los adoptantes filtrando, de forma opcional, por nombre, apellido,
	 * documento de identidad y tipo de vivienda. Si se combinan varios filtros,
	 * todos deben coincidir para retornar un adoptante.
	 */
	@Transactional
	public List<AdopterEntity> getAdopters(String firstName, String lastName, String nationalId, String housingType) {
		log.info("Inicia proceso de consultar adoptantes por filtros");
		List<AdopterEntity> adopters = adopterRepository.findAll().stream()
				.filter(a -> firstName == null || (a.getFirstName() != null && a.getFirstName().equalsIgnoreCase(firstName)))
				.filter(a -> lastName == null || (a.getLastName() != null && a.getLastName().equalsIgnoreCase(lastName)))
				.filter(a -> nationalId == null || (a.getNationalId() != null && a.getNationalId().equalsIgnoreCase(nationalId)))
				.filter(a -> housingType == null
						|| (a.getHousingType() != null && a.getHousingType().equalsIgnoreCase(housingType)))
				.toList();
		if (adopters.isEmpty())
			log.info("No hay adoptantes registrados que cumplan los filtros");
		return adopters;
	}

	/**
	 * Actualiza un adoptante existente. El documento de identidad no puede ser
	 * modificado una vez el adoptante ha sido creado.
	 */
	@Transactional
	public AdopterEntity updateAdopter(Long adopterId, AdopterEntity adopter)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar el adoptante con id = {}", adopterId);
		if (adopterId == null || adopterId <= 0)
			throw new IllegalOperationException("Adopter id is not valid");

		Optional<AdopterEntity> existing = adopterRepository.findById(adopterId);
		if (existing.isEmpty())
			throw new EntityNotFoundException("Adopter not found");

		validateRequiredAttributes(adopter);

		AdopterEntity current = existing.get();
		if (adopter.getNationalId() != null && !adopter.getNationalId().equalsIgnoreCase(current.getNationalId()))
			throw new IllegalOperationException("The national id cannot be modified after the adopter has been created");

		current.setFirstName(adopter.getFirstName());
		current.setLastName(adopter.getLastName());
		current.setEmail(adopter.getEmail());
		current.setPassword(adopter.getPassword());
		current.setPhone(adopter.getPhone());
		current.setAddress(adopter.getAddress());
		current.setOccupation(adopter.getOccupation());
		current.setEarnings(adopter.getEarnings());
		current.setHousingType(adopter.getHousingType());
		current.setAllergies(adopter.getAllergies());
		current.setHasChildren(adopter.getHasChildren());
		current.setHasOtherPets(adopter.getHasOtherPets());

		log.info("Termina proceso de actualizar el adoptante con id = {}", adopterId);
		return adopterRepository.save(current);
	}

	/**
	 * Borra un adoptante a partir de su id. No se puede eliminar un adoptante que
	 * tenga solicitudes de adopción aprobadas o en proceso.
	 */
	@Transactional
	public void deleteAdopter(Long adopterId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar el adoptante con id = {}", adopterId);
		if (adopterId == null || adopterId <= 0)
			throw new IllegalOperationException("Adopter id is not valid");

		Optional<AdopterEntity> adopter = adopterRepository.findById(adopterId);
		if (adopter.isEmpty())
			throw new EntityNotFoundException("Adopter not found");

		boolean hasActiveProcess = adopter.get().getAdoptionRequests().stream()
				.anyMatch(r -> r.getStatus() != null
						&& (APPROVED_STATUS.equalsIgnoreCase(r.getStatus())
								|| IN_PROGRESS_STATUS.equalsIgnoreCase(r.getStatus())));
		if (hasActiveProcess)
			throw new IllegalOperationException(
					"An adopter with approved or in-process adoption requests cannot be deleted; it must be deactivated");

		adopterRepository.deleteById(adopterId);
		log.info("Termina proceso de borrar el adoptante con id = {}", adopterId);
	}

	private void validateRequiredAttributes(AdopterEntity adopter) throws IllegalOperationException {
		if (adopter.getFirstName() == null || adopter.getFirstName().isBlank())
			throw new IllegalOperationException("First name cannot be null or empty");
		if (adopter.getLastName() == null || adopter.getLastName().isBlank())
			throw new IllegalOperationException("Last name cannot be null or empty");
		if (adopter.getEmail() == null || adopter.getEmail().isBlank())
			throw new IllegalOperationException("Email cannot be null or empty");
		if (adopter.getPassword() == null || adopter.getPassword().isBlank())
			throw new IllegalOperationException("Password cannot be null or empty");
		if (adopter.getPhone() == null || adopter.getPhone().isBlank())
			throw new IllegalOperationException("Phone cannot be null or empty");
		if (adopter.getAddress() == null || adopter.getAddress().isBlank())
			throw new IllegalOperationException("Address cannot be null or empty");
		if (adopter.getNationalId() == null || adopter.getNationalId().isBlank())
			throw new IllegalOperationException("National id cannot be null or empty");
		if (adopter.getOccupation() == null || adopter.getOccupation().isBlank())
			throw new IllegalOperationException("Occupation cannot be null or empty");
		if (adopter.getHousingType() == null || adopter.getHousingType().isBlank())
			throw new IllegalOperationException("Housing type cannot be null or empty");
	}
}
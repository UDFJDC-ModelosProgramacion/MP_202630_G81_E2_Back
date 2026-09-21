package co.edu.udistrital.mdp.pets.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdopterRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRequestRepository;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor 
public class AdopterService {

	// Estados de solicitud de adopción que impiden eliminar al adoptante
	private static final Set<String> ACTIVE_REQUEST_STATUSES = Set.of("PENDING", "APPROVED", "IN_PROGRESS");

	private static final String ADOPTER_NOT_FOUND = "Adopter not found";
	private static final String ADOPTER_ID_NOT_VALID = "Adopter id is not valid";
	private static final String SEARCH_FILTERS_CANNOT_BE_EMPTY = "Search filters cannot be empty";

	private final AdopterRepository adopterRepository;
	private final UserRepository userRepository;
	private final AdoptionRequestRepository adoptionRequestRepository;

	/**
	 * Crea un nuevo adoptante.
	 */
	@Transactional 
	public AdopterEntity createAdopter(AdopterEntity adopter) throws IllegalOperationException {
		log.info("The process of creating the adopter record begins.");
 
		validateIdentityAndContactFields(adopter);
		validateProfileFields(adopter);
		validateNoDuplicates(adopter);
 
		log.info("Adopter creation process completed.");
		return adopterRepository.save(adopter);
	}
 
	private void validateIdentityAndContactFields(AdopterEntity adopter) throws IllegalOperationException {
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
	}
 
	private void validateProfileFields(AdopterEntity adopter) throws IllegalOperationException {
		if (adopter.getOccupation() == null || adopter.getOccupation().isBlank())
			throw new IllegalOperationException("Occupation cannot be null or empty");
		if (adopter.getEarnings() == null)
			throw new IllegalOperationException("Earnings cannot be null");
		if (adopter.getHousingType() == null || adopter.getHousingType().isBlank())
			throw new IllegalOperationException("Housing type cannot be null or empty");
		if (adopter.getAllergies() == null || adopter.getAllergies().isBlank())
			throw new IllegalOperationException("Allergies cannot be null or empty");
		if (adopter.getHasChildren() == null)
			throw new IllegalOperationException("Has children cannot be null");
		if (adopter.getHasOtherPets() == null)
			throw new IllegalOperationException("Has other pets cannot be null");
	}
 
	private void validateNoDuplicates(AdopterEntity adopter) throws IllegalOperationException {
		// El usuario asociado debe existir (y no estar duplicado) en el sistema.
		boolean userAlreadyExists = userRepository.findAll().stream()
				.anyMatch(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(adopter.getEmail()));
		if (userAlreadyExists)
			throw new IllegalOperationException(
					"The user associated with this adopter already exists; this email is already registered");
 
		boolean nationalIdDuplicate = adopterRepository.findAll().stream()
				.anyMatch(a -> a.getNationalId() != null
						&& a.getNationalId().equalsIgnoreCase(adopter.getNationalId()));
		if (nationalIdDuplicate)
			throw new IllegalOperationException("An adopter with the same national id already exists");
	}

	/**
	 * Obtiene un adoptante a partir de su id.
	 */
	@Transactional
	public AdopterEntity readAdopter(Long adopterId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Initiate the process of querying the adopter with id = {}", adopterId);
		if (adopterId == null || adopterId <= 0)
			throw new IllegalOperationException(ADOPTER_ID_NOT_VALID);

		Optional<AdopterEntity> adopter = adopterRepository.findById(adopterId);
		if (adopter.isEmpty())
			throw new EntityNotFoundException(ADOPTER_NOT_FOUND);

		log.info("Process of querying the adopter with id = {} completed.", adopterId);
		return adopter.get();
	}

	/**
	 * Obtiene todos los adoptantes registrados.
	 */
	@Transactional
	public List<AdopterEntity> readAllAdopters() {
		log.info("Initiate the process of querying all adopters.");
		List<AdopterEntity> adopters = adopterRepository.findAll();
		if (adopters.isEmpty())
			log.info("There are no registered adopters.");
		return adopters;
	}

	/**
	 * Obtiene los adoptantes filtrando, de forma opcional, por documento de
	 * identidad, tipo de vivienda y/o ocupación. Si se combinan varios filtros,
	 * todos deben coincidir para retornar un resultado.
	 */
	@Transactional
	public List<AdopterEntity> readAllAdopters(String nationalId, String housingType, String occupation)
			throws IllegalOperationException {
		log.info("The process of consulting screened adopters begins.");
		if (nationalId != null && nationalId.isBlank())
			throw new IllegalOperationException(SEARCH_FILTERS_CANNOT_BE_EMPTY);
		if (housingType != null && housingType.isBlank())
			throw new IllegalOperationException(SEARCH_FILTERS_CANNOT_BE_EMPTY);
		if (occupation != null && occupation.isBlank())
			throw new IllegalOperationException(SEARCH_FILTERS_CANNOT_BE_EMPTY);

		List<AdopterEntity> adopters = adopterRepository.findAll().stream()
				.filter(a -> nationalId == null
						|| (a.getNationalId() != null && nationalId.equalsIgnoreCase(a.getNationalId())))
				.filter(a -> housingType == null
						|| (a.getHousingType() != null && housingType.equalsIgnoreCase(a.getHousingType())))
				.filter(a -> occupation == null
						|| (a.getOccupation() != null && occupation.equalsIgnoreCase(a.getOccupation())))
				.toList();
		if (adopters.isEmpty())
			log.info("There are no registered adopters who meet the criteria.");
		return adopters;
	}

	/**
	 * Actualiza un adoptante existente. El documento de identidad y el usuario
	 * asociado no pueden ser modificados tras la creación.
	 */
	@Transactional
	public AdopterEntity updateAdopter(Long adopterId, AdopterEntity adopter)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Initiating the process to update the adopter with id = {}", adopterId);
		if (adopterId == null || adopterId <= 0)
			throw new IllegalOperationException(ADOPTER_ID_NOT_VALID);
 
		Optional<AdopterEntity> existing = adopterRepository.findById(adopterId);
		if (existing.isEmpty())
			throw new EntityNotFoundException(ADOPTER_NOT_FOUND);
 
		validateUpdatableFields(adopter);
 
		AdopterEntity current = existing.get();
		applyUpdatableFields(current, adopter);
 
		log.info("Completes the process of updating the adopter with id = {}", adopterId);
		return adopterRepository.save(current);
	}
 
	private void validateUpdatableFields(AdopterEntity adopter) throws IllegalOperationException {
		if (adopter.getAddress() == null || adopter.getAddress().isBlank())
			throw new IllegalOperationException("Address cannot be null or empty");
		if (adopter.getOccupation() == null || adopter.getOccupation().isBlank())
			throw new IllegalOperationException("Occupation cannot be null or empty");
		if (adopter.getEarnings() == null)
			throw new IllegalOperationException("Earnings cannot be null");
		if (adopter.getHousingType() == null || adopter.getHousingType().isBlank())
			throw new IllegalOperationException("Housing type cannot be null or empty");
		if (adopter.getAllergies() == null || adopter.getAllergies().isBlank())
			throw new IllegalOperationException("Allergies cannot be null or empty");
		if (adopter.getHasChildren() == null)
			throw new IllegalOperationException("Has children cannot be null");
		if (adopter.getHasOtherPets() == null)
			throw new IllegalOperationException("Has other pets cannot be null");
	}
 
	private void applyUpdatableFields(AdopterEntity current, AdopterEntity adopter) {
		current.setAddress(adopter.getAddress());
		current.setOccupation(adopter.getOccupation());
		current.setEarnings(adopter.getEarnings());
		current.setHousingType(adopter.getHousingType());
		current.setAllergies(adopter.getAllergies());
		current.setHasChildren(adopter.getHasChildren());
		current.setHasOtherPets(adopter.getHasOtherPets());
	}
 

	/**
	 * Borra un adoptante a partir de su id.
	 */
	@Transactional
	public void deleteAdopter(Long adopterId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Starting the process to delete the adopter with id = {}", adopterId);
		if (adopterId == null || adopterId <= 0)
			throw new IllegalOperationException(ADOPTER_ID_NOT_VALID);

		Optional<AdopterEntity> adopter = adopterRepository.findById(adopterId);
		if (adopter.isEmpty())
			throw new EntityNotFoundException(ADOPTER_NOT_FOUND);

		boolean hasActiveRequests = adoptionRequestRepository.findAll().stream()
				.anyMatch(r -> r.getAdopter() != null && r.getAdopter().getId().equals(adopterId)
						&& r.getStatus() != null
						&& ACTIVE_REQUEST_STATUSES.contains(r.getStatus().toUpperCase()));
		if (hasActiveRequests)
			throw new IllegalOperationException(
					"An adopter with approved or in-progress adoption requests cannot be deleted; deactivate it instead");

		adopterRepository.deleteById(adopterId);
		log.info("Finished the process of deleting the adopter with id = {}", adopterId);
	}
}
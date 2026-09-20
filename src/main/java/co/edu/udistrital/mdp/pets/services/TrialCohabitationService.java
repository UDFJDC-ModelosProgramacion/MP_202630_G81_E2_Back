package co.edu.udistrital.mdp.pets.services;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdopterRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.ShelterRepository;
import co.edu.udistrital.mdp.pets.repositories.TrialCohabitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor 
public class TrialCohabitationService {

	public static final String PENDING_STATUS = "PENDING";
	public static final String IN_PROGRESS_STATUS = "IN_PROGRESS";
	public static final String FINALIZED_STATUS = "FINALIZED";

	private static final String TRIAL_ID_NOT_VALID = "Trial id is not valid";
	private static final String TRIAL_NOT_FOUND = "Trial cohabitation not found";

	private final TrialCohabitationRepository trialCohabitationRepository;
	private final AdopterRepository adopterRepository;
	private final ShelterRepository shelterRepository;
	private final PetRepository petRepository;

	/**
	 * Crea una nueva convivencia de prueba.
	 */
	@Transactional
	public TrialCohabitationEntity createTrial(TrialCohabitationEntity trial)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la convivencia de prueba");

		if (trial.getStartDate() == null)
			throw new IllegalOperationException("Trial start date cannot be null");

		AdopterEntity adopter = resolveAdopter(trial);
		ShelterEntity shelter = resolveShelter(trial);
		Long petId = resolvePetId(trial);
		validatePetNotAlreadyInTrial(trial, petId);

		Date today = new Date();
		if (trial.getStartDate().before(today))
			throw new IllegalOperationException("The trial start date cannot be before the current date");

		trial.setAdopter(adopter);
		trial.setShelter(shelter);
		if (trial.getStatus() == null || trial.getStatus().isBlank())
			trial.setStatus(PENDING_STATUS);

		log.info("Termina proceso de creación de la convivencia de prueba");
		return trialCohabitationRepository.save(trial);
	}

	private AdopterEntity resolveAdopter(TrialCohabitationEntity trial)
			throws EntityNotFoundException, IllegalOperationException {
		if (trial.getAdopter() == null || trial.getAdopter().getId() == null)
			throw new IllegalOperationException("Trial must be associated with an existing adopter");
		Optional<AdopterEntity> adopter = adopterRepository.findById(trial.getAdopter().getId());
		if (adopter.isEmpty())
			throw new EntityNotFoundException("Adopter not found");
		return adopter.get();
	}

	private ShelterEntity resolveShelter(TrialCohabitationEntity trial)
			throws EntityNotFoundException, IllegalOperationException {
		if (trial.getShelter() == null || trial.getShelter().getId() == null)
			throw new IllegalOperationException("Trial must be associated with an existing shelter");
		Optional<ShelterEntity> shelter = shelterRepository.findById(trial.getShelter().getId());
		if (shelter.isEmpty())
			throw new EntityNotFoundException("Shelter not found");
		return shelter.get();
	}

	private Long resolvePetId(TrialCohabitationEntity trial) throws EntityNotFoundException, IllegalOperationException {
		if (trial.getTrialCohabitationRequest() == null)
			throw new IllegalOperationException(
					"Trial must be associated with a cohabitation request that references an existing pet");

		Long petId = trial.getTrialCohabitationRequest().getPet() == null ? null
				: trial.getTrialCohabitationRequest().getPet().getId();
		if (petId == null || petRepository.findById(petId).isEmpty())
			throw new EntityNotFoundException("Pet not found");
		return petId;
	}

	private void validatePetNotAlreadyInTrial(TrialCohabitationEntity trial, Long petId)
			throws IllegalOperationException {
		boolean petAlreadyInTrial = trialCohabitationRepository.findAll().stream()
				.anyMatch(t -> (trial.getId() == null || !trial.getId().equals(t.getId()))
						&& t.getTrialCohabitationRequest() != null
						&& t.getTrialCohabitationRequest().getPet() != null
						&& petId.equals(t.getTrialCohabitationRequest().getPet().getId())
						&& t.getStatus() != null
						&& IN_PROGRESS_STATUS.equalsIgnoreCase(t.getStatus()));
		if (petAlreadyInTrial)
			throw new IllegalOperationException(
					"The requested pet is already involved in another in-progress trial cohabitation");
	}

	/**
	 * Obtiene todas las convivencias de prueba registradas.
	 */
	@Transactional
	public List<TrialCohabitationEntity> readAllTrials() {
		log.info("Inicia proceso de consultar todas las convivencias de prueba");
		List<TrialCohabitationEntity> trials = trialCohabitationRepository.findAll();
		if (trials.isEmpty())
			log.info("No hay convivencias de prueba registradas");
		return trials;
	}

	/**
	 * Obtiene las convivencias de prueba filtrando, de forma opcional, por estado
	 * y/o rango de fechas. Si se combinan varios filtros, todos deben coincidir.
	 */
	@Transactional
	public List<TrialCohabitationEntity> readAllTrials(String status, Date startDate, Date endDate)
			throws IllegalOperationException {
		log.info("Inicia proceso de consultar convivencias de prueba filtradas");
		if (status != null && status.isBlank())
			throw new IllegalOperationException("Search filters cannot be empty");

		List<TrialCohabitationEntity> trials = trialCohabitationRepository.findAll().stream()
				.filter(t -> status == null || (t.getStatus() != null && status.equalsIgnoreCase(t.getStatus())))
				.filter(t -> startDate == null || (t.getStartDate() != null && !t.getStartDate().before(startDate)))
				.filter(t -> endDate == null || (t.getStartDate() != null && !t.getStartDate().after(endDate)))
				.toList();
		if (trials.isEmpty())
			log.info("No hay convivencias de prueba registradas que cumplan los filtros");
		return trials;
	}

	/**
	 * Obtiene una convivencia de prueba a partir de su id.
	 */
	@Transactional
	public TrialCohabitationEntity readTrial(Long trialId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la convivencia de prueba con id = {}", trialId);
		if (trialId == null || trialId <= 0)
			throw new IllegalOperationException(TRIAL_ID_NOT_VALID);

		Optional<TrialCohabitationEntity> trial = trialCohabitationRepository.findById(trialId);
		if (trial.isEmpty())
			throw new EntityNotFoundException(TRIAL_NOT_FOUND);

		log.info("Termina proceso de consultar la convivencia de prueba con id = {}", trialId);
		return trial.get();
	}

	/**
	 * Actualiza una convivencia de prueba existente.
	 */
	@Transactional
	public TrialCohabitationEntity updateTrial(Long trialId, TrialCohabitationEntity trial)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la convivencia de prueba con id = {}", trialId);
		if (trialId == null || trialId <= 0)
			throw new IllegalOperationException(TRIAL_ID_NOT_VALID);

		Optional<TrialCohabitationEntity> existing = trialCohabitationRepository.findById(trialId);
		if (existing.isEmpty())
			throw new EntityNotFoundException(TRIAL_NOT_FOUND);

		if (trial.getStartDate() == null)
			throw new IllegalOperationException("Trial start date cannot be null");
		if (trial.getEndDate() == null)
			throw new IllegalOperationException("Trial end date cannot be null");
		if (trial.getStatus() == null || trial.getStatus().isBlank())
			throw new IllegalOperationException("Trial status cannot be null or empty");
		if (trial.getObservations() == null || trial.getObservations().isBlank())
			throw new IllegalOperationException("Trial observations cannot be null or empty");

		TrialCohabitationEntity current = existing.get();
		if (FINALIZED_STATUS.equalsIgnoreCase(current.getStatus())
				&& !FINALIZED_STATUS.equalsIgnoreCase(trial.getStatus()))
			throw new IllegalOperationException(
					"A finalized trial cohabitation cannot be changed back to an in-progress state");

		current.setStatus(trial.getStatus());
		current.setStartDate(trial.getStartDate());
		current.setEndDate(trial.getEndDate());
		current.setObservations(trial.getObservations());

		log.info("Termina proceso de actualizar la convivencia de prueba con id = {}", trialId);
		return trialCohabitationRepository.save(current);
	}

	/**
	 * Borra una convivencia de prueba a partir de su id. Únicamente se pueden
	 * eliminar aquellas en estado pendiente.
	 */
	@Transactional
	public void deleteTrial(Long trialId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar la convivencia de prueba con id = {}", trialId);
		if (trialId == null || trialId <= 0)
			throw new IllegalOperationException(TRIAL_ID_NOT_VALID);

		Optional<TrialCohabitationEntity> trial = trialCohabitationRepository.findById(trialId);
		if (trial.isEmpty())
			throw new EntityNotFoundException(TRIAL_NOT_FOUND);

		TrialCohabitationEntity current = trial.get();
		if (!PENDING_STATUS.equalsIgnoreCase(current.getStatus()))
			throw new IllegalOperationException(
					"Only trial cohabitations in pending status can be deleted; those in progress or finalized cannot");

		trialCohabitationRepository.deleteById(trialId);
		log.info("Termina proceso de borrar la convivencia de prueba con id = {}", trialId);
	}
}
package co.edu.udistrital.mdp.ZZZ.services;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.ZZZ.entities.AdopterEntity;
import co.edu.udistrital.mdp.ZZZ.entities.PetEntity;
import co.edu.udistrital.mdp.ZZZ.entities.TrialCohabitationEntity;
import co.edu.udistrital.mdp.ZZZ.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.ZZZ.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.ZZZ.repositories.AdopterRepository;
import co.edu.udistrital.mdp.ZZZ.repositories.PetRepository;
import co.edu.udistrital.mdp.ZZZ.repositories.TrialCohabitationRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TrialCohabitationService {

	private static final String PENDING_STATUS = "PENDING";
	private static final String IN_PROGRESS_STATUS = "IN_PROGRESS";
	private static final String FINALIZED_STATUS = "FINALIZED";

	@Autowired
	TrialCohabitationRepository trialCohabitationRepository;

	@Autowired
	AdopterRepository adopterRepository;

	@Autowired
	PetRepository petRepository;

	/**
	 * Crea una nueva convivencia de prueba. La mascota y el adoptante deben
	 * existir y estar activos, la mascota no puede estar involucrada en otra
	 * convivencia en curso y la fecha de inicio no puede ser anterior a la fecha
	 * actual.
	 */
	@Transactional
	public TrialCohabitationEntity createTrialCohabitation(TrialCohabitationEntity trial)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la convivencia de prueba");

		if (trial.getStartDate() == null)
			throw new IllegalOperationException("Start date cannot be null");
		if (trial.getStartDate().before(todayStart()))
			throw new IllegalOperationException("The start date cannot be before the current date");

		PetEntity pet = resolvePet(trial);
		if (pet == null)
			throw new IllegalOperationException(
					"Trial cohabitation must be associated with an existing pet through a request");
		Optional<PetEntity> petEntity = petRepository.findById(pet.getId());
		if (petEntity.isEmpty())
			throw new EntityNotFoundException("Pet not found");
		final PetEntity persistedPet = petEntity.get();
		if (!isPetActive(persistedPet))
			throw new IllegalOperationException("The pet must be active to participate in a trial cohabitation");

		AdopterEntity adopter = resolveAdopter(trial);
		if (adopter == null)
			throw new IllegalOperationException("Trial cohabitation must be associated with an existing adopter");
		if (adopterRepository.findById(adopter.getId()).isEmpty())
			throw new EntityNotFoundException("Adopter not found");

		boolean petInOngoingTrial = trialCohabitationRepository.findAll().stream()
				.filter(t -> IN_PROGRESS_STATUS.equalsIgnoreCase(t.getStatus()))
				.anyMatch(t -> {
					PetEntity otherPet = resolvePet(t);
					return otherPet != null && otherPet.getId().equals(persistedPet.getId());
				});
		if (petInOngoingTrial)
			throw new IllegalOperationException("The pet is already involved in an ongoing trial cohabitation");

		if (trial.getStatus() == null)
			trial.setStatus(PENDING_STATUS);

		log.info("Termina proceso de creación de la convivencia de prueba");
		return trialCohabitationRepository.save(trial);
	}

	/**
	 * Obtiene una convivencia de prueba a partir de su id.
	 */
	@Transactional
	public TrialCohabitationEntity getTrialCohabitation(Long trialId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la convivencia de prueba con id = {}", trialId);
		if (trialId == null || trialId <= 0)
			throw new IllegalOperationException("Trial cohabitation id is not valid");

		Optional<TrialCohabitationEntity> trial = trialCohabitationRepository.findById(trialId);
		if (trial.isEmpty())
			throw new EntityNotFoundException("Trial cohabitation not found");

		log.info("Termina proceso de consultar la convivencia de prueba con id = {}", trialId);
		return trial.get();
	}

	/**
	 * Obtiene todas las convivencias de prueba registradas.
	 */
	@Transactional
	public List<TrialCohabitationEntity> getTrials() {
		log.info("Inicia proceso de consultar todas las convivencias de prueba");
		List<TrialCohabitationEntity> trials = trialCohabitationRepository.findAll();
		if (trials.isEmpty())
			log.info("No hay convivencias de prueba registradas");
		return trials;
	}

	/**
	 * Obtiene las convivencias de prueba filtrando, de forma opcional, por estado
	 * y rango de fechas. Si se combinan varios filtros, todos deben coincidir.
	 */
	@Transactional
	public List<TrialCohabitationEntity> getTrials(String status, Date startDate, Date endDate) {
		log.info("Inicia proceso de consultar convivencias de prueba por filtros");
		List<TrialCohabitationEntity> trials = trialCohabitationRepository.findAll().stream()
				.filter(t -> status == null || (t.getStatus() != null && t.getStatus().equalsIgnoreCase(status)))
				.filter(t -> startDate == null || (t.getStartDate() != null && !t.getStartDate().before(startDate)))
				.filter(t -> endDate == null || (t.getEndDate() != null && !t.getEndDate().after(endDate)))
				.toList();
		if (trials.isEmpty())
			log.info("No hay convivencias de prueba registradas que cumplan los filtros");
		return trials;
	}

	/**
	 * Actualiza una convivencia de prueba existente. Una convivencia finalizada no
	 * puede volver a un estado anterior.
	 */
	@Transactional
	public TrialCohabitationEntity updateTrialCohabitation(Long trialId, TrialCohabitationEntity trial)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la convivencia de prueba con id = {}", trialId);
		if (trialId == null || trialId <= 0)
			throw new IllegalOperationException("Trial cohabitation id is not valid");

		Optional<TrialCohabitationEntity> existing = trialCohabitationRepository.findById(trialId);
		if (existing.isEmpty())
			throw new EntityNotFoundException("Trial cohabitation not found");

		if (trial.getStartDate() == null)
			throw new IllegalOperationException("Start date cannot be null");
		if (trial.getEndDate() == null)
			throw new IllegalOperationException("End date cannot be null");
		if (trial.getStatus() == null || trial.getStatus().isBlank())
			throw new IllegalOperationException("Status cannot be null or empty");

		TrialCohabitationEntity current = existing.get();
		if (FINALIZED_STATUS.equalsIgnoreCase(current.getStatus())
				&& !FINALIZED_STATUS.equalsIgnoreCase(trial.getStatus()))
			throw new IllegalOperationException(
					"A finalized trial cohabitation cannot be changed back to another state");

		current.setStartDate(trial.getStartDate());
		current.setEndDate(trial.getEndDate());
		current.setStatus(trial.getStatus());
		current.setObservations(trial.getObservations());

		log.info("Termina proceso de actualizar la convivencia de prueba con id = {}", trialId);
		return trialCohabitationRepository.save(current);
	}

	/**
	 * Borra una convivencia de prueba a partir de su id. Solo se pueden eliminar
	 * aquellas que se encuentran en estado pendiente.
	 */
	@Transactional
	public void deleteTrialCohabitation(Long trialId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar la convivencia de prueba con id = {}", trialId);
		if (trialId == null || trialId <= 0)
			throw new IllegalOperationException("Trial cohabitation id is not valid");

		Optional<TrialCohabitationEntity> trial = trialCohabitationRepository.findById(trialId);
		if (trial.isEmpty())
			throw new EntityNotFoundException("Trial cohabitation not found");

		TrialCohabitationEntity trialEntity = trial.get();
		if (IN_PROGRESS_STATUS.equalsIgnoreCase(trialEntity.getStatus())
				|| FINALIZED_STATUS.equalsIgnoreCase(trialEntity.getStatus()))
			throw new IllegalOperationException(
					"Only trial cohabitations in pending state can be deleted; an ongoing or finalized one must be kept");

		trialCohabitationRepository.deleteById(trialId);
		log.info("Termina proceso de borrar la convivencia de prueba con id = {}", trialId);
	}

	private PetEntity resolvePet(TrialCohabitationEntity trial) {
		if (trial.getTrialCohabitationRequest() != null && trial.getTrialCohabitationRequest().getPet() != null)
			return trial.getTrialCohabitationRequest().getPet();
		if (trial.getAdoptionRequest() != null && trial.getAdoptionRequest().getPet() != null)
			return trial.getAdoptionRequest().getPet();
		return null;
	}

	private AdopterEntity resolveAdopter(TrialCohabitationEntity trial) {
		if (trial.getAdopter() != null)
			return trial.getAdopter();
		if (trial.getTrialCohabitationRequest() != null)
			return trial.getTrialCohabitationRequest().getAdopter();
		if (trial.getAdoptionRequest() != null)
			return trial.getAdoptionRequest().getAdopter();
		return null;
	}

	private boolean isPetActive(PetEntity pet) {
		if (pet.getAdoptions() == null)
			return true;
		return pet.getAdoptions().stream()
				.noneMatch(adoption -> FINALIZED_STATUS.equalsIgnoreCase(adoption.getStatus()));
	}

	private Date todayStart() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}
}
package co.edu.udistrital.mdp.pets.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.BaseEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationRequestEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdopterRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.ShelterRepository;
import co.edu.udistrital.mdp.pets.repositories.TrialCohabitationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrialCohabitationRequestService {

	public static final String PENDING_STATUS = "PENDING";
	public static final String APPROVED_STATUS = "APPROVED";
	public static final String REJECTED_STATUS = "REJECTED";
	public static final String CANCELLED_STATUS = "CANCELLED";

	private static final Set<String> FINALIZED_STATUSES = Set.of(APPROVED_STATUS, REJECTED_STATUS, CANCELLED_STATUS);

	private static final String REQUEST_ID_NOT_VALID = "Trial cohabitation request id is not valid";
	private static final String REQUEST_NOT_VALID = "Trial cohabitation request is not valid";
	private static final String REQUEST_NOT_FOUND = "The trial cohabitation request with the given id was not found";
	private static final String STATUS_NOT_VALID = "Trial cohabitation request status cannot be null or empty";
	private static final String DATE_NOT_VALID = "Trial cohabitation request date cannot be null or empty";
	private static final String DESCRIPTION_NOT_VALID =
			"Trial cohabitation request description cannot be null or empty";
	private static final String ADOPTER_ID_NOT_VALID = "Adopter id is not valid";
	private static final String PET_ID_NOT_VALID = "Pet id is not valid";
	private static final String ADOPTER_NOT_ACTIVE = "The adopter is not active in the system";
	private static final String PET_NOT_ACTIVE = "The pet is not active in the system";
	private static final String PET_NOT_AVAILABLE = "The pet is not available for trial cohabitation";
	private static final String DUPLICATE_REQUEST =
			"The adopter already has an active trial cohabitation request for this pet";
	private static final String PERIOD_NOT_DEFINED =
			"The trial cohabitation period must be defined with a start date and an end date";
	private static final String START_DATE_IN_PAST = "The start date cannot be earlier than the current date";
	private static final String END_DATE_BEFORE_START = "The end date cannot be earlier than the start date";
	private static final String ADOPTER_PET_CANNOT_BE_MODIFIED =
			"The adopter and the pet of a trial cohabitation request cannot be modified";
	private static final String FINALIZED_REQUEST_CANNOT_BE_MODIFIED =
			"A finalized request (approved, rejected or cancelled) cannot be modified";
	private static final String IN_PROGRESS_REQUEST_CANNOT_BE_DELETED = "A request in progress cannot be deleted";
	private static final String SHELTER_LABEL = "Shelter";
	private static final String ADOPTER_LABEL = "Adopter";
	private static final String PET_LABEL = "Pet";

	private final TrialCohabitationRequestRepository trialCohabitationRequestRepository;
	private final PetRepository petRepository;
	private final ShelterRepository shelterRepository;
	private final AdopterRepository adopterRepository;

	/**
	 * Crea una nueva solicitud de convivencia de prueba.
	 *
	 * Reglas de negocio:
	 * 1. Ningún atributo obligatorio puede ser nulo o vacío.
	 * 2. El refugio, el adoptante y la mascota deben existir.
	 * 3. El adoptante y la mascota deben estar activos.
	 * 4. El adoptante no puede tener otra solicitud pendiente para la misma mascota.
	 * 5. La mascota no puede estar adoptada ni en otra convivencia pendiente.
	 * 6. Debe definirse un periodo válido: el inicio no puede ser anterior a hoy y el fin no puede
	 *    ser anterior al inicio.
	 */
	@Transactional
	public TrialCohabitationRequestEntity createTrialCohabitationRequest(TrialCohabitationRequestEntity request)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la solicitud de convivencia de prueba");

		validateRequest(request);
		if (request.getDate() == null)
			throw new IllegalOperationException(DATE_NOT_VALID);
		validatePeriod(request.getStartDate(), request.getEndDate());

		ShelterEntity shelter = findReference(request.getShelter(), shelterRepository, SHELTER_LABEL);
		AdopterEntity adopter = findReference(request.getAdopter(), adopterRepository, ADOPTER_LABEL);
		PetEntity pet = findReference(request.getPet(), petRepository, PET_LABEL);

		if (!isActive(adopter.getActive()))
			throw new IllegalOperationException(ADOPTER_NOT_ACTIVE);
		if (!isActive(pet.getActive()))
			throw new IllegalOperationException(PET_NOT_ACTIVE);
		if (hasPendingRequestForPet(adopter, pet.getId()))
			throw new IllegalOperationException(DUPLICATE_REQUEST);
		if (isAdopted(pet) || hasPendingRequestForPetFromAnyAdopter(pet.getId()))
			throw new IllegalOperationException(PET_NOT_AVAILABLE);

		request.setShelter(shelter);
		request.setAdopter(adopter);
		request.setPet(pet);

		log.info("Termina proceso de creación de la solicitud de convivencia de prueba");
		return trialCohabitationRequestRepository.save(request);
	}

	@Transactional(readOnly = true)
	public List<TrialCohabitationRequestEntity> getTrialCohabitationRequests() {
		log.info("Inicia proceso de consultar todas las solicitudes de convivencia de prueba");
		return trialCohabitationRequestRepository.findAll();
	}

	/** Consulta con filtros opcionales por adoptante, mascota y estado. */
	@Transactional(readOnly = true)
	public List<TrialCohabitationRequestEntity> getTrialCohabitationRequests(Long adopterId, Long petId,
			String status) throws IllegalOperationException {
		log.info("Inicia proceso de consultar las solicitudes de convivencia de prueba con filtros");
		if (adopterId != null && adopterId < 0)
			throw new IllegalOperationException(ADOPTER_ID_NOT_VALID);
		if (petId != null && petId < 0)
			throw new IllegalOperationException(PET_ID_NOT_VALID);
		return trialCohabitationRequestRepository.findAll().stream()
				.filter(r -> adopterId == null || (r.getAdopter() != null && adopterId.equals(r.getAdopter().getId())))
				.filter(r -> petId == null || (r.getPet() != null && petId.equals(r.getPet().getId())))
				.filter(r -> status == null || status.equalsIgnoreCase(r.getStatus()))
				.toList();
	}

	@Transactional(readOnly = true)
	public TrialCohabitationRequestEntity getTrialCohabitationRequest(Long requestId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la solicitud de convivencia de prueba con id = {}", requestId);
		TrialCohabitationRequestEntity request = findRequest(requestId);
		log.info("Termina proceso de consultar la solicitud de convivencia de prueba con id = {}", requestId);
		return request;
	}

	/**
	 * Actualiza una solicitud de convivencia de prueba.
	 *
	 * Reglas de negocio:
	 * 1. No se aceptan identificadores inválidos ni atributos nulos.
	 * 2. El adoptante y la mascota no pueden modificarse tras la creación.
	 * 3. No se puede modificar una solicitud que ya generó una convivencia de prueba, ni una que
	 *    esté finalizada (aprobada, rechazada o cancelada).
	 * 4. Si se envían startDate o endDate, el periodo resultante debe ser válido.
	 */
	@Transactional
	public TrialCohabitationRequestEntity updateTrialCohabitationRequest(Long requestId,
			TrialCohabitationRequestEntity requestUpdate) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la solicitud de convivencia de prueba con id = {}", requestId);

		TrialCohabitationRequestEntity current = findRequest(requestId);
		validateRequest(requestUpdate);

		if (current.getTrialCohabitation() != null || isFinalized(current))
			throw new IllegalOperationException(FINALIZED_REQUEST_CANNOT_BE_MODIFIED);

		if (requestUpdate.getAdopter() != null
				&& !current.getAdopter().getId().equals(requestUpdate.getAdopter().getId()))
			throw new IllegalOperationException(ADOPTER_PET_CANNOT_BE_MODIFIED);

		if (requestUpdate.getPet() != null && !current.getPet().getId().equals(requestUpdate.getPet().getId()))
			throw new IllegalOperationException(ADOPTER_PET_CANNOT_BE_MODIFIED);

		Date startDate = requestUpdate.getStartDate() != null ? requestUpdate.getStartDate() : current.getStartDate();
		Date endDate = requestUpdate.getEndDate() != null ? requestUpdate.getEndDate() : current.getEndDate();
		if (startDate != null && endDate != null)
			validateEndDate(startDate, endDate);

		current.setStatus(requestUpdate.getStatus());
		current.setDescription(requestUpdate.getDescription());
		current.setStartDate(startDate);
		current.setEndDate(endDate);
		if (requestUpdate.getDate() != null)
			current.setDate(requestUpdate.getDate());

		log.info("Termina proceso de actualizar la solicitud de convivencia de prueba con id = {}", requestId);
		return trialCohabitationRequestRepository.save(current);
	}

	/**
	 * Elimina una solicitud de convivencia de prueba.
	 *
	 * Reglas de negocio:
	 * 1. No se aceptan identificadores inválidos.
	 * 2. Si el identificador no existe, se lanza una excepción.
	 * 3. No se puede eliminar una solicitud que ya generó una convivencia de prueba.
	 * 4. No se puede eliminar una solicitud en curso (status = "PENDING").
	 */
	@Transactional
	public void deleteTrialCohabitationRequest(Long requestId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar la solicitud de convivencia de prueba con id = {}", requestId);

		TrialCohabitationRequestEntity current = findRequest(requestId);

		if (current.getTrialCohabitation() != null)
			throw new IllegalOperationException(
					"Unable to delete request because it already generated a trial cohabitation");

		if (PENDING_STATUS.equals(current.getStatus()))
			throw new IllegalOperationException(IN_PROGRESS_REQUEST_CANNOT_BE_DELETED);

		trialCohabitationRequestRepository.delete(current);
		log.info("Termina proceso de borrar la solicitud de convivencia de prueba con id = {}", requestId);
	}

	private TrialCohabitationRequestEntity findRequest(Long requestId)
			throws EntityNotFoundException, IllegalOperationException {
		if (requestId == null || requestId < 0)
			throw new IllegalOperationException(REQUEST_ID_NOT_VALID);
		return trialCohabitationRequestRepository.findById(requestId)
				.orElseThrow(() -> new EntityNotFoundException(REQUEST_NOT_FOUND));
	}

	/** Reglas de datos comunes a la creación y a la actualización. */
	private void validateRequest(TrialCohabitationRequestEntity request) throws IllegalOperationException {
		if (request == null)
			throw new IllegalOperationException(REQUEST_NOT_VALID);
		if (request.getStatus() == null || request.getStatus().isBlank())
			throw new IllegalOperationException(STATUS_NOT_VALID);
		if (request.getDescription() == null || request.getDescription().isBlank())
			throw new IllegalOperationException(DESCRIPTION_NOT_VALID);
	}

	private boolean isFinalized(TrialCohabitationRequestEntity request) {
		return FINALIZED_STATUSES.contains(request.getStatus());
	}

	/**
	 * Valida el periodo de convivencia al crear la solicitud: ambas fechas deben estar definidas,
	 * el inicio no puede ser anterior a hoy (hoy sí es válido) y el fin no puede ser anterior al inicio.
	 */
	private void validatePeriod(Date startDate, Date endDate) throws IllegalOperationException {
		if (startDate == null || endDate == null)
			throw new IllegalOperationException(PERIOD_NOT_DEFINED);
		if (toLocalDate(startDate).isBefore(LocalDate.now()))
			throw new IllegalOperationException(START_DATE_IN_PAST);
		validateEndDate(startDate, endDate);
	}

	private void validateEndDate(Date startDate, Date endDate) throws IllegalOperationException {
		if (toLocalDate(endDate).isBefore(toLocalDate(startDate)))
			throw new IllegalOperationException(END_DATE_BEFORE_START);
	}

	/** Se usa getTime() porque las fechas leídas de la base de datos pueden ser java.sql.Date. */
	private LocalDate toLocalDate(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}

	/**
	 * Resuelve una entidad relacionada: la referencia y su id son obligatorios y debe existir.
	 * El mensaje de "no encontrado" sigue el formato "The {label} with the given id was not found".
	 */
	private <T extends BaseEntity> T findReference(T reference, JpaRepository<T, Long> repository, String label)
			throws EntityNotFoundException, IllegalOperationException {
		if (reference == null || reference.getId() == null)
			throw new IllegalOperationException(label + " is not valid");
		return repository.findById(reference.getId()).orElseThrow(
				() -> new EntityNotFoundException("The " + label.toLowerCase() + " with the given id was not found"));
	}

	private boolean isActive(Boolean active) {
		return !Boolean.FALSE.equals(active);
	}

	/** Una mascota adoptada tiene una adopción que no fue cancelada. */
	private boolean isAdopted(PetEntity pet) {
		return pet.getAdoptions() != null
				&& pet.getAdoptions().stream().anyMatch(a -> !CANCELLED_STATUS.equalsIgnoreCase(a.getStatus()));
	}

	private boolean hasPendingRequestForPet(AdopterEntity adopter, Long petId) {
		return adopter.getCohabitationRequests() != null && adopter.getCohabitationRequests().stream()
				.anyMatch(r -> r.getPet() != null && petId.equals(r.getPet().getId())
						&& PENDING_STATUS.equals(r.getStatus()));
	}

	private boolean hasPendingRequestForPetFromAnyAdopter(Long petId) {
		return trialCohabitationRequestRepository.findAll().stream()
				.anyMatch(r -> r.getPet() != null && petId.equals(r.getPet().getId())
						&& PENDING_STATUS.equals(r.getStatus()));
	}
}

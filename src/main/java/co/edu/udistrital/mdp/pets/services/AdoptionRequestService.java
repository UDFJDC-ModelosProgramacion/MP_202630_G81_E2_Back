package co.edu.udistrital.mdp.pets.services;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdopterRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRequestRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdoptionRequestService {

	public static final String PENDING_STATUS = "PENDING";
	public static final String APPROVED_STATUS = "APPROVED";
	public static final String REJECTED_STATUS = "REJECTED";
	public static final String IN_PROGRESS_STATUS = "IN_PROGRESS";
	public static final String FINALIZED_STATUS = "FINALIZED";

	private static final String ADMIN_ROLE = "ADMIN";
	private static final String ADOPTION_CANCELLED_STATUS = "CANCELLED";

	private static final Set<String> VALID_STATUSES = Set.of(PENDING_STATUS, APPROVED_STATUS, REJECTED_STATUS,
			IN_PROGRESS_STATUS, FINALIZED_STATUS);

	// Solicitudes "activas": las mismas que AdopterService usa para impedir eliminar al adoptante
	private static final Set<String> ACTIVE_STATUSES = Set.of(PENDING_STATUS, APPROVED_STATUS, IN_PROGRESS_STATUS);

	// Solicitudes ya procesadas: no se pueden devolver a pendiente ni eliminar
	private static final Set<String> PROCESSED_STATUSES = Set.of(APPROVED_STATUS, REJECTED_STATUS,
			IN_PROGRESS_STATUS, FINALIZED_STATUS);

	private static final String REQUEST_ID_NOT_VALID = "Invalid identifiers are not accepted";
	private static final String REQUEST_NOT_FOUND = "Adoption request not found";

	private final AdoptionRequestRepository adoptionRequestRepository;
	private final AdopterRepository adopterRepository;
	private final PetRepository petRepository;
	private final AdoptionRepository adoptionRepository;

	/**
	 * Crea una solicitud de adopción. El estado inicial (PENDING), la fecha y el
	 * refugio (el de la mascota) los asigna el sistema.
	 */
	@Transactional
	public AdoptionRequestEntity createAdoptionRequest(AdoptionRequestEntity request)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la solicitud de adopción");

		if (request == null)
			throw new IllegalOperationException("Adoption request cannot be null");
		if (request.getAdopter() == null || request.getAdopter().getId() == null)
			throw new IllegalOperationException("Adoption request must be associated with an existing adopter");
		if (request.getPet() == null || request.getPet().getId() == null)
			throw new IllegalOperationException("Adoption request must be associated with an existing pet");
		if (request.getDescription() == null || request.getDescription().isBlank())
			throw new IllegalOperationException("Description cannot be null or empty");

		AdopterEntity adopter = adopterRepository.findById(request.getAdopter().getId())
				.orElseThrow(() -> new EntityNotFoundException("Adopter not found"));
		PetEntity pet = petRepository.findById(request.getPet().getId())
				.orElseThrow(() -> new EntityNotFoundException("Pet not found"));

		if (!isPetAvailable(pet.getId()))
			throw new IllegalOperationException("The requested pet is not available for adoption");

		if (hasActiveRequest(adopter.getId(), pet.getId()))
			throw new IllegalOperationException("A user cannot have more than one active request for the same pet");

		request.setId(null);
		request.setAdopter(adopter);
		request.setPet(pet);
		request.setShelter(pet.getShelter());
		request.setStatus(PENDING_STATUS);
		request.setDate(new Date());

		log.info("Termina proceso de creación de la solicitud de adopción");
		return adoptionRequestRepository.save(request);
	}

	private boolean hasActiveRequest(Long adopterId, Long petId) {
		return ACTIVE_STATUSES.stream().anyMatch(
				status -> !adoptionRequestRepository.findByAdopterIdAndPetIdAndStatus(adopterId, petId, status)
						.isEmpty());
	}

	/**
	 * Una mascota está disponible mientras no tenga una adopción que no haya sido
	 * cancelada.
	 */
	private boolean isPetAvailable(Long petId) {
		return adoptionRepository.findAll().stream()
				.noneMatch(a -> a.getPet() != null && petId.equals(a.getPet().getId())
						&& !ADOPTION_CANCELLED_STATUS.equalsIgnoreCase(a.getStatus()));
	}

	/**
	 * Obtiene una solicitud. Solo puede verla quien la creó o un administrador.
	 */
	@Transactional(readOnly = true)
	public AdoptionRequestEntity readAdoptionRequest(Long id, Long currentUserId, String currentUserRole)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la solicitud de adopción con id = {}", id);

		if (id == null || id <= 0)
			throw new IllegalOperationException(REQUEST_ID_NOT_VALID);

		AdoptionRequestEntity request = adoptionRequestRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException(REQUEST_NOT_FOUND));

		if (!ADMIN_ROLE.equalsIgnoreCase(currentUserRole) && !isCreator(request, currentUserId))
			throw new IllegalOperationException("Only the user who created the request and administrators can view it");

		log.info("Termina proceso de consultar la solicitud de adopción con id = {}", id);
		return request;
	}

	/**
	 * Lista las solicitudes filtrando, de forma opcional, por adoptante, mascota y
	 * estado. Si se combinan varios filtros, todos deben coincidir. Un usuario que
	 * no es administrador solo ve sus propias solicitudes.
	 */
	@Transactional(readOnly = true)
	public List<AdoptionRequestEntity> readAllAdoptionRequests(Long adopterId, Long petId, String status,
			Long currentUserId, String currentUserRole) throws IllegalOperationException {
		log.info("Inicia proceso de consultar las solicitudes de adopción");

		if ((adopterId != null && adopterId <= 0) || (petId != null && petId <= 0))
			throw new IllegalOperationException(REQUEST_ID_NOT_VALID);
		if (status != null && status.isBlank())
			throw new IllegalOperationException("Search filters cannot be empty");

		boolean isAdmin = ADMIN_ROLE.equalsIgnoreCase(currentUserRole);

		List<AdoptionRequestEntity> requests = adoptionRequestRepository.findAll().stream()
				.filter(r -> isAdmin || isCreator(r, currentUserId))
				.filter(r -> adopterId == null || (r.getAdopter() != null && adopterId.equals(r.getAdopter().getId())))
				.filter(r -> petId == null || (r.getPet() != null && petId.equals(r.getPet().getId())))
				.filter(r -> status == null || status.equalsIgnoreCase(r.getStatus()))
				.toList();

		if (requests.isEmpty())
			log.info("No hay solicitudes de adopción que cumplan los filtros");
		return requests;
	}

	/**
	 * Actualiza la descripción y el estado de una solicitud. La mascota no se puede
	 * modificar y una solicitud ya procesada no puede volver a pendiente.
	 */
	@Transactional
	public AdoptionRequestEntity updateAdoptionRequest(Long id, AdoptionRequestEntity requestUpdate)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la solicitud de adopción con id = {}", id);

		if (id == null || id <= 0)
			throw new IllegalOperationException(REQUEST_ID_NOT_VALID);
		if (requestUpdate == null || requestUpdate.getDescription() == null || requestUpdate.getDescription().isBlank()
				|| requestUpdate.getStatus() == null || requestUpdate.getStatus().isBlank())
			throw new IllegalOperationException("Description and status cannot be null or empty");

		AdoptionRequestEntity current = adoptionRequestRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException(REQUEST_NOT_FOUND));

		if (requestUpdate.getPet() != null && requestUpdate.getPet().getId() != null
				&& !requestUpdate.getPet().getId().equals(current.getPet().getId()))
			throw new IllegalOperationException("The pet associated with the request cannot be modified");

		String newStatus = normalizeStatus(requestUpdate.getStatus());
		if (PENDING_STATUS.equals(newStatus) && current.getStatus() != null
				&& PROCESSED_STATUSES.contains(current.getStatus().toUpperCase(Locale.ROOT)))
			throw new IllegalOperationException("A processed request cannot be reverted to pending");

		current.setDescription(requestUpdate.getDescription());
		current.setStatus(newStatus);

		log.info("Termina proceso de actualizar la solicitud de adopción con id = {}", id);
		return adoptionRequestRepository.save(current);
	}

	/**
	 * Elimina una solicitud. Las solicitudes aprobadas, rechazadas o ya en proceso
	 * o finalizadas no se pueden eliminar.
	 */
	@Transactional
	public void deleteAdoptionRequest(Long id) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de eliminar la solicitud de adopción con id = {}", id);

		if (id == null || id <= 0)
			throw new IllegalOperationException(REQUEST_ID_NOT_VALID);

		AdoptionRequestEntity current = adoptionRequestRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException(REQUEST_NOT_FOUND));

		if (current.getStatus() != null && PROCESSED_STATUSES.contains(current.getStatus().toUpperCase(Locale.ROOT)))
			throw new IllegalOperationException("A request that has already been processed cannot be deleted");

		adoptionRequestRepository.delete(current);
		log.info("Termina proceso de eliminar la solicitud de adopción con id = {}", id);
	}

	private boolean isCreator(AdoptionRequestEntity request, Long userId) {
		return userId != null && request.getAdopter() != null && userId.equals(request.getAdopter().getId());
	}

	private String normalizeStatus(String status) throws IllegalOperationException {
		String normalized = status.trim().toUpperCase(Locale.ROOT);
		if (!VALID_STATUSES.contains(normalized))
			throw new IllegalOperationException("Invalid request status");
		return normalized;
	}
}
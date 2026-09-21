package co.edu.udistrital.mdp.pets.services;

import java.util.List;

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

	private static final String REQUEST_ID_NOT_VALID = "Trial cohabitation request id is not valid";
	private static final String REQUEST_NOT_VALID = "Trial cohabitation request is not valid";
	private static final String REQUEST_NOT_FOUND = "Trial cohabitation request was not found";
	private static final String STATUS_NOT_VALID = "Status is not valid";
	private static final String DATE_NOT_VALID = "Date is not valid";
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
	 * 3. El adoptante no puede tener otra solicitud de convivencia activa (status = "PENDING")
	 *    para la misma mascota.
	 */
	@Transactional
	public TrialCohabitationRequestEntity createTrialCohabitationRequest(TrialCohabitationRequestEntity request)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la solicitud de convivencia de prueba");

		validateRequest(request);
		if (request.getDate() == null)
			throw new IllegalOperationException(DATE_NOT_VALID);

		ShelterEntity shelter = findReference(request.getShelter(), shelterRepository, SHELTER_LABEL);
		AdopterEntity adopter = findReference(request.getAdopter(), adopterRepository, ADOPTER_LABEL);
		PetEntity pet = findReference(request.getPet(), petRepository, PET_LABEL);

		if (hasPendingRequestForPet(adopter, pet.getId()))
			throw new IllegalOperationException(
					"Unable to create request because the adopter already has a pending request for this pet");

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
	 * 2. El refugio, el adoptante y la mascota no pueden modificarse tras la creación.
	 * 3. No se puede modificar una solicitud que ya generó una convivencia de prueba (trialCohabitation != null).
	 */
	@Transactional
	public TrialCohabitationRequestEntity updateTrialCohabitationRequest(Long requestId,
			TrialCohabitationRequestEntity requestUpdate) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la solicitud de convivencia de prueba con id = {}", requestId);

		TrialCohabitationRequestEntity current = findRequest(requestId);
		validateRequest(requestUpdate);

		if (current.getTrialCohabitation() != null)
			throw new IllegalOperationException(
					"Unable to update request because it already generated a trial cohabitation");

		current.setStatus(requestUpdate.getStatus());
		current.setDescription(requestUpdate.getDescription());
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
	 */
	@Transactional
	public void deleteTrialCohabitationRequest(Long requestId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar la solicitud de convivencia de prueba con id = {}", requestId);

		TrialCohabitationRequestEntity current = findRequest(requestId);

		if (current.getTrialCohabitation() != null)
			throw new IllegalOperationException(
					"Unable to delete request because it already generated a trial cohabitation");

		trialCohabitationRequestRepository.delete(current);
		log.info("Termina proceso de borrar la solicitud de convivencia de prueba con id = {}", requestId);
	}

	private TrialCohabitationRequestEntity findRequest(Long requestId)
			throws EntityNotFoundException, IllegalOperationException {
		if (requestId == null || requestId <= 0)
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
	}

	/** Resuelve una entidad relacionada: la referencia y su id son obligatorios y debe existir. */
	private <T extends BaseEntity> T findReference(T reference, JpaRepository<T, Long> repository, String label)
			throws EntityNotFoundException, IllegalOperationException {
		if (reference == null || reference.getId() == null)
			throw new IllegalOperationException(label + " is not valid");
		return repository.findById(reference.getId())
				.orElseThrow(() -> new EntityNotFoundException(label + " was not found"));
	}

	private boolean hasPendingRequestForPet(AdopterEntity adopter, Long petId) {
		return adopter.getCohabitationRequests() != null && adopter.getCohabitationRequests().stream()
				.anyMatch(r -> r.getPet() != null && petId.equals(r.getPet().getId())
						&& PENDING_STATUS.equals(r.getStatus()));
	}
}
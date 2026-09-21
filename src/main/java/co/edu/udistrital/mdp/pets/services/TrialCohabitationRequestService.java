package co.edu.udistrital.mdp.pets.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationRequestEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdopterRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.ShelterRepository;
import co.edu.udistrital.mdp.pets.repositories.TrialCohabitationRequestRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TrialCohabitationRequestService {

	@Autowired
	private TrialCohabitationRequestRepository trialCohabitationRequestRepository;

	@Autowired
	private PetRepository petRepository;

	@Autowired
	private ShelterRepository shelterRepository;

	@Autowired
	private AdopterRepository adopterRepository;

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
	public TrialCohabitationRequestEntity createTrialCohabitationRequest(
			TrialCohabitationRequestEntity requestEntity) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la solicitud de convivencia de prueba");

		if (requestEntity.getStatus() == null || requestEntity.getStatus().isBlank())
			throw new IllegalOperationException("Status is not valid");

		if (requestEntity.getDate() == null)
			throw new IllegalOperationException("Date is not valid");

		if (requestEntity.getShelter() == null)
			throw new IllegalOperationException("Shelter is not valid");

		Optional<ShelterEntity> shelter = shelterRepository.findById(requestEntity.getShelter().getId());
		if (shelter.isEmpty())
			throw new EntityNotFoundException("Shelter was not found");

		if (requestEntity.getAdopter() == null)
			throw new IllegalOperationException("Adopter is not valid");

		Optional<AdopterEntity> adopter = adopterRepository.findById(requestEntity.getAdopter().getId());
		if (adopter.isEmpty())
			throw new EntityNotFoundException("Adopter was not found");

		if (requestEntity.getPet() == null)
			throw new IllegalOperationException("Pet is not valid");

		Optional<PetEntity> pet = petRepository.findById(requestEntity.getPet().getId());
		if (pet.isEmpty())
			throw new EntityNotFoundException("Pet was not found");

		boolean duplicated = adopter.get().getCohabitationRequests() != null
				&& adopter.get().getCohabitationRequests().stream()
						.anyMatch(r -> r.getPet() != null && r.getPet().getId().equals(pet.get().getId())
								&& "PENDING".equals(r.getStatus()));
		if (duplicated)
			throw new IllegalOperationException(
					"Unable to create request because the adopter already has a pending request for this pet");

		requestEntity.setShelter(shelter.get());
		requestEntity.setAdopter(adopter.get());
		requestEntity.setPet(pet.get());

		log.info("Termina proceso de creación de la solicitud de convivencia de prueba");
		return trialCohabitationRequestRepository.save(requestEntity);
	}

	@Transactional
	public List<TrialCohabitationRequestEntity> getTrialCohabitationRequests() {
		log.info("Inicia proceso de consultar todas las solicitudes de convivencia de prueba");
		return trialCohabitationRequestRepository.findAll();
	}

	@Transactional
	public TrialCohabitationRequestEntity getTrialCohabitationRequest(Long requestId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la solicitud de convivencia de prueba con id = {0}", requestId);
		if (requestId == null || requestId <= 0)
			throw new IllegalOperationException("Trial cohabitation request id is not valid");
		Optional<TrialCohabitationRequestEntity> requestEntity =
				trialCohabitationRequestRepository.findById(requestId);
		if (requestEntity.isEmpty())
			throw new EntityNotFoundException("Trial cohabitation request was not found");
		log.info("Termina proceso de consultar la solicitud de convivencia de prueba con id = {0}", requestId);
		return requestEntity.get();
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
			TrialCohabitationRequestEntity requestEntity) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la solicitud de convivencia de prueba con id = {0}", requestId);

		if (requestId == null || requestId <= 0)
			throw new IllegalOperationException("Trial cohabitation request id is not valid");

		Optional<TrialCohabitationRequestEntity> current = trialCohabitationRequestRepository.findById(requestId);
		if (current.isEmpty())
			throw new EntityNotFoundException("Trial cohabitation request was not found");

		if (requestEntity.getStatus() == null || requestEntity.getStatus().isBlank())
			throw new IllegalOperationException("Status is not valid");

		if (current.get().getTrialCohabitation() != null)
			throw new IllegalOperationException(
					"Unable to update request because it already generated a trial cohabitation");

		requestEntity.setId(requestId);
		requestEntity.setShelter(current.get().getShelter());
		requestEntity.setAdopter(current.get().getAdopter());
		requestEntity.setPet(current.get().getPet());

		log.info("Termina proceso de actualizar la solicitud de convivencia de prueba con id = {0}", requestId);
		return trialCohabitationRequestRepository.save(requestEntity);
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
		log.info("Inicia proceso de borrar la solicitud de convivencia de prueba con id = {0}", requestId);

		if (requestId == null || requestId <= 0)
			throw new IllegalOperationException("Trial cohabitation request id is not valid");

		Optional<TrialCohabitationRequestEntity> requestEntity =
				trialCohabitationRequestRepository.findById(requestId);
		if (requestEntity.isEmpty())
			throw new EntityNotFoundException("Trial cohabitation request was not found");

		if (requestEntity.get().getTrialCohabitation() != null)
			throw new IllegalOperationException(
					"Unable to delete request because it already generated a trial cohabitation");

		trialCohabitationRequestRepository.deleteById(requestId);
		log.info("Termina proceso de borrar la solicitud de convivencia de prueba con id = {0}", requestId);
	}
}
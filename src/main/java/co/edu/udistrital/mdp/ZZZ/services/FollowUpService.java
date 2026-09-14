package co.edu.udistrital.mdp.ZZZ.services;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.ZZZ.entities.AdoptionEntity;
import co.edu.udistrital.mdp.ZZZ.entities.FollowUpEntity;
import co.edu.udistrital.mdp.ZZZ.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.ZZZ.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.ZZZ.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.ZZZ.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.ZZZ.repositories.FollowUpRepository;
import co.edu.udistrital.mdp.ZZZ.repositories.VeterinarianRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FollowUpService {

	private static final String FINALIZED_STATUS = "FINALIZED";

	@Autowired
	FollowUpRepository followUpRepository;

	@Autowired
	AdoptionRepository adoptionRepository;

	@Autowired
	VeterinarianRepository veterinarianRepository;

	/**
	 * Crea un nuevo seguimiento. La adopción asociada debe existir y estar
	 * finalizada, y la fecha programada no puede ser anterior a la fecha actual.
	 */
	@Transactional
	public FollowUpEntity createFollowUp(FollowUpEntity followUp)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación del seguimiento");

		if (followUp.getDate() == null)
			throw new IllegalOperationException("Date cannot be null");
		if (followUp.getObservation() == null || followUp.getObservation().isBlank())
			throw new IllegalOperationException("Observation cannot be null or empty");

		if (followUp.getDate().before(todayStart()))
			throw new IllegalOperationException("The scheduled date for the follow-up cannot be before the current date");

		if (followUp.getAdoption() == null || followUp.getAdoption().getId() == null)
			throw new IllegalOperationException("Follow-up must be associated with an existing adoption");
		Optional<AdoptionEntity> adoption = adoptionRepository.findById(followUp.getAdoption().getId());
		if (adoption.isEmpty())
			throw new EntityNotFoundException("Adoption not found");
		if (!FINALIZED_STATUS.equalsIgnoreCase(adoption.get().getStatus()))
			throw new IllegalOperationException("The adoption must be finalized in order to create a follow-up");

		if (followUp.getVeterinarian() == null || followUp.getVeterinarian().getId() == null)
			throw new IllegalOperationException("Follow-up must be associated with an existing veterinarian");
		Optional<VeterinarianEntity> veterinarian = veterinarianRepository.findById(followUp.getVeterinarian().getId());
		if (veterinarian.isEmpty())
			throw new EntityNotFoundException("Veterinarian not found");

		followUp.setAdoption(adoption.get());
		followUp.setVeterinarian(veterinarian.get());

		log.info("Termina proceso de creación del seguimiento");
		return followUpRepository.save(followUp);
	}

	/**
	 * Obtiene un seguimiento a partir de su id.
	 */
	@Transactional
	public FollowUpEntity getFollowUp(Long followUpId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar el seguimiento con id = {}", followUpId);
		if (followUpId == null || followUpId <= 0)
			throw new IllegalOperationException("Follow-up id is not valid");

		Optional<FollowUpEntity> followUp = followUpRepository.findById(followUpId);
		if (followUp.isEmpty())
			throw new EntityNotFoundException("Follow-up not found");

		log.info("Termina proceso de consultar el seguimiento con id = {}", followUpId);
		return followUp.get();
	}

	/**
	 * Obtiene un seguimiento verificando que el solicitante sea el adoptante
	 * asociado o el veterinario a cargo.
	 */
	@Transactional
	public FollowUpEntity getFollowUp(Long followUpId, Long requesterId)
			throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity followUp = getFollowUp(followUpId);
		if (requesterId == null)
			throw new IllegalOperationException("Requester id is required to visualize the follow-up");

		boolean isAssociatedAdopter = followUp.getAdoption() != null && followUp.getAdoption().getAdopter() != null
				&& requesterId.equals(followUp.getAdoption().getAdopter().getId());
		boolean isAssociatedVeterinarian = followUp.getVeterinarian() != null
				&& requesterId.equals(followUp.getVeterinarian().getId());

		if (!isAssociatedAdopter && !isAssociatedVeterinarian)
			throw new IllegalOperationException(
					"Only authorized personnel and the associated adopter can visualize the follow-up");

		return followUp;
	}

	/**
	 * Obtiene todos los seguimientos registrados.
	 */
	@Transactional
	public List<FollowUpEntity> getFollowUps() {
		log.info("Inicia proceso de consultar todos los seguimientos");
		List<FollowUpEntity> followUps = followUpRepository.findAll();
		if (followUps.isEmpty())
			log.info("No hay seguimientos registrados");
		return followUps;
	}

	/**
	 * Obtiene los seguimientos asociados a una adopción consultada.
	 */
	@Transactional
	public List<FollowUpEntity> getFollowUps(Long adoptionId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar los seguimientos de la adopción con id = {}", adoptionId);
		if (adoptionId == null || adoptionId <= 0)
			throw new IllegalOperationException("Adoption id is not valid");

		Optional<AdoptionEntity> adoption = adoptionRepository.findById(adoptionId);
		if (adoption.isEmpty())
			throw new EntityNotFoundException("Adoption not found");

		List<FollowUpEntity> followUps = followUpRepository.findAll().stream()
				.filter(f -> f.getAdoption() != null && f.getAdoption().getId().equals(adoptionId))
				.toList();
		if (followUps.isEmpty())
			log.info("No hay seguimientos registrados para la adopción consultada");
		return followUps;
	}

	/**
	 * Actualiza un seguimiento existente. La adopción asociada al seguimiento
	 * original no puede ser modificada por otra.
	 */
	@Transactional
	public FollowUpEntity updateFollowUp(Long followUpId, FollowUpEntity followUp)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar el seguimiento con id = {}", followUpId);
		if (followUpId == null || followUpId <= 0)
			throw new IllegalOperationException("Follow-up id is not valid");

		Optional<FollowUpEntity> existing = followUpRepository.findById(followUpId);
		if (existing.isEmpty())
			throw new EntityNotFoundException("Follow-up not found");

		if (followUp.getDate() == null)
			throw new IllegalOperationException("Date cannot be null");
		if (followUp.getObservation() == null || followUp.getObservation().isBlank())
			throw new IllegalOperationException("Observation cannot be null or empty");

		FollowUpEntity current = existing.get();
		if (followUp.getAdoption() != null && current.getAdoption() != null
				&& !followUp.getAdoption().getId().equals(current.getAdoption().getId()))
			throw new IllegalOperationException("The adoption associated with the follow-up cannot be modified");

		current.setDate(followUp.getDate());
		current.setObservation(followUp.getObservation());

		log.info("Termina proceso de actualizar el seguimiento con id = {}", followUpId);
		return followUpRepository.save(current);
	}

	/**
	 * Borra un seguimiento a partir de su id. No se puede eliminar un seguimiento
	 * una vez que haya sido realizado o validado, para preservar el historial.
	 */
	@Transactional
	public void deleteFollowUp(Long followUpId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar el seguimiento con id = {}", followUpId);
		if (followUpId == null || followUpId <= 0)
			throw new IllegalOperationException("Follow-up id is not valid");

		Optional<FollowUpEntity> followUp = followUpRepository.findById(followUpId);
		if (followUp.isEmpty())
			throw new EntityNotFoundException("Follow-up not found");

		if (followUp.get().getDate() != null && followUp.get().getDate().before(todayStart()))
			throw new IllegalOperationException(
					"A follow-up that has already been carried out or validated cannot be deleted");

		followUpRepository.deleteById(followUpId);
		log.info("Termina proceso de borrar el seguimiento con id = {}", followUpId);
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
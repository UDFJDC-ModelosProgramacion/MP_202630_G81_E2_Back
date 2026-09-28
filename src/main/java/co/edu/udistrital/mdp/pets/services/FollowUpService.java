package co.edu.udistrital.mdp.pets.services;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.FollowUpEntity;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.FollowUpRepository;
import co.edu.udistrital.mdp.pets.repositories.VeterinarianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor 
public class FollowUpService {

	private static final String FINALIZED_STATUS = "FINALIZED";
	private static final String ADMIN_ROLE = "ADMIN";

	private static final String FOLLOW_UP_ID_INVALID = "Follow-up id is not valid";
	private static final String FOLLOW_UP_NOT_FOUND = "Follow-up not found";


	private final FollowUpRepository followUpRepository;
	private final AdoptionRepository adoptionRepository;
	private final VeterinarianRepository veterinarianRepository;

	/**
	 * Creates a follow-up for a finalized adoption. Validates the date and
	 * observation, that the veterinarian and the adoption exist, and that the
	 * adoption is in finalized status; the scheduled date must not be in the past.
	 *
	 * @param followUp the follow-up to persist
	 * @return the persisted follow-up
	 * @throws EntityNotFoundException  if the veterinarian or the adoption do not
	 *                                  exist
	 * @throws IllegalOperationException if a required field is missing, the
	 *                                   adoption is not finalized or the date is in
	 *                                   the past
	 */
	@Transactional
	public FollowUpEntity createFollowUp(FollowUpEntity followUp)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Initiate the process of creating the tracking mechanism.");

		if (followUp.getDate() == null)
			throw new IllegalOperationException("Follow-up date cannot be null");
		if (followUp.getObservation() == null || followUp.getObservation().isBlank())
			throw new IllegalOperationException("Observation cannot be null or empty");
		if (followUp.getVeterinarian() == null || followUp.getVeterinarian().getId() == null)
			throw new IllegalOperationException("Follow-up must be associated with an existing veterinarian");
		if (followUp.getAdoption() == null || followUp.getAdoption().getId() == null)
			throw new IllegalOperationException("Follow-up must be associated with an existing adoption");

		Optional<VeterinarianEntity> veterinarian = veterinarianRepository.findById(followUp.getVeterinarian().getId());
		if (veterinarian.isEmpty())
			throw new EntityNotFoundException("Veterinarian not found");

		Optional<AdoptionEntity> adoption = adoptionRepository.findById(followUp.getAdoption().getId());
		if (adoption.isEmpty())
			throw new EntityNotFoundException("Adoption not found");

		if (!FINALIZED_STATUS.equalsIgnoreCase(adoption.get().getStatus()))
			throw new IllegalOperationException("The associated adoption must be in finalized status");

		Date today = new Date();
		if (followUp.getDate().before(today))
			throw new IllegalOperationException("The scheduled follow-up date cannot be before the current date");

		followUp.setVeterinarian(veterinarian.get());
		followUp.setAdoption(adoption.get());

		log.info("The process of creating the tracking setup is complete.");
		return followUpRepository.save(followUp);
	}

	@Transactional
	public List<FollowUpEntity> getFollowUps() {
		log.info("Initiate the process of querying all follow-ups.");
		List<FollowUpEntity> followUps = followUpRepository.findAll();
		if (followUps.isEmpty())
			log.info("No hay seguimientos registrados.");
		return followUps;
	}

	@Transactional
	public List<FollowUpEntity> getFollowUps(Long adoptionId) {
		log.info("Initiate the process of checking adoption follow-ups with id = {}", adoptionId);
		List<FollowUpEntity> followUps = followUpRepository.findAll().stream()
				.filter(f -> f.getAdoption() != null && adoptionId.equals(f.getAdoption().getId()))
				.toList();
		if (followUps.isEmpty())
			log.info("No follow-ups are recorded for the adoption in question.");
		return followUps;
	}

	@Transactional
	public FollowUpEntity getFollowUp(Long followUpId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Starting the process to check the tracking for id = {}", followUpId);
		if (followUpId == null || followUpId <= 0)
			throw new IllegalOperationException(FOLLOW_UP_ID_INVALID);

		Optional<FollowUpEntity> followUp = followUpRepository.findById(followUpId);
		if (followUp.isEmpty())
			throw new EntityNotFoundException(FOLLOW_UP_NOT_FOUND);

		log.info("Tracking inquiry process for id = {} completed.", followUpId);
		return followUp.get();
	}

	@Transactional
	public FollowUpEntity getFollowUp(Long followUpId, Long requesterId)
			throws EntityNotFoundException, IllegalOperationException {
		return doGetFollowUp(followUpId, requesterId, null);
	}

	@Transactional
	public FollowUpEntity getFollowUp(Long followUpId, Long requesterId, String requesterRole)
			throws EntityNotFoundException, IllegalOperationException {
		return doGetFollowUp(followUpId, requesterId, requesterRole);
	}

	/**
	 * Shared lookup logic with access control. It is not annotated as @Transactional
	 * to avoid self-invocation between transactional methods of the same class (the
	 * Spring proxy does not intercept internal calls via "this"); every public entry
	 * point is already annotated and delegates here.
	 *
	 * @param followUpId    the id of the follow-up to consult
	 * @param requesterId   the id of the user requesting the consultation
	 * @param requesterRole the role of the requesting user, or null for the
	 *                      two-argument variant
	 * @return the follow-up
	 * @throws EntityNotFoundException  if the follow-up or its associated entities do
	 *                                  not exist
	 * @throws IllegalOperationException if the follow-up id is not valid, no requester
	 *                                   is provided or the requester is not authorized
	 */
	private FollowUpEntity doGetFollowUp(Long followUpId, Long requesterId, String requesterRole)
			throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity followUp = fetchFollowUpById(followUpId);

		if (ADMIN_ROLE.equalsIgnoreCase(requesterRole))
			return followUp;

		if (requesterId == null)
			throw new IllegalOperationException("A requester is required to consult a follow-up");

		boolean isAssociatedAdopter = followUp.getAdoption() != null && followUp.getAdoption().getAdopter() != null
				&& requesterId.equals(followUp.getAdoption().getAdopter().getId());
		boolean isAssociatedVeterinarian = followUp.getVeterinarian() != null
				&& requesterId.equals(followUp.getVeterinarian().getId());
		if (!isAssociatedAdopter && !isAssociatedVeterinarian)
			throw new IllegalOperationException(
					"Only authorized staff (administrators/veterinarians) and the associated adopter can consult a follow-up");

		return followUp;
	}

	private FollowUpEntity fetchFollowUpById(Long followUpId) throws EntityNotFoundException, IllegalOperationException {
		if (followUpId == null || followUpId <= 0)
			throw new IllegalOperationException(FOLLOW_UP_ID_INVALID);

		Optional<FollowUpEntity> followUp = followUpRepository.findById(followUpId);
		if (followUp.isEmpty())
			throw new EntityNotFoundException(FOLLOW_UP_NOT_FOUND);

		return followUp.get();
	}

	/**
	 * Updates a follow-up. Only the date and the observation can be changed; the
	 * associated adoption and veterinarian are immutable.
	 *
	 * @param followUpId the id of the follow-up to update
	 * @param followUp   the object holding the new date and observation
	 * @return the updated follow-up
	 * @throws EntityNotFoundException  if the follow-up does not exist
	 * @throws IllegalOperationException if the id is not valid, a required field is
	 *                                   missing or the adoption is changed
	 */
	@Transactional
	public FollowUpEntity updateFollowUp(Long followUpId, FollowUpEntity followUp)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Initiating the process to update the tracking with id = {}", followUpId);
		if (followUpId == null || followUpId <= 0)
			throw new IllegalOperationException(FOLLOW_UP_ID_INVALID);

		Optional<FollowUpEntity> existing = followUpRepository.findById(followUpId);
		if (existing.isEmpty())
			throw new EntityNotFoundException(FOLLOW_UP_NOT_FOUND);

		if (followUp.getDate() == null)
			throw new IllegalOperationException("Follow-up date cannot be null");
		if (followUp.getObservation() == null || followUp.getObservation().isBlank())
			throw new IllegalOperationException("Observation cannot be null or empty");

		FollowUpEntity current = existing.get();
		if (followUp.getAdoption() != null && followUp.getAdoption().getId() != null
				&& !followUp.getAdoption().getId().equals(current.getAdoption().getId()))
			throw new IllegalOperationException("The adoption associated to the follow-up cannot be changed");

		current.setDate(followUp.getDate());
		current.setObservation(followUp.getObservation());

		log.info("Process of updating the tracking with id = {} completed.", followUpId);
		return followUpRepository.save(current);
	}

	/**
	 * Deletes a follow-up. Follow-ups whose date has already passed are preserved
	 * because they are part of the shelter's adoption history.
	 *
	 * @param followUpId the id of the follow-up to delete
	 * @throws EntityNotFoundException  if the follow-up does not exist
	 * @throws IllegalOperationException if the id is not valid or the follow-up has
	 *                                   already been validated/closed
	 */
	@Transactional
	public void deleteFollowUp(Long followUpId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Starting the process to delete the tracking with id = {}", followUpId);
		if (followUpId == null || followUpId <= 0)
			throw new IllegalOperationException(FOLLOW_UP_ID_INVALID);

		Optional<FollowUpEntity> followUp = followUpRepository.findById(followUpId);
		if (followUp.isEmpty())
			throw new EntityNotFoundException(FOLLOW_UP_NOT_FOUND);

		FollowUpEntity current = followUp.get();
		if (current.getDate() != null && current.getDate().before(new Date()))
			throw new IllegalOperationException(
					"A follow-up that has already been closed or validated cannot be deleted; it preserves the shelter history");

		followUpRepository.deleteById(followUpId);
		log.info("Finished the process of deleting the tracking with id = {}", followUpId);
	}
}
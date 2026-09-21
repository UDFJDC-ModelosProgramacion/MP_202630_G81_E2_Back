package co.edu.udistrital.mdp.pets.services;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.pets.entities.BaseEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdopterRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRequestRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.ShelterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdoptionService {

	public static final String FINALIZED_STATUS = "FINALIZED";
	public static final String CANCELLED_STATUS = "CANCELLED";

	private static final String ADOPTION_ID_NOT_VALID = "Adoption id is not valid";
	private static final String ADOPTION_NOT_VALID = "Adoption is not valid";
	private static final String ADOPTION_NOT_FOUND = "Adoption was not found";
	private static final String STATUS_NOT_VALID = "Status is not valid";
	private static final String DATE_NOT_VALID = "Date is not valid";
	private static final String PET_LABEL = "Pet";
	private static final String SHELTER_LABEL = "Shelter";
	private static final String ADOPTER_LABEL = "Adopter";
	private static final String ADOPTION_REQUEST_LABEL = "Adoption request";

	private final AdoptionRepository adoptionRepository;
	private final PetRepository petRepository;
	private final ShelterRepository shelterRepository;
	private final AdopterRepository adopterRepository;
	private final AdoptionRequestRepository adoptionRequestRepository;

	/**
	 * Crea una nueva adopción.
	 *
	 * Reglas de negocio:
	 * 1. Ningún atributo obligatorio puede ser nulo o vacío.
	 * 2. La mascota, el refugio y el adoptante deben existir.
	 * 3. La solicitud de adopción (adoptionRequest) debe existir y estar aprobada (status = "APPROVED").
	 * 4. La mascota no puede tener ya una adopción vigente (las canceladas no cuentan).
	 * 5. La solicitud de adopción no puede estar ya asociada a otra adopción.
	 */
	@Transactional
	public AdoptionEntity createAdoption(AdoptionEntity adoption)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la adopción");

		validateAdoption(adoption);
		if (adoption.getDate() == null)
			throw new IllegalOperationException(DATE_NOT_VALID);

		PetEntity pet = findReference(adoption.getPet(), petRepository, PET_LABEL);
		ShelterEntity shelter = findReference(adoption.getShelter(), shelterRepository, SHELTER_LABEL);
		AdopterEntity adopter = findReference(adoption.getAdopter(), adopterRepository, ADOPTER_LABEL);
		AdoptionRequestEntity adoptionRequest = findReference(adoption.getAdoptionRequest(),
				adoptionRequestRepository, ADOPTION_REQUEST_LABEL);

		if (!AdoptionRequestService.APPROVED_STATUS.equalsIgnoreCase(adoptionRequest.getStatus()))
			throw new IllegalOperationException("Unable to create adoption because the request is not approved");

		if (adoptionRequest.getAdoption() != null)
			throw new IllegalOperationException("Unable to create adoption because the request already has one");

		if (hasActiveAdoption(pet))
			throw new IllegalOperationException("Unable to create adoption because the pet is already adopted");

		adoption.setPet(pet);
		adoption.setShelter(shelter);
		adoption.setAdopter(adopter);
		adoption.setAdoptionRequest(adoptionRequest);

		log.info("Termina proceso de creación de la adopción");
		return adoptionRepository.save(adoption);
	}

	@Transactional(readOnly = true)
	public List<AdoptionEntity> getAdoptions() {
		log.info("Inicia proceso de consultar todas las adopciones");
		return adoptionRepository.findAll();
	}

	@Transactional(readOnly = true)
	public AdoptionEntity getAdoption(Long adoptionId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la adopción con id = {}", adoptionId);
		AdoptionEntity adoption = findAdoption(adoptionId);
		log.info("Termina proceso de consultar la adopción con id = {}", adoptionId);
		return adoption;
	}

	/**
	 * Actualiza una adopción.
	 *
	 * Reglas de negocio:
	 * 1. No se aceptan identificadores inválidos ni atributos nulos.
	 * 2. La mascota, el adoptante, el refugio y la solicitud de adopción no pueden modificarse tras la creación.
	 * 3. No se puede modificar una adopción finalizada o cancelada.
	 */
	@Transactional
	public AdoptionEntity updateAdoption(Long adoptionId, AdoptionEntity adoptionUpdate)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la adopción con id = {}", adoptionId);

		AdoptionEntity current = findAdoption(adoptionId);
		validateAdoption(adoptionUpdate);

		if (isClosed(current))
			throw new IllegalOperationException("Unable to update an adoption that is finished or cancelled");

		current.setStatus(adoptionUpdate.getStatus());
		current.setImportantNotes(adoptionUpdate.getImportantNotes());
		if (adoptionUpdate.getDate() != null)
			current.setDate(adoptionUpdate.getDate());

		log.info("Termina proceso de actualizar la adopción con id = {}", adoptionId);
		return adoptionRepository.save(current);
	}

	/**
	 * Elimina una adopción.
	 *
	 * Reglas de negocio:
	 * 1. No se aceptan identificadores inválidos.
	 * 2. Si el identificador no existe, se lanza una excepción.
	 * 3. No se puede eliminar una adopción que tenga reseñas (reviews) o seguimientos (followUps) asociados.
	 */
	@Transactional
	public void deleteAdoption(Long adoptionId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar la adopción con id = {}", adoptionId);

		AdoptionEntity adoption = findAdoption(adoptionId);

		if (!isNullOrEmpty(adoption.getReviews()))
			throw new IllegalOperationException("Unable to delete adoption because it has associated reviews");

		if (!isNullOrEmpty(adoption.getFollowUps()))
			throw new IllegalOperationException("Unable to delete adoption because it has associated follow-ups");

		adoptionRepository.delete(adoption);
		log.info("Termina proceso de borrar la adopción con id = {}", adoptionId);
	}

	private AdoptionEntity findAdoption(Long adoptionId) throws EntityNotFoundException, IllegalOperationException {
		if (adoptionId == null || adoptionId <= 0)
			throw new IllegalOperationException(ADOPTION_ID_NOT_VALID);
		return adoptionRepository.findById(adoptionId)
				.orElseThrow(() -> new EntityNotFoundException(ADOPTION_NOT_FOUND));
	}

	/** Reglas de datos comunes a la creación y a la actualización. */
	private void validateAdoption(AdoptionEntity adoption) throws IllegalOperationException {
		if (adoption == null)
			throw new IllegalOperationException(ADOPTION_NOT_VALID);
		if (adoption.getStatus() == null || adoption.getStatus().isBlank())
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

	/** Una mascota ya está adoptada mientras tenga una adopción que no haya sido cancelada. */
	private boolean hasActiveAdoption(PetEntity pet) {
		return pet.getAdoptions() != null
				&& pet.getAdoptions().stream().anyMatch(a -> !CANCELLED_STATUS.equalsIgnoreCase(a.getStatus()));
	}

	private boolean isClosed(AdoptionEntity adoption) {
		return FINALIZED_STATUS.equalsIgnoreCase(adoption.getStatus())
				|| CANCELLED_STATUS.equalsIgnoreCase(adoption.getStatus());
	}

	private boolean isNullOrEmpty(Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}
}
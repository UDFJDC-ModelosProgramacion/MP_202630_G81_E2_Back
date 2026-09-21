package co.edu.udistrital.mdp.pets.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdopterRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRequestRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.ShelterRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AdoptionService {

	@Autowired
	private AdoptionRepository adoptionRepository;

	@Autowired
	private PetRepository petRepository;

	@Autowired
	private ShelterRepository shelterRepository;

	@Autowired
	private AdopterRepository adopterRepository;

	@Autowired
	private AdoptionRequestRepository adoptionRequestRepository;

	/**
	 * Crea una nueva adopción.
	 *
	 * Reglas de negocio:
	 * 1. Ningún atributo obligatorio puede ser nulo o vacío.
	 * 2. La mascota, el refugio y el adoptante deben existir.
	 * 3. La solicitud de adopción (adoptionRequest) debe existir y estar aprobada (status = "APPROVED").
	 * 4. No puede existir ya una adopción registrada para esa mascota.
	 * 5. La solicitud de adopción no puede estar ya asociada a otra adopción.
	 */
	@Transactional
	public AdoptionEntity createAdoption(AdoptionEntity adoptionEntity)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación de la adopción");

		if (adoptionEntity.getDate() == null)
			throw new IllegalOperationException("Date is not valid");

		if (adoptionEntity.getStatus() == null || adoptionEntity.getStatus().isBlank())
			throw new IllegalOperationException("Status is not valid");

		if (adoptionEntity.getPet() == null)
			throw new IllegalOperationException("Pet is not valid");

		Optional<PetEntity> pet = petRepository.findById(adoptionEntity.getPet().getId());
		if (pet.isEmpty())
			throw new EntityNotFoundException("Pet was not found");

		if (adoptionEntity.getShelter() == null)
			throw new IllegalOperationException("Shelter is not valid");

		Optional<ShelterEntity> shelter = shelterRepository.findById(adoptionEntity.getShelter().getId());
		if (shelter.isEmpty())
			throw new EntityNotFoundException("Shelter was not found");

		if (adoptionEntity.getAdopter() == null)
			throw new IllegalOperationException("Adopter is not valid");

		Optional<AdopterEntity> adopter = adopterRepository.findById(adoptionEntity.getAdopter().getId());
		if (adopter.isEmpty())
			throw new EntityNotFoundException("Adopter was not found");

		if (adoptionEntity.getAdoptionRequest() == null)
			throw new IllegalOperationException("Adoption request is not valid");

		Optional<AdoptionRequestEntity> adoptionRequest =
				adoptionRequestRepository.findById(adoptionEntity.getAdoptionRequest().getId());
		if (adoptionRequest.isEmpty())
			throw new EntityNotFoundException("Adoption request was not found");

		if (!"APPROVED".equals(adoptionRequest.get().getStatus()))
			throw new IllegalOperationException("Unable to create adoption because the request is not approved");

		if (adoptionRequest.get().getAdoption() != null)
			throw new IllegalOperationException("Unable to create adoption because the request already has one");

		if (pet.get().getAdoptions() != null && !pet.get().getAdoptions().isEmpty())
			throw new IllegalOperationException("Unable to create adoption because the pet is already adopted");

		adoptionEntity.setPet(pet.get());
		adoptionEntity.setShelter(shelter.get());
		adoptionEntity.setAdopter(adopter.get());
		adoptionEntity.setAdoptionRequest(adoptionRequest.get());

		log.info("Termina proceso de creación de la adopción");
		return adoptionRepository.save(adoptionEntity);
	}

	@Transactional
	public List<AdoptionEntity> getAdoptions() {
		log.info("Inicia proceso de consultar todas las adopciones");
		return adoptionRepository.findAll();
	}

	@Transactional
	public AdoptionEntity getAdoption(Long adoptionId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar la adopción con id = {0}", adoptionId);
		if (adoptionId == null || adoptionId <= 0)
			throw new IllegalOperationException("Adoption id is not valid");
		Optional<AdoptionEntity> adoptionEntity = adoptionRepository.findById(adoptionId);
		if (adoptionEntity.isEmpty())
			throw new EntityNotFoundException("Adoption was not found");
		log.info("Termina proceso de consultar la adopción con id = {0}", adoptionId);
		return adoptionEntity.get();
	}

	/**
	 * Actualiza una adopción.
	 *
	 * Reglas de negocio:
	 * 1. No se aceptan identificadores inválidos ni atributos nulos.
	 * 2. La mascota, el adoptante y la solicitud de adopción no pueden modificarse tras la creación.
	 * 3. No se puede modificar una adopción finalizada o cancelada.
	 */
	@Transactional
	public AdoptionEntity updateAdoption(Long adoptionId, AdoptionEntity adoption)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar la adopción con id = {0}", adoptionId);

		if (adoptionId == null || adoptionId <= 0)
			throw new IllegalOperationException("Adoption id is not valid");

		Optional<AdoptionEntity> current = adoptionRepository.findById(adoptionId);
		if (current.isEmpty())
			throw new EntityNotFoundException("Adoption was not found");

		if (adoption.getStatus() == null || adoption.getStatus().isBlank())
			throw new IllegalOperationException("Status is not valid");

		if ("FINISHED".equals(current.get().getStatus()) || "CANCELLED".equals(current.get().getStatus()))
			throw new IllegalOperationException("Unable to update an adoption that is finished or cancelled");

		adoption.setId(adoptionId);
		adoption.setPet(current.get().getPet());
		adoption.setAdopter(current.get().getAdopter());
		adoption.setShelter(current.get().getShelter());
		adoption.setAdoptionRequest(current.get().getAdoptionRequest());

		log.info("Termina proceso de actualizar la adopción con id = {0}", adoptionId);
		return adoptionRepository.save(adoption);
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
		log.info("Inicia proceso de borrar la adopción con id = {0}", adoptionId);

		if (adoptionId == null || adoptionId <= 0)
			throw new IllegalOperationException("Adoption id is not valid");

		Optional<AdoptionEntity> adoptionEntity = adoptionRepository.findById(adoptionId);
		if (adoptionEntity.isEmpty())
			throw new EntityNotFoundException("Adoption was not found");

		if (adoptionEntity.get().getReviews() != null && !adoptionEntity.get().getReviews().isEmpty())
			throw new IllegalOperationException("Unable to delete adoption because it has associated reviews");

		if (adoptionEntity.get().getFollowUps() != null && !adoptionEntity.get().getFollowUps().isEmpty())
			throw new IllegalOperationException("Unable to delete adoption because it has associated follow-ups");

		adoptionRepository.deleteById(adoptionId);
		log.info("Termina proceso de borrar la adopción con id = {0}", adoptionId);
	}
}
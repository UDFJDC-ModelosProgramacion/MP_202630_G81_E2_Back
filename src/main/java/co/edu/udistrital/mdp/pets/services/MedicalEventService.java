package co.edu.udistrital.mdp.pets.services;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.BaseEntity;
import co.edu.udistrital.mdp.pets.entities.MedicalEventEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.MedicalEventRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.VeterinarianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalEventService {

	private static final String MEDICAL_EVENT_ID_NOT_VALID = "Medical event id is not valid";
	private static final String MEDICAL_EVENT_NOT_VALID = "Medical event is not valid";
	private static final String MEDICAL_EVENT_NOT_FOUND = "Medical event was not found";
	private static final String DATE_NOT_VALID = "Date is not valid";
	private static final String DATE_IN_FUTURE = "Date cannot be in the future";
	private static final String TYPE_NOT_VALID = "Type is not valid";
	private static final String PET_LABEL = "Pet";
	private static final String VETERINARIAN_LABEL = "Veterinarian";

	private final MedicalEventRepository medicalEventRepository;
	private final PetRepository petRepository;
	private final VeterinarianRepository veterinarianRepository;

	/**
	 * Crea un nuevo evento médico.
	 *
	 * Reglas de negocio:
	 * 1. Ningún atributo obligatorio puede ser nulo o vacío.
	 * 2. La mascota (pet) debe existir.
	 * 3. El veterinario debe existir.
	 * 4. La fecha del evento no puede ser posterior a la fecha actual.
	 */
	@Transactional
	public MedicalEventEntity createMedicalEvent(MedicalEventEntity medicalEvent)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación del evento médico");

		validateMedicalEvent(medicalEvent);

		PetEntity pet = findReference(medicalEvent.getPet(), petRepository, PET_LABEL);
		VeterinarianEntity veterinarian = findReference(medicalEvent.getVeterinarian(), veterinarianRepository,
				VETERINARIAN_LABEL);

		medicalEvent.setPet(pet);
		medicalEvent.setVeterinarian(veterinarian);

		log.info("Termina proceso de creación del evento médico");
		return medicalEventRepository.save(medicalEvent);
	}

	@Transactional(readOnly = true)
	public List<MedicalEventEntity> getMedicalEvents() {
		log.info("Inicia proceso de consultar todos los eventos médicos");
		return medicalEventRepository.findAll();
	}

	@Transactional(readOnly = true)
	public MedicalEventEntity getMedicalEvent(Long medicalEventId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar el evento médico con id = {}", medicalEventId);
		MedicalEventEntity medicalEvent = findMedicalEvent(medicalEventId);
		log.info("Termina proceso de consultar el evento médico con id = {}", medicalEventId);
		return medicalEvent;
	}

	/**
	 * Actualiza un evento médico.
	 *
	 * Reglas de negocio:
	 * 1. No se aceptan identificadores inválidos ni atributos nulos.
	 * 2. La mascota asociada no puede modificarse tras la creación.
	 * 3. La fecha del evento no puede ser posterior a la fecha actual.
	 * 4. Si se envía un veterinario, este debe existir; si no, se conserva el actual.
	 */
	@Transactional
	public MedicalEventEntity updateMedicalEvent(Long medicalEventId, MedicalEventEntity medicalEventUpdate)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar el evento médico con id = {}", medicalEventId);

		MedicalEventEntity current = findMedicalEvent(medicalEventId);
		validateMedicalEvent(medicalEventUpdate);

		if (medicalEventUpdate.getVeterinarian() != null)
			current.setVeterinarian(
					findReference(medicalEventUpdate.getVeterinarian(), veterinarianRepository, VETERINARIAN_LABEL));

		current.setDate(medicalEventUpdate.getDate());
		current.setType(medicalEventUpdate.getType());
		current.setDescription(medicalEventUpdate.getDescription());

		log.info("Termina proceso de actualizar el evento médico con id = {}", medicalEventId);
		return medicalEventRepository.save(current);
	}

	/**
	 * Elimina un evento médico.
	 *
	 * Reglas de negocio:
	 * 1. No se aceptan identificadores inválidos.
	 * 2. Si el identificador no existe, se lanza una excepción.
	 */
	@Transactional
	public void deleteMedicalEvent(Long medicalEventId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar el evento médico con id = {}", medicalEventId);

		medicalEventRepository.delete(findMedicalEvent(medicalEventId));

		log.info("Termina proceso de borrar el evento médico con id = {}", medicalEventId);
	}

	private MedicalEventEntity findMedicalEvent(Long medicalEventId)
			throws EntityNotFoundException, IllegalOperationException {
		if (medicalEventId == null || medicalEventId <= 0)
			throw new IllegalOperationException(MEDICAL_EVENT_ID_NOT_VALID);
		return medicalEventRepository.findById(medicalEventId)
				.orElseThrow(() -> new EntityNotFoundException(MEDICAL_EVENT_NOT_FOUND));
	}

	/** Reglas de datos comunes a la creación y a la actualización. */
	private void validateMedicalEvent(MedicalEventEntity medicalEvent) throws IllegalOperationException {
		if (medicalEvent == null)
			throw new IllegalOperationException(MEDICAL_EVENT_NOT_VALID);
		if (medicalEvent.getDate() == null)
			throw new IllegalOperationException(DATE_NOT_VALID);
		if (medicalEvent.getDate().after(new Date()))
			throw new IllegalOperationException(DATE_IN_FUTURE);
		if (medicalEvent.getType() == null || medicalEvent.getType().isBlank())
			throw new IllegalOperationException(TYPE_NOT_VALID);
	}

	/** Resuelve una entidad relacionada: la referencia y su id son obligatorios y debe existir. */
	private <T extends BaseEntity> T findReference(T reference, JpaRepository<T, Long> repository, String label)
			throws EntityNotFoundException, IllegalOperationException {
		if (reference == null || reference.getId() == null)
			throw new IllegalOperationException(label + " is not valid");
		return repository.findById(reference.getId())
				.orElseThrow(() -> new EntityNotFoundException(label + " was not found"));
	}
}
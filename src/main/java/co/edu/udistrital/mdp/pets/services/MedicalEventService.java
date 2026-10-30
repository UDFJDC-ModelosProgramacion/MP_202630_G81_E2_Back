package co.edu.udistrital.mdp.pets.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
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
	private static final String DESCRIPTION_NOT_VALID = "Description is not valid";
	private static final String PET_NOT_ACTIVE = "The pet is not active in the system";
	private static final String DUPLICATE_MEDICAL_EVENT =
			"A medical event with the same pet, type and date already exists";
	private static final String PET_CANNOT_BE_MODIFIED = "The pet of a medical event cannot be modified";
	private static final String PET_ID_NOT_VALID = "Pet id is not valid";
	private static final String PET_NOT_FOUND = "Pet was not found";
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
	 * 2. La mascota (pet) debe existir y estar activa.
	 * 3. El veterinario debe existir.
	 * 4. La fecha del evento no puede ser posterior a la fecha actual.
	 * 5. No se puede registrar un evento duplicado (misma mascota, tipo y fecha).
	 */
	@Transactional
	public MedicalEventEntity createMedicalEvent(MedicalEventEntity medicalEvent)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación del evento médico");

		validateMedicalEvent(medicalEvent);

		PetEntity pet = findReference(medicalEvent.getPet(), petRepository, PET_LABEL);
		VeterinarianEntity veterinarian = findReference(medicalEvent.getVeterinarian(), veterinarianRepository,
				VETERINARIAN_LABEL);

		if (!isActive(pet.getActive()))
			throw new IllegalOperationException(PET_NOT_ACTIVE);
		if (isDuplicate(medicalEvent, pet))
			throw new IllegalOperationException(DUPLICATE_MEDICAL_EVENT);

		medicalEvent.setPet(pet);
		medicalEvent.setVeterinarian(veterinarian);

		log.info("Termina proceso de creación del evento médico");
		return medicalEventRepository.save(medicalEvent);
	}

	@Transactional
	public MedicalEventEntity createMedicalEvent(Long petId, MedicalEventEntity medicalEvent)
			throws EntityNotFoundException, IllegalOperationException {
		medicalEvent.setPet(findPetOrThrow(petId));
		return createMedicalEvent(medicalEvent);
	}

	@Transactional(readOnly = true)
	public List<MedicalEventEntity> getMedicalEvents() {
		log.info("Inicia proceso de consultar todos los eventos médicos");
		return medicalEventRepository.findAll();
	}

	/** Consulta con filtros opcionales por mascota, tipo y fecha (yyyy-MM-dd). */
	@Transactional(readOnly = true)
	public List<MedicalEventEntity> getMedicalEvents(Long petId, String type, String date)
			throws IllegalOperationException {
		log.info("Inicia proceso de consultar los eventos médicos con filtros");
		if (petId != null && petId < 0)
			throw new IllegalOperationException(PET_ID_NOT_VALID);
		final LocalDate day = parseDay(date);
		return medicalEventRepository.findAll().stream()
				.filter(e -> petId == null || (e.getPet() != null && petId.equals(e.getPet().getId())))
				.filter(e -> type == null || type.equalsIgnoreCase(e.getType()))
				.filter(e -> day == null || (e.getDate() != null && day.equals(toLocalDate(e.getDate()))))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MedicalEventEntity> getMedicalEvents(Long petId)
			throws EntityNotFoundException, IllegalOperationException {
		findPetOrThrow(petId);
		return medicalEventRepository.findAll().stream()
				.filter(event -> event.getPet() != null && petId.equals(event.getPet().getId()))
				.toList();
	}

	@Transactional(readOnly = true)
	public MedicalEventEntity getMedicalEvent(Long medicalEventId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar el evento médico con id = {}", medicalEventId);
		MedicalEventEntity medicalEvent = findMedicalEvent(medicalEventId);
		log.info("Termina proceso de consultar el evento médico con id = {}", medicalEventId);
		return medicalEvent;
	}

	@Transactional(readOnly = true)
	public MedicalEventEntity getMedicalEvent(Long petId, Long medicalEventId)
			throws EntityNotFoundException, IllegalOperationException {
		findPetOrThrow(petId);
		MedicalEventEntity medicalEvent = getMedicalEvent(medicalEventId);
		validateEventBelongsToPet(medicalEvent, petId);
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

		if (medicalEventUpdate.getPet() != null && medicalEventUpdate.getPet().getId() != null
				&& !medicalEventUpdate.getPet().getId().equals(current.getPet().getId()))
			throw new IllegalOperationException(PET_CANNOT_BE_MODIFIED);

		if (medicalEventUpdate.getVeterinarian() != null)
			current.setVeterinarian(
					findReference(medicalEventUpdate.getVeterinarian(), veterinarianRepository, VETERINARIAN_LABEL));

		current.setDate(medicalEventUpdate.getDate());
		current.setType(medicalEventUpdate.getType());
		current.setDescription(medicalEventUpdate.getDescription());

		log.info("Termina proceso de actualizar el evento médico con id = {}", medicalEventId);
		return medicalEventRepository.save(current);
	}

	@Transactional
	public MedicalEventEntity updateMedicalEvent(Long petId, Long medicalEventId,
			MedicalEventEntity medicalEventUpdate)
			throws EntityNotFoundException, IllegalOperationException {
		findPetOrThrow(petId);
		MedicalEventEntity current = getMedicalEvent(medicalEventId);
		validateEventBelongsToPet(current, petId);
		return updateMedicalEvent(medicalEventId, medicalEventUpdate);
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

	@Transactional
	public void deleteMedicalEvent(Long petId, Long medicalEventId)
			throws EntityNotFoundException, IllegalOperationException {
		findPetOrThrow(petId);
		MedicalEventEntity medicalEvent = getMedicalEvent(medicalEventId);
		validateEventBelongsToPet(medicalEvent, petId);
		deleteMedicalEvent(medicalEventId);
	}

	private PetEntity findPetOrThrow(Long petId) throws EntityNotFoundException, IllegalOperationException {
		if (petId == null || petId < 0)
			throw new IllegalOperationException(PET_ID_NOT_VALID);
		return petRepository.findById(petId)
				.orElseThrow(() -> new EntityNotFoundException(PET_NOT_FOUND));
	}

	private void validateEventBelongsToPet(MedicalEventEntity medicalEvent, Long petId)
			throws IllegalOperationException {
		if (medicalEvent.getPet() == null || !petId.equals(medicalEvent.getPet().getId()))
			throw new IllegalOperationException("The medical event is not associated with the pet");
	}

	/** Un id negativo no es válido; el cero se trata como un id que no existe. */
	private MedicalEventEntity findMedicalEvent(Long medicalEventId)
			throws EntityNotFoundException, IllegalOperationException {
		if (medicalEventId == null || medicalEventId < 0)
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
		if (medicalEvent.getDescription() == null || medicalEvent.getDescription().isBlank())
			throw new IllegalOperationException(DESCRIPTION_NOT_VALID);
	}

	private boolean isDuplicate(MedicalEventEntity medicalEvent, PetEntity pet) {
		return medicalEventRepository.findAll().stream()
				.anyMatch(e -> e.getPet() != null && pet.getId().equals(e.getPet().getId())
						&& medicalEvent.getType().equalsIgnoreCase(e.getType())
						&& e.getDate() != null && toLocalDate(e.getDate()).equals(toLocalDate(medicalEvent.getDate())));
	}

	private boolean isActive(Boolean active) {
		return !Boolean.FALSE.equals(active);
	}

	/** Se usa getTime() porque las fechas leídas de la base de datos pueden ser java.sql.Date. */
	private LocalDate toLocalDate(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}

	private LocalDate parseDay(String date) throws IllegalOperationException {
		if (date == null)
			return null;
		try {
			return LocalDate.parse(date);
		} catch (DateTimeParseException e) {
			throw new IllegalOperationException(DATE_NOT_VALID);
		}
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

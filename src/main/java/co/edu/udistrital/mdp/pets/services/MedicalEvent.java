package co.edu.udistrital.mdp.pets.services;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.MedicalEventEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.MedicalEventRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.VeterinarianRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MedicalEventService {

	@Autowired
	private MedicalEventRepository medicalEventRepository;

	@Autowired
	private PetRepository petRepository;

	@Autowired
	private VeterinarianRepository veterinarianRepository;

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
	public MedicalEventEntity createMedicalEvent(MedicalEventEntity medicalEventEntity)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación del evento médico");

		if (medicalEventEntity.getDate() == null)
			throw new IllegalOperationException("Date is not valid");

		if (medicalEventEntity.getDate().after(new Date()))
			throw new IllegalOperationException("Date cannot be in the future");

		if (medicalEventEntity.getType() == null || medicalEventEntity.getType().isBlank())
			throw new IllegalOperationException("Type is not valid");

		if (medicalEventEntity.getPet() == null)
			throw new IllegalOperationException("Pet is not valid");

		Optional<PetEntity> pet = petRepository.findById(medicalEventEntity.getPet().getId());
		if (pet.isEmpty())
			throw new EntityNotFoundException("Pet was not found");

		if (medicalEventEntity.getVeterinarian() == null)
			throw new IllegalOperationException("Veterinarian is not valid");

		Optional<VeterinarianEntity> veterinarian =
				veterinarianRepository.findById(medicalEventEntity.getVeterinarian().getId());
		if (veterinarian.isEmpty())
			throw new EntityNotFoundException("Veterinarian was not found");

		medicalEventEntity.setPet(pet.get());
		medicalEventEntity.setVeterinarian(veterinarian.get());

		log.info("Termina proceso de creación del evento médico");
		return medicalEventRepository.save(medicalEventEntity);
	}

	@Transactional
	public List<MedicalEventEntity> getMedicalEvents() {
		log.info("Inicia proceso de consultar todos los eventos médicos");
		return medicalEventRepository.findAll();
	}

	@Transactional
	public MedicalEventEntity getMedicalEvent(Long medicalEventId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar el evento médico con id = {0}", medicalEventId);
		if (medicalEventId == null || medicalEventId <= 0)
			throw new IllegalOperationException("Medical event id is not valid");
		Optional<MedicalEventEntity> medicalEventEntity = medicalEventRepository.findById(medicalEventId);
		if (medicalEventEntity.isEmpty())
			throw new EntityNotFoundException("Medical event was not found");
		log.info("Termina proceso de consultar el evento médico con id = {0}", medicalEventId);
		return medicalEventEntity.get();
	}

	/**
	 * Actualiza un evento médico.
	 *
	 * Reglas de negocio:
	 * 1. No se aceptan identificadores inválidos ni atributos nulos.
	 * 2. La mascota asociada no puede modificarse tras la creación.
	 * 3. La fecha del evento no puede ser posterior a la fecha actual.
	 */
	@Transactional
	public MedicalEventEntity updateMedicalEvent(Long medicalEventId, MedicalEventEntity medicalEvent)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar el evento médico con id = {0}", medicalEventId);

		if (medicalEventId == null || medicalEventId <= 0)
			throw new IllegalOperationException("Medical event id is not valid");

		Optional<MedicalEventEntity> current = medicalEventRepository.findById(medicalEventId);
		if (current.isEmpty())
			throw new EntityNotFoundException("Medical event was not found");

		if (medicalEvent.getDate() == null)
			throw new IllegalOperationException("Date is not valid");

		if (medicalEvent.getDate().after(new Date()))
			throw new IllegalOperationException("Date cannot be in the future");

		medicalEvent.setId(medicalEventId);
		medicalEvent.setPet(current.get().getPet());

		log.info("Termina proceso de actualizar el evento médico con id = {0}", medicalEventId);
		return medicalEventRepository.save(medicalEvent);
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
		log.info("Inicia proceso de borrar el evento médico con id = {0}", medicalEventId);

		if (medicalEventId == null || medicalEventId <= 0)
			throw new IllegalOperationException("Medical event id is not valid");

		Optional<MedicalEventEntity> medicalEventEntity = medicalEventRepository.findById(medicalEventId);
		if (medicalEventEntity.isEmpty())
			throw new EntityNotFoundException("Medical event was not found");

		medicalEventRepository.deleteById(medicalEventId);
		log.info("Termina proceso de borrar el evento médico con id = {0}", medicalEventId);
	}
}
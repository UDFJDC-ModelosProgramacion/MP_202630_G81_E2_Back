package co.edu.udistrital.mdp.ZZZ.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.ZZZ.entities.PetEntity;
import co.edu.udistrital.mdp.ZZZ.entities.VaccinationRecordEntity;
import co.edu.udistrital.mdp.ZZZ.entities.VaccineEntity;
import co.edu.udistrital.mdp.ZZZ.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.ZZZ.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.ZZZ.repositories.PetRepository;
import co.edu.udistrital.mdp.ZZZ.repositories.VaccinationRecordRepository;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class VaccinationRecordService {

	private static final String FINALIZED_STATUS = "FINALIZED";

	@Autowired
	VaccinationRecordRepository vaccinationRecordRepository;

	@Autowired
	PetRepository petRepository;

	/**
	 * Crea un nuevo registro de vacunación.
	 */
	@Transactional
	public VaccinationRecordEntity createVaccinationRecord(VaccinationRecordEntity record)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de creación del registro de vacunación");

		if (record.getPet() == null || record.getPet().getId() == null)
			throw new IllegalOperationException("Vaccination record must be associated with an existing pet");

		Optional<PetEntity> pet = petRepository.findById(record.getPet().getId());
		if (pet.isEmpty())
			throw new EntityNotFoundException("Pet not found");

		boolean petAlreadyHasRecord = vaccinationRecordRepository.findAll().stream()
				.anyMatch(r -> r.getPet() != null && r.getPet().getId().equals(pet.get().getId()));
		if (petAlreadyHasRecord)
			throw new IllegalOperationException("This pet already has a vaccination record");

		record.setPet(pet.get());

		log.info("Termina proceso de creación del registro de vacunación");
		return vaccinationRecordRepository.save(record);
	}

	/**
	 * Obtiene todos los registros de vacunación.
	 */
	@Transactional
	public List<VaccinationRecordEntity> getVaccinationRecords() {
		log.info("Inicia proceso de consultar todos los registros de vacunación");
		List<VaccinationRecordEntity> records = vaccinationRecordRepository.findAll();
		if (records.isEmpty())
			log.info("No hay registros de vacunación registrados");
		return records;
	}

	/**
	 * Obtiene el registro de vacunación asociado a una mascota específica.
	 */
	@Transactional
	public VaccinationRecordEntity getVaccinationRecordByPet(Long petId) throws EntityNotFoundException {
		log.info("Inicia proceso de consultar el registro de vacunación de la mascota con id = {}", petId);
		Optional<VaccinationRecordEntity> record = vaccinationRecordRepository.findAll().stream()
				.filter(r -> r.getPet() != null && r.getPet().getId().equals(petId))
				.findFirst();
		if (record.isEmpty())
			throw new EntityNotFoundException("Vaccination record not found for that pet");
		return record.get();
	}

	/**
	 * Obtiene un registro de vacunación a partir de su id.
	 */
	@Transactional
	public VaccinationRecordEntity getVaccinationRecord(Long recordId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de consultar el registro de vacunación con id = {}", recordId);
		if (recordId == null || recordId <= 0)
			throw new IllegalOperationException("Vaccination record id is not valid");

		Optional<VaccinationRecordEntity> record = vaccinationRecordRepository.findById(recordId);
		if (record.isEmpty())
			throw new EntityNotFoundException("Vaccination record not found");

		log.info("Termina proceso de consultar el registro de vacunación con id = {}", recordId);
		return record.get();
	}

	/**
	 * Actualiza un registro de vacunación existente.
	 */
	@Transactional
	public VaccinationRecordEntity updateVaccinationRecord(Long recordId, VaccinationRecordEntity record)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de actualizar el registro de vacunación con id = {}", recordId);
		if (recordId == null || recordId <= 0)
			throw new IllegalOperationException("Vaccination record id is not valid");

		Optional<VaccinationRecordEntity> existing = vaccinationRecordRepository.findById(recordId);
		if (existing.isEmpty())
			throw new EntityNotFoundException("Vaccination record not found");

		VaccinationRecordEntity current = existing.get();

		if (record.getPet() != null && current.getPet() != null
				&& !record.getPet().getId().equals(current.getPet().getId()))
			throw new IllegalOperationException("The vaccination record cannot be reassigned to a different pet");

		record.setId(recordId);
		record.setPet(current.getPet());

		log.info("Termina proceso de actualizar el registro de vacunación con id = {}", recordId);
		return vaccinationRecordRepository.save(record);
	}

	/**
	 * Agrega una vacuna a un registro de vacunación existente.
	 */
	@Transactional
	public VaccinationRecordEntity addVaccine(Long recordId, VaccineEntity vaccine)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de agregar una vacuna al registro con id = {}", recordId);
		if (recordId == null || recordId <= 0)
			throw new IllegalOperationException("Vaccination record id is not valid");

		Optional<VaccinationRecordEntity> existing = vaccinationRecordRepository.findById(recordId);
		if (existing.isEmpty())
			throw new EntityNotFoundException("Vaccination record not found");

		if (vaccine == null)
			throw new IllegalOperationException("Vaccine cannot be null");

		VaccinationRecordEntity current = existing.get();
		vaccine.setVaccinationRecord(current);
		current.getVaccines().add(vaccine);

		log.info("Termina proceso de agregar una vacuna al registro con id = {}", recordId);
		return vaccinationRecordRepository.save(current);
	}

	/**
	 * Borra un registro de vacunación a partir de su id.
	 */
	@Transactional
	public void deleteVaccinationRecord(Long recordId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Inicia proceso de borrar el registro de vacunación con id = {}", recordId);
		if (recordId == null || recordId <= 0)
			throw new IllegalOperationException("Vaccination record id is not valid");

		Optional<VaccinationRecordEntity> record = vaccinationRecordRepository.findById(recordId);
		if (record.isEmpty())
			throw new EntityNotFoundException("Vaccination record not found");

		PetEntity pet = record.get().getPet();
		if (pet != null && isPetActiveInShelter(pet))
			throw new IllegalOperationException(
					"Vaccination record cannot be deleted while the pet is still active in the shelter");

		// relación vaccines, sus vacunas asociadas se borran en cascada.
		vaccinationRecordRepository.deleteById(recordId);
		log.info("Termina proceso de borrar el registro de vacunación con id = {}", recordId);
	}

	private boolean isPetActiveInShelter(PetEntity pet) {
		if (pet.getAdoptions() == null)
			return true;
		return pet.getAdoptions().stream()
				.noneMatch(a -> FINALIZED_STATUS.equalsIgnoreCase(a.getStatus()));
	}
}

package co.edu.udistrital.mdp.pets.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.VaccinationRecordEntity;
import co.edu.udistrital.mdp.pets.entities.VaccineEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.VaccinationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor 
public class VaccinationRecordService {

	private static final String FINALIZED_STATUS = "FINALIZED";
	private static final String VACCINATION_RECORD_ID_NOT_VALID = "Vaccination record id is not valid";
	private static final String VACCINATION_RECORD_NOT_FOUND = "Vaccination record not found";

	private final VaccinationRecordRepository vaccinationRecordRepository;
	private final PetRepository petRepository;

	/**
	 * Crea un nuevo registro de vacunación.
	 */
	@Transactional
	public VaccinationRecordEntity createVaccinationRecord(VaccinationRecordEntity vaccinationRecord)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Starting vaccination record creation process");

		if (vaccinationRecord.getPet() == null || vaccinationRecord.getPet().getId() == null)
			throw new IllegalOperationException("Vaccination record must be associated with an existing pet");

		Optional<PetEntity> pet = petRepository.findById(vaccinationRecord.getPet().getId());
		if (pet.isEmpty())
			throw new EntityNotFoundException("Pet not found");

		boolean petAlreadyHasRecord = vaccinationRecordRepository.findAll().stream()
				.anyMatch(r -> r.getPet() != null && r.getPet().getId().equals(pet.get().getId()));
		if (petAlreadyHasRecord)
			throw new IllegalOperationException("This pet already has a vaccination record");

		vaccinationRecord.setPet(pet.get());

		log.info("Ending vaccination record creation process");
		return vaccinationRecordRepository.save(vaccinationRecord);
	}

	/**
	 * Obtiene todos los registros de vacunación.
	 */
	@Transactional
	public List<VaccinationRecordEntity> getVaccinationRecords() {
		log.info("Starting process to consult all vaccination records");
		List<VaccinationRecordEntity> records = vaccinationRecordRepository.findAll();
		if (records.isEmpty())
			log.info("No vaccination records found");
		return records;
	}

	/**
	 * Obtiene el registro de vacunación asociado a una mascota específica.
	 */
	@Transactional
	public VaccinationRecordEntity getVaccinationRecordByPet(Long petId) throws EntityNotFoundException {
		log.info("Starting process to consult vaccination record for pet with id = {}", petId);
		Optional<VaccinationRecordEntity> vaccinationRecord = vaccinationRecordRepository.findAll().stream()
				.filter(r -> r.getPet() != null && r.getPet().getId().equals(petId))
				.findFirst();
		if (vaccinationRecord.isEmpty())
			throw new EntityNotFoundException("Vaccination record not found for that pet");
		return vaccinationRecord.get();
	}

	/**
	 * Obtiene un registro de vacunación a partir de su id.
	 */
	@Transactional
	public VaccinationRecordEntity getVaccinationRecord(Long recordId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Starting process to consult vaccination record with id = {}", recordId);
		if (recordId == null || recordId <= 0)
			throw new IllegalOperationException(VACCINATION_RECORD_ID_NOT_VALID);

		Optional<VaccinationRecordEntity> vaccinationRecord = vaccinationRecordRepository.findById(recordId);
		if (vaccinationRecord.isEmpty())
			throw new EntityNotFoundException(VACCINATION_RECORD_NOT_FOUND);

		log.info("Ending process to consult vaccination record with id = {}", recordId);
		return vaccinationRecord.get();
	}

	/**
	 * Actualiza un registro de vacunación existente.
	 */
	@Transactional
	public VaccinationRecordEntity updateVaccinationRecord(Long recordId, VaccinationRecordEntity vaccinationRecord)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Starting process to update vaccination record with id = {}", recordId);
		if (recordId == null || recordId <= 0)
			throw new IllegalOperationException(VACCINATION_RECORD_ID_NOT_VALID);

		Optional<VaccinationRecordEntity> existing = vaccinationRecordRepository.findById(recordId);
		if (existing.isEmpty())
			throw new EntityNotFoundException(VACCINATION_RECORD_NOT_FOUND);

		VaccinationRecordEntity current = existing.get();

		if (vaccinationRecord.getPet() != null && current.getPet() != null
				&& !vaccinationRecord.getPet().getId().equals(current.getPet().getId()))
			throw new IllegalOperationException("The vaccination record cannot be reassigned to a different pet");

		vaccinationRecord.setId(recordId);
		vaccinationRecord.setPet(current.getPet());

		log.info("Ending process to update vaccination record with id = {}", recordId);
		return vaccinationRecordRepository.save(vaccinationRecord);
	}

	/**
	 * Agrega una vacuna a un registro de vacunación existente.
	 */
	@Transactional
	public VaccinationRecordEntity addVaccine(Long recordId, VaccineEntity vaccine)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Starting process to add vaccine to vaccination record with id = {}", recordId);
		if (recordId == null || recordId <= 0)
			throw new IllegalOperationException(VACCINATION_RECORD_ID_NOT_VALID);

		Optional<VaccinationRecordEntity> existing = vaccinationRecordRepository.findById(recordId);
		if (existing.isEmpty())
			throw new EntityNotFoundException(VACCINATION_RECORD_NOT_FOUND);

		if (vaccine == null)
			throw new IllegalOperationException("Vaccine cannot be null");

		VaccinationRecordEntity current = existing.get();
		vaccine.setVaccinationRecord(current);
		current.getVaccines().add(vaccine);

		log.info("Ending process to add vaccine to vaccination record with id = {}", recordId);
		return vaccinationRecordRepository.save(current);
	}

	/**
	 * Borra un registro de vacunación a partir de su id.
	 */
	@Transactional
	public void deleteVaccinationRecord(Long recordId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Starting process to delete vaccination record with id = {}", recordId);
		if (recordId == null || recordId <= 0)
			throw new IllegalOperationException(VACCINATION_RECORD_ID_NOT_VALID);

		Optional<VaccinationRecordEntity> vaccinationRecord = vaccinationRecordRepository.findById(recordId);
		if (vaccinationRecord.isEmpty())
			throw new EntityNotFoundException(VACCINATION_RECORD_NOT_FOUND);

		PetEntity pet = vaccinationRecord.get().getPet();
		if (pet != null && isPetActiveInShelter(pet))
			throw new IllegalOperationException(
					"Vaccination record cannot be deleted while the pet is still active in the shelter");

		// relación vaccines, sus vacunas asociadas se borran en cascada.
		vaccinationRecordRepository.deleteById(recordId);
		log.info("Ending process to delete vaccination record with id = {}", recordId);
	}

	private boolean isPetActiveInShelter(PetEntity pet) {
		if (pet.getAdoptions() == null)
			return true;
		return pet.getAdoptions().stream()
				.noneMatch(a -> FINALIZED_STATUS.equalsIgnoreCase(a.getStatus()));
	}
}

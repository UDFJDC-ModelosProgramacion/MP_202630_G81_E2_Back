package co.edu.udistrital.mdp.pets.services;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.VaccinationRecordEntity;
import co.edu.udistrital.mdp.pets.entities.VaccineEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.VaccinationRecordRepository;
import co.edu.udistrital.mdp.pets.repositories.VaccineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VaccineService {

	private static final String FINALIZED_STATUS = "FINALIZED";
	private static final String VACCINE_ID_NOT_VALID = "Vaccine id is not valid";
	private static final String VACCINE_NOT_FOUND = "Vaccine not found";
	private static final String VACCINATION_RECORD_NOT_FOUND = "Vaccination record not found";
	private static final String VACCINE_NOT_ASSOCIATED_TO_RECORD = "The vaccine is not associated with the vaccination record";

	private final VaccineRepository vaccineRepository;
	private final VaccinationRecordRepository vaccinationRecordRepository;
	private final AdoptionRepository adoptionRepository;

	@Transactional
	public VaccineEntity createVaccine(Long vaccinationRecordId, VaccineEntity vaccine)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Vaccine creation process begins for vaccination record with id = {}", vaccinationRecordId);

		VaccinationRecordEntity vaccinationRecord = findVaccinationRecordOrThrow(vaccinationRecordId);

		validateMandatoryAttributes(vaccine);

		if (vaccine.getAdministrationDate().after(new Date()))
			throw new IllegalOperationException("Administration date cannot be a future date");

		if (vaccine.getNextAdministration() != null
				&& !vaccine.getNextAdministration().after(vaccine.getAdministrationDate()))
			throw new IllegalOperationException("Next administration must be a date later than administration date");

		vaccine.setVaccinationRecord(vaccinationRecord);

		boolean duplicated = vaccineRepository.findAll().stream()
				.filter(v -> v.getVaccinationRecord() != null
						&& v.getVaccinationRecord().getId().equals(vaccinationRecordId))
				.anyMatch(v -> v.getName() != null && v.getName().equalsIgnoreCase(vaccine.getName())
						&& v.getAdministrationDate() != null
						&& sameDay(v.getAdministrationDate(), vaccine.getAdministrationDate()));
		if (duplicated)
			throw new IllegalOperationException(
					"This vaccination record already has a vaccine with the same name and administration date");

		log.info("Vaccine creation process ends");
		return vaccineRepository.save(vaccine);
	}

	@Transactional
	public List<VaccineEntity> getVaccines(Long vaccinationRecordId) throws EntityNotFoundException, IllegalOperationException{
		log.info("The process of consulting vaccines for vaccination record with id = {} begins", vaccinationRecordId);

		findVaccinationRecordOrThrow(vaccinationRecordId);

		List<VaccineEntity> vaccines = vaccineRepository.findAll().stream()
				.filter(v -> v.getVaccinationRecord() != null
						&& vaccinationRecordId.equals(v.getVaccinationRecord().getId()))
				.toList();

		vaccines.forEach(this::refreshStatus);

		if (vaccines.isEmpty())
			log.info("There are no registered vaccines for this vaccination record");

		return vaccines;
	}

	@Transactional
	public VaccineEntity getVaccine(Long vaccinationRecordId, Long vaccineId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("The process of consulting vaccine with id = {} of vaccination record with id = {} begins",
				vaccineId, vaccinationRecordId);

		findVaccinationRecordOrThrow(vaccinationRecordId);
		VaccineEntity vaccine = findVaccineOrThrow(vaccineId);
		validateBelongsToRecord(vaccine, vaccinationRecordId);

		refreshStatus(vaccine);

		log.info("The process of consulting vaccine with id = {} of vaccination record with id = {} ends",
				vaccineId, vaccinationRecordId);
		return vaccine;
	}

	@Transactional
	public VaccineEntity updateVaccine(Long vaccinationRecordId, Long vaccineId, VaccineEntity vaccine)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("The process of updating vaccine with id = {} of vaccination record with id = {} begins",
				vaccineId, vaccinationRecordId);

		findVaccinationRecordOrThrow(vaccinationRecordId);
		VaccineEntity current = findVaccineOrThrow(vaccineId);
		validateBelongsToRecord(current, vaccinationRecordId);

		if (vaccine.getName() == null || vaccine.getName().isBlank())
			throw new IllegalOperationException("Name cannot be null or empty");
		if (vaccine.getStatus() == null)
			throw new IllegalOperationException("Status cannot be null");

		if (vaccine.getAdministrationDate() != null
				&& !sameDay(vaccine.getAdministrationDate(), current.getAdministrationDate()))
			throw new IllegalOperationException(
					"Administration date cannot be modified once the vaccine has been created");

		if (vaccine.getNextAdministration() != null
				&& !vaccine.getNextAdministration().after(current.getAdministrationDate()))
			throw new IllegalOperationException("Next administration must remain later than administration date");

		vaccine.setId(vaccineId);
		vaccine.setAdministrationDate(current.getAdministrationDate());
		vaccine.setVaccinationRecord(current.getVaccinationRecord());

		VaccineEntity updated = vaccineRepository.save(vaccine);
		refreshStatus(updated);

		log.info("The process of updating vaccine with id = {} of vaccination record with id = {} ends",
				vaccineId, vaccinationRecordId);
		return updated;
	}

	@Transactional
	public void deleteVaccine(Long vaccinationRecordId, Long vaccineId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("Start the process of deleting vaccine with id = {} of vaccination record with id = {}",
				vaccineId, vaccinationRecordId);

		findVaccinationRecordOrThrow(vaccinationRecordId);
		VaccineEntity vaccine = findVaccineOrThrow(vaccineId);
		validateBelongsToRecord(vaccine, vaccinationRecordId);

		PetEntity pet = vaccine.getVaccinationRecord() != null ? vaccine.getVaccinationRecord().getPet() : null;

		boolean petAlreadyAdopted = pet != null && adoptionRepository.findAll().stream()
				.filter(a -> a.getPet() != null && a.getPet().getId().equals(pet.getId()))
				.anyMatch(a -> FINALIZED_STATUS.equalsIgnoreCase(a.getStatus()));
		if (petAlreadyAdopted)
			throw new IllegalOperationException(
					"A vaccine that is part of the medical history of an already adopted pet cannot be deleted");

		vaccineRepository.deleteById(vaccineId);
		log.info("Finish the process of deleting vaccine with id = {} of vaccination record with id = {}",
				vaccineId, vaccinationRecordId);
	}

	private VaccinationRecordEntity findVaccinationRecordOrThrow(Long vaccinationRecordId)
			throws EntityNotFoundException, IllegalOperationException {
		if (vaccinationRecordId == null || vaccinationRecordId <= 0)
			throw new IllegalOperationException("Vaccination record id is not valid");

		Optional<VaccinationRecordEntity> vaccinationRecord = vaccinationRecordRepository.findById(vaccinationRecordId);
		if (vaccinationRecord.isEmpty())
			throw new EntityNotFoundException(VACCINATION_RECORD_NOT_FOUND);

		return vaccinationRecord.get();
	}

	private VaccineEntity findVaccineOrThrow(Long vaccineId) throws EntityNotFoundException, IllegalOperationException {
		if (vaccineId == null || vaccineId <= 0)
			throw new IllegalOperationException(VACCINE_ID_NOT_VALID);

		Optional<VaccineEntity> vaccine = vaccineRepository.findById(vaccineId);
		if (vaccine.isEmpty())
			throw new EntityNotFoundException(VACCINE_NOT_FOUND);

		return vaccine.get();
	}

	private void validateBelongsToRecord(VaccineEntity vaccine, Long vaccinationRecordId) throws IllegalOperationException {
		if (vaccine.getVaccinationRecord() == null || !vaccine.getVaccinationRecord().getId().equals(vaccinationRecordId))
			throw new IllegalOperationException(VACCINE_NOT_ASSOCIATED_TO_RECORD);
	}

	// --- Reglas de negocio existentes (sin cambios) ---

	private void validateMandatoryAttributes(VaccineEntity vaccine) throws IllegalOperationException {
		if (vaccine.getName() == null || vaccine.getName().isBlank())
			throw new IllegalOperationException("Name cannot be null or empty");
		if (vaccine.getAdministrationDate() == null)
			throw new IllegalOperationException("Administration date cannot be null");
		if (vaccine.getStatus() == null)
			throw new IllegalOperationException("Status must be explicitly initialized");
	}

	private boolean sameDay(Date d1, Date d2) {
		if (d1 == null || d2 == null)
			return false;
		return truncate(d1).equals(truncate(d2));
	}

	private Date truncate(Date date) {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.setTime(date);
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		return c.getTime();
	}

	private void refreshStatus(VaccineEntity vaccine) {
		if (Boolean.TRUE.equals(vaccine.getStatus()) && vaccine.getNextAdministration() != null
				&& vaccine.getNextAdministration().before(new Date())) {
			vaccine.setStatus(false);
			vaccineRepository.save(vaccine);
		}
	}
}
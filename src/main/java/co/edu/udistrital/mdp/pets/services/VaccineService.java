package co.edu.udistrital.mdp.pets.services;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VaccineService {

	private static final String FINALIZED_STATUS = "FINALIZED";

	@Autowired
	VaccineRepository vaccineRepository;

	@Autowired
	VaccinationRecordRepository vaccinationRecordRepository;

	@Autowired 
	AdoptionRepository adoptionRepository;

	@Transactional
	public VaccineEntity createVaccine(VaccineEntity vaccine) throws EntityNotFoundException, IllegalOperationException {
		log.info("Vaccine creation process begins");

		if (vaccine.getName() == null || vaccine.getName().isBlank())
			throw new IllegalOperationException("Name cannot be null or empty");
		if (vaccine.getAdministrationDate() == null)
			throw new IllegalOperationException("Administration date cannot be null");
		if (vaccine.getStatus() == null)
			throw new IllegalOperationException("Status must be explicitly initialized");

		if (vaccine.getAdministrationDate().after(new Date()))
			throw new IllegalOperationException("Administration date cannot be a future date");

		if (vaccine.getNextAdministration() != null
				&& !vaccine.getNextAdministration().after(vaccine.getAdministrationDate()))
			throw new IllegalOperationException("Next administration must be a date later than administration date");

		if (vaccine.getVaccinationRecord() == null || vaccine.getVaccinationRecord().getId() == null)
			throw new IllegalOperationException("Vaccine must be associated with an existing vaccination record");
		Optional<VaccinationRecordEntity> record = vaccinationRecordRepository
				.findById(vaccine.getVaccinationRecord().getId());
		if (record.isEmpty())
			throw new EntityNotFoundException("Vaccination record not found");

		vaccine.setVaccinationRecord(record.get());

		boolean duplicated = vaccineRepository.findAll().stream()
				.filter(v -> v.getVaccinationRecord() != null
						&& v.getVaccinationRecord().getId().equals(record.get().getId()))
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
	public List<VaccineEntity> getVaccines() {
		log.info("The process of consulting all vaccines begins");
		List<VaccineEntity> vaccines = vaccineRepository.findAll();
		vaccines.forEach(this::refreshStatus);
		if (vaccines.isEmpty())
			log.info("There are no registered vaccines");
		return vaccines;
	}

	@Transactional
	public List<VaccineEntity> getVaccines(Long vaccinationRecordId, Long petId, Boolean status) {
		log.info("Process of consulting leaked vaccines begins");
		List<VaccineEntity> vaccines = vaccineRepository.findAll().stream()
				.filter(v -> vaccinationRecordId == null
						|| (v.getVaccinationRecord() != null && vaccinationRecordId.equals(v.getVaccinationRecord().getId())))
				.filter(v -> petId == null || (v.getVaccinationRecord() != null
						&& v.getVaccinationRecord().getPet() != null
						&& petId.equals(v.getVaccinationRecord().getPet().getId())))
				.map(v -> {
					refreshStatus(v);
					return v;
				})
				.filter(v -> status == null || status.equals(v.getStatus()))
				.toList();
		if (vaccines.isEmpty())
			log.info("There are no registered vaccines that meet the filters");
		return vaccines;
	}

	@Transactional
	public VaccineEntity getVaccine(Long vaccineId) throws EntityNotFoundException, IllegalOperationException {
		return getVaccine(vaccineId, null, null);
	}

	@Transactional
	public VaccineEntity getVaccine(Long vaccineId, String name, Long vaccinationRecordId)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("The process of consulting a vaccine through filters begins");

		if (vaccineId != null && vaccineId <= 0)
			throw new IllegalOperationException("Vaccine id is not valid");

		Optional<VaccineEntity> vaccine = vaccineRepository.findAll().stream()
				.filter(v -> vaccineId == null || vaccineId.equals(v.getId()))
				.filter(v -> name == null || name.equalsIgnoreCase(v.getName()))
				.filter(v -> vaccinationRecordId == null || (v.getVaccinationRecord() != null
						&& vaccinationRecordId.equals(v.getVaccinationRecord().getId())))
				.findFirst();

		if (vaccine.isEmpty())
			throw new EntityNotFoundException("Vaccine not found");

		refreshStatus(vaccine.get());

		log.info("Termina proceso de consultar una vacuna por filtros");
		return vaccine.get();
	}

	@Transactional
	public VaccineEntity updateVaccine(Long vaccineId, VaccineEntity vaccine)
			throws EntityNotFoundException, IllegalOperationException {
		log.info("The process of updating the vaccine begins with id = {}", vaccineId);
		if (vaccineId == null || vaccineId <= 0)
			throw new IllegalOperationException("Vaccine id is not valid");

		Optional<VaccineEntity> existing = vaccineRepository.findById(vaccineId);
		if (existing.isEmpty())
			throw new EntityNotFoundException("Vaccine not found");

		if (vaccine.getName() == null || vaccine.getName().isBlank())
			throw new IllegalOperationException("Name cannot be null or empty");
		if (vaccine.getStatus() == null)
			throw new IllegalOperationException("Status cannot be null");

		VaccineEntity current = existing.get();

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

		log.info("The process of updating the vaccine with id = {} ends", vaccineId);
		VaccineEntity updated = vaccineRepository.save(vaccine);
		refreshStatus(updated);
		return updated;
	}

	@Transactional
	public void deleteVaccine(Long vaccineId) throws EntityNotFoundException, IllegalOperationException {
		log.info("Start the process of deleting the vaccine with id = {}", vaccineId);
		if (vaccineId == null || vaccineId <= 0)
			throw new IllegalOperationException("Vaccine id is not valid");

		Optional<VaccineEntity> vaccine = vaccineRepository.findById(vaccineId);
		if (vaccine.isEmpty())
			throw new EntityNotFoundException("Vaccine not found");

		PetEntity pet = vaccine.get().getVaccinationRecord() != null
				? vaccine.get().getVaccinationRecord().getPet()
				: null;

		boolean petAlreadyAdopted = pet != null && adoptionRepository.findAll().stream()
				.filter(a -> a.getPet() != null && a.getPet().getId().equals(pet.getId()))
				.anyMatch(a -> FINALIZED_STATUS.equalsIgnoreCase(a.getStatus()));
		if (petAlreadyAdopted)
			throw new IllegalOperationException(
					"A vaccine that is part of the medical history of an already adopted pet cannot be deleted");

		vaccineRepository.deleteById(vaccineId);
		log.info("Finish the process of deleting the vaccine with id = {}", vaccineId);
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
package co.edu.udistrital.mdp.pets.controllers;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udistrital.mdp.pets.dto.AdopterDTO;
import co.edu.udistrital.mdp.pets.dto.ShelterDTO;
import co.edu.udistrital.mdp.pets.dto.TrialCohabitationDTO;
import co.edu.udistrital.mdp.pets.dto.TrialCohabitationRequestDTO;
import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationRequestEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.TrialCohabitationService;
import lombok.RequiredArgsConstructor;

/**
 * Controlador de convivencias de prueba. Permite consultar por estado y por
 * rango de fechas, además del CRUD básico.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/trialCohabitations")
public class TrialCohabitationController {

	private final TrialCohabitationService trialCohabitationService;

	@GetMapping
	@ResponseStatus(code = HttpStatus.OK)
	public List<TrialCohabitationDTO> getTrials(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String startDate,
			@RequestParam(required = false) String endDate)
			throws EntityNotFoundException, IllegalOperationException {
		return trialCohabitationService.readAllTrials(status, parseDate(startDate, "Start date"),
				parseDate(endDate, "End date")).stream().map(this::toDTO).toList();
	}

	@GetMapping(value = "/{trialId}")
	@ResponseStatus(code = HttpStatus.OK)
	public TrialCohabitationDTO getTrial(@PathVariable Long trialId)
			throws EntityNotFoundException, IllegalOperationException {
		return toDTO(trialCohabitationService.readTrial(trialId));
	}

	@PostMapping
	@ResponseStatus(code = HttpStatus.CREATED)
	public TrialCohabitationDTO createTrial(@RequestBody TrialCohabitationDTO trialDTO)
			throws EntityNotFoundException, IllegalOperationException {
		return toDTO(trialCohabitationService.createTrial(toEntity(trialDTO)));
	}

	@PutMapping(value = "/{trialId}")
	@ResponseStatus(code = HttpStatus.OK)
	public TrialCohabitationDTO updateTrial(@PathVariable Long trialId,
			@RequestBody TrialCohabitationDTO trialDTO)
			throws EntityNotFoundException, IllegalOperationException {
		return toDTO(trialCohabitationService.updateTrial(trialId, toEntity(trialDTO)));
	}

	@DeleteMapping(value = "/{trialId}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void deleteTrial(@PathVariable Long trialId)
			throws EntityNotFoundException, IllegalOperationException {
		trialCohabitationService.deleteTrial(trialId);
	}

	/**
	 * Acepta fechas con o sin zona horaria y con o sin milisegundos, porque las
	 * colecciones envian formatos distintos (por ejemplo 2026-11-29T00:00:00-05:00).
	 */
	private Date parseDate(String value, String label) throws IllegalOperationException {
		if (value == null || value.isBlank())
			return null;
		try {
			return Date.from(OffsetDateTime.parse(value).toInstant());
		} catch (DateTimeParseException e) {
			try {
				return Date.from(LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant());
			} catch (DateTimeParseException ex) {
				throw new IllegalOperationException(label + " is not valid");
			}
		}
	}

	/** Mapeo manual: el DTO solo trae los identificadores de las entidades asociadas. */
	private TrialCohabitationEntity toEntity(TrialCohabitationDTO dto) {
		TrialCohabitationEntity trial = new TrialCohabitationEntity();
		trial.setStartDate(dto.getStartDate());
		trial.setEndDate(dto.getEndDate());
		trial.setStatus(dto.getStatus());
		trial.setObservations(dto.getObservations());
		if (dto.getAdopter() != null && dto.getAdopter().getId() != null) {
			AdopterEntity adopter = new AdopterEntity();
			adopter.setId(dto.getAdopter().getId());
			trial.setAdopter(adopter);
		}
		if (dto.getShelter() != null && dto.getShelter().getId() != null) {
			ShelterEntity shelter = new ShelterEntity();
			shelter.setId(dto.getShelter().getId());
			trial.setShelter(shelter);
		}
		if (dto.getTrialCohabitationRequest() != null && dto.getTrialCohabitationRequest().getId() != null) {
			TrialCohabitationRequestEntity request = new TrialCohabitationRequestEntity();
			request.setId(dto.getTrialCohabitationRequest().getId());
			trial.setTrialCohabitationRequest(request);
		}
		return trial;
	}

	private TrialCohabitationDTO toDTO(TrialCohabitationEntity trial) {
		TrialCohabitationDTO dto = new TrialCohabitationDTO();
		dto.setId(trial.getId());
		dto.setStartDate(trial.getStartDate());
		dto.setEndDate(trial.getEndDate());
		dto.setStatus(trial.getStatus());
		dto.setObservations(trial.getObservations());
		if (trial.getAdopter() != null) {
			AdopterDTO adopter = new AdopterDTO();
			adopter.setId(trial.getAdopter().getId());
			dto.setAdopter(adopter);
		}
		if (trial.getShelter() != null) {
			ShelterDTO shelter = new ShelterDTO();
			shelter.setId(trial.getShelter().getId());
			dto.setShelter(shelter);
		}
		if (trial.getTrialCohabitationRequest() != null) {
			TrialCohabitationRequestDTO request = new TrialCohabitationRequestDTO();
			request.setId(trial.getTrialCohabitationRequest().getId());
			dto.setTrialCohabitationRequest(request);
		}
		return dto;
	}
}

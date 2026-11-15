package co.edu.udistrital.mdp.pets.controllers;

import java.time.LocalDate;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.format.annotation.DateTimeFormat;
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

import co.edu.udistrital.mdp.pets.dto.MedicalEventDTO;
import co.edu.udistrital.mdp.pets.entities.MedicalEventEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.MedicalEventService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/medicalEvents")
public class MedicalEventController {

	private final MedicalEventService medicalEventService;
	private final ModelMapper modelMapper;

	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public List<MedicalEventDTO> findAll(
			@RequestParam(required = false) Long petId,
			@RequestParam(required = false) Long veterinarianId,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date)
			throws IllegalOperationException {
		List<MedicalEventEntity> events = medicalEventService.getMedicalEvents(petId, veterinarianId, type, date);
		return modelMapper.map(events, new TypeToken<List<MedicalEventDTO>>() {
		}.getType());
	}

	@GetMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public MedicalEventDTO findOne(@PathVariable Long id)
			throws EntityNotFoundException, IllegalOperationException {
		return modelMapper.map(medicalEventService.getMedicalEvent(id), MedicalEventDTO.class);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MedicalEventDTO create(@RequestBody MedicalEventDTO dto)
			throws EntityNotFoundException, IllegalOperationException {
		MedicalEventEntity event = medicalEventService.createMedicalEvent(modelMapper.map(dto, MedicalEventEntity.class));
		return modelMapper.map(event, MedicalEventDTO.class);
	}

	@PutMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public MedicalEventDTO update(@PathVariable Long id, @RequestBody MedicalEventDTO dto)
			throws EntityNotFoundException, IllegalOperationException {
		MedicalEventEntity event = medicalEventService.updateMedicalEvent(id,
				modelMapper.map(dto, MedicalEventEntity.class));
		return modelMapper.map(event, MedicalEventDTO.class);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
		medicalEventService.deleteMedicalEvent(id);
	}
}

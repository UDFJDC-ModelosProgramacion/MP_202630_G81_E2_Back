package co.edu.udistrital.mdp.pets.controllers;

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

import co.edu.udistrital.mdp.pets.dto.AdoptionDTO;
import co.edu.udistrital.mdp.pets.dto.FollowUpDTO;
import co.edu.udistrital.mdp.pets.dto.VeterinarianDTO;
import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.FollowUpEntity;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.FollowUpService;
import lombok.RequiredArgsConstructor;

/**
 * Controlador de seguimientos de una adopción.
 *
 * La consulta por id acepta requesterId y role para aplicar las reglas de
 * autorización del servicio: el adoptante asociado y el veterinario asignado
 * pueden consultar, y el rol ADMIN puede consultar cualquiera.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/followUps")
public class FollowUpController {

	private final FollowUpService followUpService;

	@GetMapping
	@ResponseStatus(code = HttpStatus.OK)
	public List<FollowUpDTO> getFollowUps(@RequestParam(required = false) Long adoptionId) {
		List<FollowUpEntity> followUps = adoptionId == null ? followUpService.getFollowUps()
				: followUpService.getFollowUps(adoptionId);
		return followUps.stream().map(this::toDTO).toList();
	}

	@GetMapping(value = "/{followUpId}")
	@ResponseStatus(code = HttpStatus.OK)
	public FollowUpDTO getFollowUp(@PathVariable Long followUpId,
			@RequestParam(required = false) Long requesterId, @RequestParam(required = false) String role)
			throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity followUp;
		if (role != null)
			followUp = followUpService.getFollowUp(followUpId, requesterId, role);
		else if (requesterId != null)
			followUp = followUpService.getFollowUp(followUpId, requesterId);
		else
			followUp = followUpService.getFollowUp(followUpId);
		return toDTO(followUp);
	}

	@PostMapping
	@ResponseStatus(code = HttpStatus.CREATED)
	public FollowUpDTO createFollowUp(@RequestBody FollowUpDTO followUpDTO)
			throws EntityNotFoundException, IllegalOperationException {
		return toDTO(followUpService.createFollowUp(toEntity(followUpDTO)));
	}

	@PutMapping(value = "/{followUpId}")
	@ResponseStatus(code = HttpStatus.OK)
	public FollowUpDTO updateFollowUp(@PathVariable Long followUpId, @RequestBody FollowUpDTO followUpDTO)
			throws EntityNotFoundException, IllegalOperationException {
		return toDTO(followUpService.updateFollowUp(followUpId, toEntity(followUpDTO)));
	}

	@DeleteMapping(value = "/{followUpId}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void deleteFollowUp(@PathVariable Long followUpId)
			throws EntityNotFoundException, IllegalOperationException {
		followUpService.deleteFollowUp(followUpId);
	}

	/** Mapeo manual: el DTO solo trae los identificadores de las entidades asociadas. */
	private FollowUpEntity toEntity(FollowUpDTO dto) {
		FollowUpEntity followUp = new FollowUpEntity();
		followUp.setDate(dto.getDate());
		followUp.setObservation(dto.getObservation());
		if (dto.getAdoption() != null && dto.getAdoption().getId() != null) {
			AdoptionEntity adoption = new AdoptionEntity();
			adoption.setId(dto.getAdoption().getId());
			followUp.setAdoption(adoption);
		}
		if (dto.getVeterinarian() != null && dto.getVeterinarian().getId() != null) {
			VeterinarianEntity veterinarian = new VeterinarianEntity();
			veterinarian.setId(dto.getVeterinarian().getId());
			followUp.setVeterinarian(veterinarian);
		}
		return followUp;
	}

	private FollowUpDTO toDTO(FollowUpEntity followUp) {
		FollowUpDTO dto = new FollowUpDTO();
		dto.setId(followUp.getId());
		dto.setDate(followUp.getDate());
		dto.setObservation(followUp.getObservation());
		if (followUp.getAdoption() != null) {
			AdoptionDTO adoption = new AdoptionDTO();
			adoption.setId(followUp.getAdoption().getId());
			dto.setAdoption(adoption);
		}
		if (followUp.getVeterinarian() != null) {
			VeterinarianDTO veterinarian = new VeterinarianDTO();
			veterinarian.setId(followUp.getVeterinarian().getId());
			dto.setVeterinarian(veterinarian);
		}
		return dto;
	}
}

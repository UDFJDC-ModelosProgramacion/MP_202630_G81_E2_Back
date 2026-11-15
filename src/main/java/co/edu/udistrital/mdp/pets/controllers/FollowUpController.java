package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

import co.edu.udistrital.mdp.pets.dto.FollowUpDTO;
import co.edu.udistrital.mdp.pets.dto.FollowUpRequestDTO;
import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.FollowUpEntity;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.FollowUpService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/adoptions/{adoptionId}/followUps")
public class FollowUpController {

	private final FollowUpService followUpService;
	private final ModelMapper modelMapper;

	@GetMapping
	@PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessFollowUps(authentication, #adoptionId)")
	@ResponseStatus(HttpStatus.OK)
	public List<FollowUpDTO> findAll(@PathVariable Long adoptionId)
			throws EntityNotFoundException, IllegalOperationException {
		List<FollowUpEntity> followUps = followUpService.getFollowUpsForAdoption(adoptionId);
		return modelMapper.map(followUps, new TypeToken<List<FollowUpDTO>>() {
		}.getType());
	}

	@GetMapping("/{followUpId}")
	@PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessFollowUp(authentication, #adoptionId, #followUpId)")
	@ResponseStatus(HttpStatus.OK)
	public FollowUpDTO findOne(@PathVariable Long adoptionId, @PathVariable Long followUpId,
			@RequestParam(required = false) Long requesterId,
			@RequestParam(required = false) String role)
			throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity followUp = followUpService.getFollowUpForAdoption(adoptionId, followUpId);
		return modelMapper.map(followUp, FollowUpDTO.class);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN') or @authorizationService.canCreateFollowUp(authentication, #adoptionId, #dto)")
	@ResponseStatus(HttpStatus.CREATED)
	public FollowUpDTO create(@PathVariable Long adoptionId, @RequestBody FollowUpRequestDTO dto)
			throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity followUp = toEntity(dto, adoptionId);
		return modelMapper.map(followUpService.createFollowUp(followUp), FollowUpDTO.class);
	}

	@PutMapping("/{followUpId}")
	@PreAuthorize("hasRole('ADMIN') or @authorizationService.canUpdateFollowUp(authentication, #adoptionId, #followUpId, #dto)")
	@ResponseStatus(HttpStatus.OK)
	public FollowUpDTO update(@PathVariable Long adoptionId, @PathVariable Long followUpId,
			@RequestBody FollowUpRequestDTO dto)
			throws EntityNotFoundException, IllegalOperationException {
		FollowUpEntity current = followUpService.getFollowUpForAdoption(adoptionId, followUpId);
		Long requestedVeterinarianId = veterinarianId(dto);
		if (requestedVeterinarianId != null && (current.getVeterinarian() == null
				|| !requestedVeterinarianId.equals(current.getVeterinarian().getId())))
			throw new IllegalOperationException("The veterinarian associated to the follow-up cannot be changed");

		FollowUpEntity update = toEntity(dto, adoptionId);
		return modelMapper.map(followUpService.updateFollowUp(followUpId, update), FollowUpDTO.class);
	}

	@DeleteMapping("/{followUpId}")
	@PreAuthorize("hasRole('ADMIN') or @authorizationService.canManageFollowUp(authentication, #adoptionId, #followUpId)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long adoptionId, @PathVariable Long followUpId)
			throws EntityNotFoundException, IllegalOperationException {
		followUpService.getFollowUpForAdoption(adoptionId, followUpId);
		followUpService.deleteFollowUp(followUpId);
	}

	private FollowUpEntity toEntity(FollowUpRequestDTO dto, Long adoptionId) throws IllegalOperationException {
		Long requestedAdoptionId = adoptionId(dto);
		if (requestedAdoptionId != null && !requestedAdoptionId.equals(adoptionId))
			throw new IllegalOperationException("The adoption in the request must match the adoption in the path");
		Long veterinarianId = veterinarianId(dto);
		FollowUpEntity followUp = new FollowUpEntity();
		followUp.setDate(dto.getDate());
		followUp.setObservation(dto.getObservation());
		if (veterinarianId != null) {
			VeterinarianEntity veterinarian = new VeterinarianEntity();
			veterinarian.setId(veterinarianId);
			followUp.setVeterinarian(veterinarian);
		}
		AdoptionEntity adoption = new AdoptionEntity();
		adoption.setId(adoptionId);
		followUp.setAdoption(adoption);
		return followUp;
	}

	private Long adoptionId(FollowUpRequestDTO dto) throws IllegalOperationException {
		Long nestedId = dto.getAdoption() == null ? null : dto.getAdoption().getId();
		Long directId = dto.getAdoptionId();
		if (nestedId != null && directId != null && !nestedId.equals(directId))
			throw new IllegalOperationException("Conflicting adoption identifiers were provided");
		return directId == null ? nestedId : directId;
	}

	private Long veterinarianId(FollowUpRequestDTO dto) throws IllegalOperationException {
		Long nestedId = dto.getVeterinarian() == null ? null : dto.getVeterinarian().getId();
		Long directId = dto.getVeterinarianId();
		if (nestedId != null && directId != null && !nestedId.equals(directId))
			throw new IllegalOperationException("Conflicting veterinarian identifiers were provided");
		return directId == null ? nestedId : directId;
	}
}

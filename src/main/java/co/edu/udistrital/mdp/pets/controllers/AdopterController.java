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

import co.edu.udistrital.mdp.pets.dto.AdopterDTO;
import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.AdopterService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/adopters")
public class AdopterController {

	private final AdopterService adopterService;
	private final ModelMapper modelMapper;

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.OK)
	public List<AdopterDTO> findAll(
			@RequestParam(required = false) String nationalId,
			@RequestParam(required = false) String housingType,
			@RequestParam(required = false) String occupation) throws IllegalOperationException {
		List<AdopterEntity> adopters = adopterService.readAllAdopters(nationalId, housingType, occupation);
		return modelMapper.map(adopters, new TypeToken<List<AdopterDTO>>() {
		}.getType());
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessAdopter(authentication, #id)")
	@ResponseStatus(HttpStatus.OK)
	public AdopterDTO findOne(@PathVariable Long id)
			throws EntityNotFoundException, IllegalOperationException {
		return modelMapper.map(adopterService.readAdopter(id), AdopterDTO.class);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AdopterDTO create(@RequestBody AdopterDTO dto) throws IllegalOperationException {
		AdopterEntity adopter = adopterService.createAdopter(modelMapper.map(dto, AdopterEntity.class));
		return modelMapper.map(adopter, AdopterDTO.class);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessAdopter(authentication, #id)")
	@ResponseStatus(HttpStatus.OK)
	public AdopterDTO update(@PathVariable Long id, @RequestBody AdopterDTO dto)
			throws EntityNotFoundException, IllegalOperationException {
		AdopterEntity adopter = adopterService.updateAdopter(id, modelMapper.map(dto, AdopterEntity.class));
		return modelMapper.map(adopter, AdopterDTO.class);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessAdopter(authentication, #id)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
		adopterService.deleteAdopter(id);
	}
}

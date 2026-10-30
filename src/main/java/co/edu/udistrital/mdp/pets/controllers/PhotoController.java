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

import co.edu.udistrital.mdp.pets.dto.PetDTO;
import co.edu.udistrital.mdp.pets.dto.PhotoDTO;
import co.edu.udistrital.mdp.pets.dto.ShelterDTO;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.PhotoEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.PhotoService;
import lombok.RequiredArgsConstructor;

/**
 * Controlador de fotografías. Las fotos pueden estar asociadas a una mascota,
 * a un refugio o a ambos.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/photos")
public class PhotoController {

	private final PhotoService photoService;

	@GetMapping
	@ResponseStatus(code = HttpStatus.OK)
	public List<PhotoDTO> getPhotos(@RequestParam(required = false) Long petId,
			@RequestParam(required = false) Long shelterId) throws EntityNotFoundException, IllegalOperationException {
		return photoService.getPhotos(petId, shelterId).stream().map(this::toDTO).toList();
	}

	@GetMapping(value = "/{photoId}")
	@ResponseStatus(code = HttpStatus.OK)
	public PhotoDTO getPhoto(@PathVariable Long photoId)
			throws EntityNotFoundException, IllegalOperationException {
		return toDTO(photoService.getPhoto(photoId));
	}

	@PostMapping
	@ResponseStatus(code = HttpStatus.CREATED)
	public PhotoDTO createPhoto(@RequestBody PhotoDTO photoDTO)
			throws EntityNotFoundException, IllegalOperationException {
		PhotoEntity created = photoService.createPhoto(toEntity(photoDTO));
		return toDTO(created);
	}

	@PutMapping(value = "/{photoId}")
	@ResponseStatus(code = HttpStatus.OK)
	public PhotoDTO updatePhoto(@PathVariable Long photoId, @RequestBody PhotoDTO photoDTO)
			throws EntityNotFoundException, IllegalOperationException {
		return toDTO(photoService.updatePhoto(photoId, toEntity(photoDTO)));
	}

	@DeleteMapping(value = "/{photoId}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void deletePhoto(@PathVariable Long photoId)
			throws EntityNotFoundException, IllegalOperationException {
		photoService.deletePhoto(photoId);
	}

	/** Mapeo manual: el DTO solo trae los identificadores de las entidades asociadas. */
	private PhotoEntity toEntity(PhotoDTO dto) {
		PhotoEntity photo = new PhotoEntity();
		photo.setUrl(dto.getUrl());
		photo.setType(dto.getType());
		photo.setDescription(dto.getDescription());
		if (dto.getPet() != null && dto.getPet().getId() != null) {
			PetEntity pet = new PetEntity();
			pet.setId(dto.getPet().getId());
			photo.setPet(pet);
		}
		if (dto.getShelter() != null && dto.getShelter().getId() != null) {
			ShelterEntity shelter = new ShelterEntity();
			shelter.setId(dto.getShelter().getId());
			photo.setShelter(shelter);
		}
		return photo;
	}

	private PhotoDTO toDTO(PhotoEntity photo) {
		PhotoDTO dto = new PhotoDTO();
		dto.setId(photo.getId());
		dto.setUrl(photo.getUrl());
		dto.setType(photo.getType());
		dto.setDescription(photo.getDescription());
		if (photo.getPet() != null) {
			PetDTO pet = new PetDTO();
			pet.setId(photo.getPet().getId());
			dto.setPet(pet);
		}
		if (photo.getShelter() != null) {
			ShelterDTO shelter = new ShelterDTO();
			shelter.setId(photo.getShelter().getId());
			dto.setShelter(shelter);
		}
		return dto;
	}
}

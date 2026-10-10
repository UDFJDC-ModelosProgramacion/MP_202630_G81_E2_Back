package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udistrital.mdp.pets.dto.PhotoDTO;
import co.edu.udistrital.mdp.pets.entities.PhotoEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.PhotoService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets/{petId}/photos")
public class PetPhotoController {

    private final PhotoService photoService;
    private final ModelMapper modelMapper;

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public PhotoDTO create(@PathVariable Long petId, @RequestBody PhotoDTO photoDTO)
            throws EntityNotFoundException, IllegalOperationException {
        PhotoEntity photo = photoService.createPetPhoto(petId, modelMapper.map(photoDTO, PhotoEntity.class));
        return modelMapper.map(photo, PhotoDTO.class);
    }

    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<PhotoDTO> findAll(@PathVariable Long petId)
            throws EntityNotFoundException, IllegalOperationException {
        List<PhotoEntity> photos = photoService.getPetPhotos(petId);
        return modelMapper.map(photos, new TypeToken<List<PhotoDTO>>() {
        }.getType());
    }

    @GetMapping("/{photoId}")
    @ResponseStatus(code = HttpStatus.OK)
    public PhotoDTO findOne(@PathVariable Long petId, @PathVariable Long photoId)
            throws EntityNotFoundException, IllegalOperationException {
        PhotoEntity photo = photoService.getPetPhoto(petId, photoId);
        return modelMapper.map(photo, PhotoDTO.class);
    }

    @PutMapping("/{photoId}")
    @ResponseStatus(code = HttpStatus.OK)
    public PhotoDTO update(@PathVariable Long petId, @PathVariable Long photoId, @RequestBody PhotoDTO photoDTO)
            throws EntityNotFoundException, IllegalOperationException {
        PhotoEntity photo = photoService.updatePetPhoto(petId, photoId,
                modelMapper.map(photoDTO, PhotoEntity.class));
        return modelMapper.map(photo, PhotoDTO.class);
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long petId, @PathVariable Long photoId)
            throws EntityNotFoundException, IllegalOperationException {
        photoService.deletePetPhoto(petId, photoId);
    }
}
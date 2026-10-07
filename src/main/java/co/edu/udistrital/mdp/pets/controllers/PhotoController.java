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
import org.springframework.web.bind.annotation.RequestParam;
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
@RequestMapping("/photos")
public class PhotoController {

    private final PhotoService photoService;
    private final ModelMapper modelMapper;

    // FindAll method with optional pet/shelter filters
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<PhotoDTO> findAll(@RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long shelterId) throws EntityNotFoundException, IllegalOperationException {
        List<PhotoEntity> photos = (petId == null && shelterId == null) ? photoService.getPhotos()
                : photoService.getPhotos(petId, shelterId);
        return modelMapper.map(photos, new TypeToken<List<PhotoDTO>>() {
        }.getType());
    }

    // findOne method
    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public PhotoDTO findOne(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        PhotoEntity photoEntity = photoService.getPhoto(id);
        return modelMapper.map(photoEntity, PhotoDTO.class);
    }

    // create method
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public PhotoDTO create(@RequestBody PhotoDTO photoDTO) throws EntityNotFoundException, IllegalOperationException {
        PhotoEntity photoEntity = photoService.createPhoto(modelMapper.map(photoDTO, PhotoEntity.class));
        return modelMapper.map(photoEntity, PhotoDTO.class);
    }

    // update method
    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public PhotoDTO update(@PathVariable Long id, @RequestBody PhotoDTO photoDTO)
            throws EntityNotFoundException, IllegalOperationException {
        PhotoEntity photoEntity = photoService.updatePhoto(id, modelMapper.map(photoDTO, PhotoEntity.class));
        return modelMapper.map(photoEntity, PhotoDTO.class);
    }

    // delete method
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        photoService.deletePhoto(id);
    }
}
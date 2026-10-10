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

import co.edu.udistrital.mdp.pets.dto.AdoptionDTO;
import co.edu.udistrital.mdp.pets.dto.AdoptionDetailDTO;
import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.AdoptionService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/adoptions")
public class AdoptionController {

    private final AdoptionService adoptionService;
    private final ModelMapper modelMapper;

    // FindAll method
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<AdoptionDetailDTO> findAll() {
        List<AdoptionEntity> adoptions = adoptionService.getAdoptions();
        return modelMapper.map(adoptions, new TypeToken<List<AdoptionDetailDTO>>() {
        }.getType());
    }

    // findOne method
    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public AdoptionDetailDTO findOne(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        AdoptionEntity adoptionEntity = adoptionService.getAdoption(id);
        return modelMapper.map(adoptionEntity, AdoptionDetailDTO.class);
    }

    // create method
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public AdoptionDTO create(@RequestBody AdoptionDTO adoptionDTO) throws IllegalOperationException, EntityNotFoundException {
        AdoptionEntity adoptionEntity = adoptionService.createAdoption(modelMapper.map(adoptionDTO, AdoptionEntity.class));
        return modelMapper.map(adoptionEntity, AdoptionDTO.class);
    }

    // update method
    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public AdoptionDTO update(@PathVariable Long id, @RequestBody AdoptionDTO adoptionDTO) throws EntityNotFoundException, IllegalOperationException {
        AdoptionEntity adoptionEntity = adoptionService.updateAdoption(id, modelMapper.map(adoptionDTO, AdoptionEntity.class));
        return modelMapper.map(adoptionEntity, AdoptionDTO.class);
    }

    // delete method
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        adoptionService.deleteAdoption(id);
    }
}
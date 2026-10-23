package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.edu.udistrital.mdp.pets.dto.AdoptionDTO;
import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.services.AdoptionService;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

@RestController
@RequestMapping("/adoptions")
public class AdoptionController {

    @Autowired
    private AdoptionService adoptionService;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<AdoptionDTO> createAdoption(@RequestBody AdoptionDTO adoptionDto) throws EntityNotFoundException, IllegalOperationException {
        AdoptionEntity entity = modelMapper.map(adoptionDto, AdoptionEntity.class);
        AdoptionEntity created = adoptionService.createAdoption(entity);
        return new ResponseEntity<>(modelMapper.map(created, AdoptionDTO.class), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdoptionDTO> getAdoption(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        AdoptionEntity entity = adoptionService.getAdoption(id);
        return new ResponseEntity<>(modelMapper.map(entity, AdoptionDTO.class), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<AdoptionDTO>> getAllAdoptions() {
        List<AdoptionEntity> list = adoptionService.getAdoptions();
        List<AdoptionDTO> dtos = list.stream().map(a -> modelMapper.map(a, AdoptionDTO.class)).collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdoptionDTO> updateAdoption(@PathVariable Long id, @RequestBody AdoptionDTO adoptionDto) throws EntityNotFoundException, IllegalOperationException {
        AdoptionEntity entity = modelMapper.map(adoptionDto, AdoptionEntity.class);
        AdoptionEntity updated = adoptionService.updateAdoption(id, entity);
        return new ResponseEntity<>(modelMapper.map(updated, AdoptionDTO.class), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdoption(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        adoptionService.deleteAdoption(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

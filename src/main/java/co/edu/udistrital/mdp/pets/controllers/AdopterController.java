package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.edu.udistrital.mdp.pets.dto.AdopterDTO;
import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.services.AdopterService;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

@RestController
@RequestMapping("/adopters")
public class AdopterController {

    @Autowired
    private AdopterService adopterService;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<AdopterDTO> createAdopter(@RequestBody AdopterDTO adopterDto) throws IllegalOperationException {
        AdopterEntity adopterEntity = modelMapper.map(adopterDto, AdopterEntity.class);
        AdopterEntity created = adopterService.createAdopter(adopterEntity);
        return new ResponseEntity<>(modelMapper.map(created, AdopterDTO.class), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdopterDTO> getAdopter(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        AdopterEntity adopterEntity = adopterService.readAdopter(id);
        return new ResponseEntity<>(modelMapper.map(adopterEntity, AdopterDTO.class), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<AdopterDTO>> getAllAdopters(
            @RequestParam(required = false) String nationalId,
            @RequestParam(required = false) String housingType,
            @RequestParam(required = false) String occupation) throws IllegalOperationException {
        List<AdopterEntity> adopters;
        if (nationalId != null || housingType != null || occupation != null) {
            adopters = adopterService.readAllAdopters(nationalId, housingType, occupation);
        } else {
            adopters = adopterService.readAllAdopters();
        }
        List<AdopterDTO> dtos = adopters.stream().map(a -> modelMapper.map(a, AdopterDTO.class)).collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdopterDTO> updateAdopter(@PathVariable Long id, @RequestBody AdopterDTO adopterDto) throws EntityNotFoundException, IllegalOperationException {
        AdopterEntity adopterEntity = modelMapper.map(adopterDto, AdopterEntity.class);
        AdopterEntity updated = adopterService.updateAdopter(id, adopterEntity);
        return new ResponseEntity<>(modelMapper.map(updated, AdopterDTO.class), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdopter(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        adopterService.deleteAdopter(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

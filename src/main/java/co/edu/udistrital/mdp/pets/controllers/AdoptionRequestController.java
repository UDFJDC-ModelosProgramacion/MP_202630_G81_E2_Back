package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.edu.udistrital.mdp.pets.dto.AdoptionRequestDTO;
import co.edu.udistrital.mdp.pets.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.pets.services.AdoptionRequestService;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;

@RestController
@RequestMapping("/api/adoption-requests")
public class AdoptionRequestController {

    @Autowired
    private AdoptionRequestService adoptionRequestService;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<AdoptionRequestDTO> createRequest(@RequestBody AdoptionRequestDTO requestDto) throws EntityNotFoundException, IllegalOperationException {
        AdoptionRequestEntity entity = modelMapper.map(requestDto, AdoptionRequestEntity.class);
        AdoptionRequestEntity created = adoptionRequestService.createAdoptionRequest(entity);
        return new ResponseEntity<>(modelMapper.map(created, AdoptionRequestDTO.class), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdoptionRequestDTO> getRequest(@PathVariable Long id, 
            @RequestParam(required = false, defaultValue = "1") Long currentUserId,
            @RequestParam(required = false, defaultValue = "ADMIN") String currentUserRole) throws EntityNotFoundException, IllegalOperationException {
        AdoptionRequestEntity entity = adoptionRequestService.readAdoptionRequest(id, currentUserId, currentUserRole);
        return new ResponseEntity<>(modelMapper.map(entity, AdoptionRequestDTO.class), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<AdoptionRequestDTO>> getAllRequests(
            @RequestParam(required = false) Long adopterId,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Long currentUserId,
            @RequestParam(required = false, defaultValue = "ADMIN") String currentUserRole) throws IllegalOperationException {
        List<AdoptionRequestEntity> list = adoptionRequestService.readAllAdoptionRequests(adopterId, petId, status, currentUserId, currentUserRole);
        List<AdoptionRequestDTO> dtoList = list.stream().map(e -> modelMapper.map(e, AdoptionRequestDTO.class)).collect(Collectors.toList());
        return new ResponseEntity<>(dtoList, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdoptionRequestDTO> updateRequest(@PathVariable Long id, @RequestBody AdoptionRequestDTO requestDto) throws EntityNotFoundException, IllegalOperationException {
        AdoptionRequestEntity entity = modelMapper.map(requestDto, AdoptionRequestEntity.class);
        AdoptionRequestEntity updated = adoptionRequestService.updateAdoptionRequest(id, entity);
        return new ResponseEntity<>(modelMapper.map(updated, AdoptionRequestDTO.class), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        adoptionRequestService.deleteAdoptionRequest(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udistrital.mdp.pets.dto.TrialCohabitationRequestDTO;
import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationRequestEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.TrialCohabitationRequestService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/trialCohabitationRequests")
public class TrialCohabitationRequestController {

    private final TrialCohabitationRequestService trialCohabitationRequestService;
    private final ModelMapper modelMapper;

    // FindAll method
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<TrialCohabitationRequestDTO> findAll(@RequestParam(required = false) Long adopterId,
            @RequestParam(required = false) Long petId, @RequestParam(required = false) String status)
            throws IllegalOperationException {
        List<TrialCohabitationRequestEntity> requests = trialCohabitationRequestService.getTrialCohabitationRequests(adopterId, petId, status);
        return modelMapper.map(requests, new TypeToken<List<TrialCohabitationRequestDTO>>() {
        }.getType());
    }

    // findOne method
    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public TrialCohabitationRequestDTO findOne(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        TrialCohabitationRequestEntity requestEntity = trialCohabitationRequestService.getTrialCohabitationRequest(id);
        return modelMapper.map(requestEntity, TrialCohabitationRequestDTO.class);
    }

    // create method
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public TrialCohabitationRequestDTO create(@RequestBody TrialCohabitationRequestDTO requestDTO) throws IllegalOperationException, EntityNotFoundException {
        TrialCohabitationRequestEntity requestEntity = trialCohabitationRequestService
                .createTrialCohabitationRequest(toEntity(requestDTO));
        return modelMapper.map(requestEntity, TrialCohabitationRequestDTO.class);
    }

    // update method
    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public TrialCohabitationRequestDTO update(@PathVariable Long id, @RequestBody TrialCohabitationRequestDTO requestDTO) throws EntityNotFoundException, IllegalOperationException {
        TrialCohabitationRequestEntity requestEntity = trialCohabitationRequestService
                .updateTrialCohabitationRequest(id, toEntity(requestDTO));
        return modelMapper.map(requestEntity, TrialCohabitationRequestDTO.class);
    }

    /**
     * Builds the entity from the DTO field by field. Mapping with ModelMapper leaves
     * the {@code trialCohabitation} reference as an unsaved transient instance, which
     * makes the save fail; mapping by hand keeps it null.
     */
    private TrialCohabitationRequestEntity toEntity(TrialCohabitationRequestDTO dto) {
        TrialCohabitationRequestEntity entity = new TrialCohabitationRequestEntity();
        entity.setStatus(dto.getStatus());
        entity.setDate(dto.getDate());
        entity.setDescription(dto.getDescription());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        if (dto.getShelter() != null && dto.getShelter().getId() != null) {
            ShelterEntity shelter = new ShelterEntity();
            shelter.setId(dto.getShelter().getId());
            entity.setShelter(shelter);
        }
        if (dto.getAdopter() != null && dto.getAdopter().getId() != null) {
            AdopterEntity adopter = new AdopterEntity();
            adopter.setId(dto.getAdopter().getId());
            entity.setAdopter(adopter);
        }
        if (dto.getPet() != null && dto.getPet().getId() != null) {
            PetEntity pet = new PetEntity();
            pet.setId(dto.getPet().getId());
            entity.setPet(pet);
        }
        return entity;
    }

    // delete method
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        trialCohabitationRequestService.deleteTrialCohabitationRequest(id);
    }
}
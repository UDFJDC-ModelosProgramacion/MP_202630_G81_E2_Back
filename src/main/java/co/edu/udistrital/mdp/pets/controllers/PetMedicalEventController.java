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

import co.edu.udistrital.mdp.pets.dto.MedicalEventDTO;
import co.edu.udistrital.mdp.pets.dto.PetMedicalEventDTO;
import co.edu.udistrital.mdp.pets.entities.MedicalEventEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.MedicalEventService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets/{petId}/medicalEvents")
public class PetMedicalEventController {

    private final MedicalEventService medicalEventService;
    private final ModelMapper modelMapper;

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public PetMedicalEventDTO create(@PathVariable Long petId, @RequestBody MedicalEventDTO medicalEventDTO)
            throws EntityNotFoundException, IllegalOperationException {
        MedicalEventEntity medicalEvent = medicalEventService.createMedicalEvent(petId,
                modelMapper.map(medicalEventDTO, MedicalEventEntity.class));
        return modelMapper.map(medicalEvent, PetMedicalEventDTO.class);
    }

    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<PetMedicalEventDTO> findAll(@PathVariable Long petId)
            throws EntityNotFoundException, IllegalOperationException {
        List<MedicalEventEntity> medicalEvents = medicalEventService.getMedicalEvents(petId);
        return modelMapper.map(medicalEvents, new TypeToken<List<PetMedicalEventDTO>>() {
        }.getType());
    }

    @GetMapping("/{medicalEventId}")
    @ResponseStatus(code = HttpStatus.OK)
    public PetMedicalEventDTO findOne(@PathVariable Long petId, @PathVariable Long medicalEventId)
            throws EntityNotFoundException, IllegalOperationException {
        MedicalEventEntity medicalEvent = medicalEventService.getMedicalEvent(petId, medicalEventId);
        return modelMapper.map(medicalEvent, PetMedicalEventDTO.class);
    }

    @PutMapping("/{medicalEventId}")
    @ResponseStatus(code = HttpStatus.OK)
    public PetMedicalEventDTO update(@PathVariable Long petId, @PathVariable Long medicalEventId,
            @RequestBody MedicalEventDTO medicalEventDTO)
            throws EntityNotFoundException, IllegalOperationException {
        MedicalEventEntity medicalEvent = medicalEventService.updateMedicalEvent(petId, medicalEventId,
                modelMapper.map(medicalEventDTO, MedicalEventEntity.class));
        return modelMapper.map(medicalEvent, PetMedicalEventDTO.class);
    }

    @DeleteMapping("/{medicalEventId}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long petId, @PathVariable Long medicalEventId)
            throws EntityNotFoundException, IllegalOperationException {
        medicalEventService.deleteMedicalEvent(petId, medicalEventId);
    }
}
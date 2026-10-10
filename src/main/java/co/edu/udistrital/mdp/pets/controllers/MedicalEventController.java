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
import co.edu.udistrital.mdp.pets.entities.MedicalEventEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.MedicalEventService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/medicalEvents")
public class MedicalEventController {

    private final MedicalEventService medicalEventService;
    private final ModelMapper modelMapper;

    // FindAll method
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<MedicalEventDTO> findAll() {
        List<MedicalEventEntity> medicalEvents = medicalEventService.getMedicalEvents();
        return modelMapper.map(medicalEvents, new TypeToken<List<MedicalEventDTO>>() {
        }.getType());
    }

    // findOne method
    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public MedicalEventDTO findOne(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        MedicalEventEntity medicalEventEntity = medicalEventService.getMedicalEvent(id);
        return modelMapper.map(medicalEventEntity, MedicalEventDTO.class);
    }

    // create method
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public MedicalEventDTO create(@RequestBody MedicalEventDTO medicalEventDTO) throws IllegalOperationException, EntityNotFoundException {
        MedicalEventEntity medicalEventEntity = medicalEventService.createMedicalEvent(modelMapper.map(medicalEventDTO, MedicalEventEntity.class));
        return modelMapper.map(medicalEventEntity, MedicalEventDTO.class);
    }

    // update method
    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public MedicalEventDTO update(@PathVariable Long id, @RequestBody MedicalEventDTO medicalEventDTO) throws EntityNotFoundException, IllegalOperationException {
        MedicalEventEntity medicalEventEntity = medicalEventService.updateMedicalEvent(id, modelMapper.map(medicalEventDTO, MedicalEventEntity.class));
        return modelMapper.map(medicalEventEntity, MedicalEventDTO.class);
    }

    // delete method
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        medicalEventService.deleteMedicalEvent(id);
    }
}
package co.edu.udistrital.mdp.pets.controllers;

import java.util.Date;
import java.util.List;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.format.annotation.DateTimeFormat;
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

import co.edu.udistrital.mdp.pets.dto.TrialCohabitationDTO;
import co.edu.udistrital.mdp.pets.dto.TrialCohabitationRequestDTO;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationEntity;
import co.edu.udistrital.mdp.pets.entities.TrialCohabitationRequestEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.TrialCohabitationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/trialCohabitations")
public class TrialCohabitationController {

    private final TrialCohabitationService trialCohabitationService;
    private final ModelMapper modelMapper;

    @PostConstruct
    public void configureModelMapper() {
        Converter<TrialCohabitationRequestEntity, TrialCohabitationRequestDTO> requestToDto = context -> {
            TrialCohabitationRequestEntity source = context.getSource();
            if (source == null)
                return null;
            TrialCohabitationRequestDTO dto = new TrialCohabitationRequestDTO();
            dto.setId(source.getId());
            return dto;
        };
        modelMapper.addConverter(requestToDto, TrialCohabitationRequestEntity.class, TrialCohabitationRequestDTO.class);
    }

    // FindAll method with optional status/date filters
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<TrialCohabitationDTO> findAll(@RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") Date endDate)
            throws IllegalOperationException {
        List<TrialCohabitationEntity> trials = (status == null && startDate == null && endDate == null)
                ? trialCohabitationService.readAllTrials()
                : trialCohabitationService.readAllTrials(status, startDate, endDate);
        return modelMapper.map(trials, new TypeToken<List<TrialCohabitationDTO>>() {
        }.getType());
    }

    // findOne method
    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public TrialCohabitationDTO findOne(@PathVariable Long id)
            throws EntityNotFoundException, IllegalOperationException {
        TrialCohabitationEntity trialEntity = trialCohabitationService.readTrial(id);
        return modelMapper.map(trialEntity, TrialCohabitationDTO.class);
    }

    // create method
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public TrialCohabitationDTO create(@RequestBody TrialCohabitationDTO trialDTO)
            throws EntityNotFoundException, IllegalOperationException {
        TrialCohabitationEntity trialEntity = trialCohabitationService
                .createTrial(modelMapper.map(trialDTO, TrialCohabitationEntity.class));
        return modelMapper.map(trialEntity, TrialCohabitationDTO.class);
    }

    // update method
    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public TrialCohabitationDTO update(@PathVariable Long id, @RequestBody TrialCohabitationDTO trialDTO)
            throws EntityNotFoundException, IllegalOperationException {
        TrialCohabitationEntity trialEntity = trialCohabitationService.updateTrial(id,
                modelMapper.map(trialDTO, TrialCohabitationEntity.class));
        return modelMapper.map(trialEntity, TrialCohabitationDTO.class);
    }

    // delete method
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        trialCohabitationService.deleteTrial(id);
    }
}
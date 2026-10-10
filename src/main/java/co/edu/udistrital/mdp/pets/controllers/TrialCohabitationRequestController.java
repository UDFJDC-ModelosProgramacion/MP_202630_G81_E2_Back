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

import co.edu.udistrital.mdp.pets.dto.TrialCohabitationRequestDTO;
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
    public List<TrialCohabitationRequestDTO> findAll() {
        List<TrialCohabitationRequestEntity> requests = trialCohabitationRequestService.getTrialCohabitationRequests();
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
                .createTrialCohabitationRequest(modelMapper.map(requestDTO, TrialCohabitationRequestEntity.class));
        return modelMapper.map(requestEntity, TrialCohabitationRequestDTO.class);
    }

    // update method
    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public TrialCohabitationRequestDTO update(@PathVariable Long id, @RequestBody TrialCohabitationRequestDTO requestDTO) throws EntityNotFoundException, IllegalOperationException {
        TrialCohabitationRequestEntity requestEntity = trialCohabitationRequestService
                .updateTrialCohabitationRequest(id, modelMapper.map(requestDTO, TrialCohabitationRequestEntity.class));
        return modelMapper.map(requestEntity, TrialCohabitationRequestDTO.class);
    }

    // delete method
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        trialCohabitationRequestService.deleteTrialCohabitationRequest(id);
    }
}
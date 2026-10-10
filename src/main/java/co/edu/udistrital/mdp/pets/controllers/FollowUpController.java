package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;

import org.modelmapper.Converter;
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

import co.edu.udistrital.mdp.pets.dto.AdoptionDTO;
import co.edu.udistrital.mdp.pets.dto.FollowUpDTO;
import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.FollowUpEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.FollowUpService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/followUps")
public class FollowUpController {

    private final FollowUpService followUpService;
    private final ModelMapper modelMapper;

    @PostConstruct
    public void configureModelMapper() {
        Converter<AdoptionEntity, AdoptionDTO> adoptionToDto = context -> {
            AdoptionEntity source = context.getSource();
            if (source == null)
                return null;
            AdoptionDTO dto = new AdoptionDTO();
            dto.setId(source.getId());
            return dto;
        };
        modelMapper.addConverter(adoptionToDto, AdoptionEntity.class, AdoptionDTO.class);
    }

    // FindAll method with optional adoption filter
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<FollowUpDTO> findAll(@RequestParam(required = false) Long adoptionId) {
        List<FollowUpEntity> followUps = (adoptionId == null) ? followUpService.getFollowUps()
                : followUpService.getFollowUps(adoptionId);
        return modelMapper.map(followUps, new TypeToken<List<FollowUpDTO>>() {
        }.getType());
    }

    // findOne method with optional access-control parameters
    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public FollowUpDTO findOne(@PathVariable Long id, @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) String role) throws EntityNotFoundException, IllegalOperationException {
        FollowUpEntity followUpEntity;
        if (role != null)
            followUpEntity = followUpService.getFollowUp(id, requesterId, role);
        else if (requesterId != null)
            followUpEntity = followUpService.getFollowUp(id, requesterId);
        else
            followUpEntity = followUpService.getFollowUp(id);
        return modelMapper.map(followUpEntity, FollowUpDTO.class);
    }

    // create method
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public FollowUpDTO create(@RequestBody FollowUpDTO followUpDTO)
            throws EntityNotFoundException, IllegalOperationException {
        FollowUpEntity followUpEntity = followUpService
                .createFollowUp(modelMapper.map(followUpDTO, FollowUpEntity.class));
        return modelMapper.map(followUpEntity, FollowUpDTO.class);
    }

    // update method
    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public FollowUpDTO update(@PathVariable Long id, @RequestBody FollowUpDTO followUpDTO)
            throws EntityNotFoundException, IllegalOperationException {
        FollowUpEntity followUpEntity = followUpService.updateFollowUp(id,
                modelMapper.map(followUpDTO, FollowUpEntity.class));
        return modelMapper.map(followUpEntity, FollowUpDTO.class);
    }

    // delete method
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        followUpService.deleteFollowUp(id);
    }
}
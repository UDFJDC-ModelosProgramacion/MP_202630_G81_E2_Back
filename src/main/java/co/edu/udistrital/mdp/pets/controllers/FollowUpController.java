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
@RequestMapping("/adoptions/{adoptionId}/followUps")
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

    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<FollowUpDTO> getFollowUps(@PathVariable Long adoptionId)
            throws EntityNotFoundException, IllegalOperationException {
        List<FollowUpEntity> followUps = followUpService.getAdoptionFollowUps(adoptionId);
        return modelMapper.map(followUps, new TypeToken<List<FollowUpDTO>>() {
        }.getType());
    }

    @GetMapping(value = "/{followUpId}")
    @ResponseStatus(code = HttpStatus.OK)
    public FollowUpDTO getFollowUp(@PathVariable Long adoptionId, @PathVariable Long followUpId,
            @RequestParam(required = false) Long requesterId, @RequestParam(required = false) String role)
            throws EntityNotFoundException, IllegalOperationException {
        FollowUpEntity followUpEntity = followUpService.getAdoptionFollowUp(adoptionId, followUpId);
        if (role != null)
            followUpEntity = followUpService.getFollowUp(followUpId, requesterId, role);
        else if (requesterId != null)
            followUpEntity = followUpService.getFollowUp(followUpId, requesterId);
        return modelMapper.map(followUpEntity, FollowUpDTO.class);
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public FollowUpDTO createFollowUp(@PathVariable Long adoptionId, @RequestBody FollowUpDTO followUpDTO)
            throws EntityNotFoundException, IllegalOperationException {
        FollowUpEntity followUpEntity = followUpService.createAdoptionFollowUp(adoptionId,
                modelMapper.map(followUpDTO, FollowUpEntity.class));
        return modelMapper.map(followUpEntity, FollowUpDTO.class);
    }

    @PutMapping(value = "/{followUpId}")
    @ResponseStatus(code = HttpStatus.OK)
    public FollowUpDTO updateFollowUp(@PathVariable Long adoptionId, @PathVariable Long followUpId,
            @RequestBody FollowUpDTO followUpDTO) throws EntityNotFoundException, IllegalOperationException {
        FollowUpEntity followUpEntity = followUpService.updateAdoptionFollowUp(adoptionId, followUpId,
                modelMapper.map(followUpDTO, FollowUpEntity.class));
        return modelMapper.map(followUpEntity, FollowUpDTO.class);
    }

    @DeleteMapping(value = "/{followUpId}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deleteFollowUp(@PathVariable Long adoptionId, @PathVariable Long followUpId)
            throws EntityNotFoundException, IllegalOperationException {
        followUpService.deleteAdoptionFollowUp(adoptionId, followUpId);
    }
}

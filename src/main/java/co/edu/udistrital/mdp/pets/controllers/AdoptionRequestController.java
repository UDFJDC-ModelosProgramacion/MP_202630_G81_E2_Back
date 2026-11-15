package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;

import org.modelmapper.ModelMapper;
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

import co.edu.udistrital.mdp.pets.dto.AdopterDTO;
import co.edu.udistrital.mdp.pets.dto.AdoptionRequestDTO;
import co.edu.udistrital.mdp.pets.dto.PetDTO;
import co.edu.udistrital.mdp.pets.dto.ShelterDTO;
import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.AdoptionRequestService;
import lombok.RequiredArgsConstructor;

/**
 * Exposes the AdoptionRequest resource. The Adoption and TrialCohabitationRequest
 * processes both need requests that already exist in a given state (for instance an
 * APPROVED one), so the resource has to be reachable to prepare that data.
 *
 * <p>The service restricts reads to the creator of the request and to
 * administrators. Since the API has no authentication yet, the queries report the
 * ADMIN role by default; when the role is supplied explicitly the restriction is
 * honoured.</p>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/adoptionRequests")
public class AdoptionRequestController {

    /**
     * Role used when the request does not carry one. It lets the clients that
     * cannot authenticate yet (such as the Postman collections) read the resource.
     */
    private static final String DEFAULT_ROLE = "ADMIN";

    private final AdoptionRequestService adoptionRequestService;
    private final ModelMapper modelMapper;

    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<AdoptionRequestDTO> findAll(@RequestParam(required = false) Long adopterId,
            @RequestParam(required = false) Long petId, @RequestParam(required = false) String status,
            @RequestParam(required = false) Long currentUserId,
            @RequestParam(required = false, defaultValue = DEFAULT_ROLE) String currentUserRole)
            throws IllegalOperationException {
        List<AdoptionRequestEntity> requests = adoptionRequestService.readAllAdoptionRequests(adopterId, petId, status,
                currentUserId, currentUserRole);
        return requests.stream().map(this::toDto).toList();
    }

    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public AdoptionRequestDTO findOne(@PathVariable Long id, @RequestParam(required = false) Long currentUserId,
            @RequestParam(required = false, defaultValue = DEFAULT_ROLE) String currentUserRole)
            throws EntityNotFoundException, IllegalOperationException {
        AdoptionRequestEntity entity = adoptionRequestService.readAdoptionRequest(id, currentUserId, currentUserRole);
        return toDto(entity);
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public AdoptionRequestDTO create(@RequestBody AdoptionRequestDTO dto)
            throws EntityNotFoundException, IllegalOperationException {
        AdoptionRequestEntity entity = adoptionRequestService
                .createAdoptionRequest(toEntity(dto));
        return toDto(entity);
    }

    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public AdoptionRequestDTO update(@PathVariable Long id, @RequestBody AdoptionRequestDTO dto)
            throws EntityNotFoundException, IllegalOperationException {
        AdoptionRequestEntity entity = adoptionRequestService.updateAdoptionRequest(id,
                toEntity(dto));
        return toDto(entity);
    }

    /**
     * Builds the DTO field by field as well. Mapping the entity with ModelMapper
     * fails because the references to TrialCohabitation and Adoption are crossed
     * (request &rarr; trial &rarr; trial &rarr; request) and ModelMapper cannot tell which
     * source hierarchy each destination property comes from.
     */
    private AdoptionRequestDTO toDto(AdoptionRequestEntity entity) {
        AdoptionRequestDTO dto = new AdoptionRequestDTO();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setDate(entity.getDate());
        dto.setDescription(entity.getDescription());
        if (entity.getPet() != null) {
            dto.setPet(modelMapper.map(entity.getPet(), PetDTO.class));
        }
        if (entity.getShelter() != null) {
            dto.setShelter(modelMapper.map(entity.getShelter(), ShelterDTO.class));
        }
        if (entity.getAdopter() != null) {
            dto.setAdopter(modelMapper.map(entity.getAdopter(), AdopterDTO.class));
        }
        return dto;
    }

    /**
     * Builds the entity from the DTO field by field. ModelMapper leaves the
     * {@code adoption} and {@code trialCohabition} references as unsaved transient
     * instances, which makes the save fail; mapping by hand keeps them null.
     */
    private AdoptionRequestEntity toEntity(AdoptionRequestDTO dto) {
        AdoptionRequestEntity entity = new AdoptionRequestEntity();
        entity.setStatus(dto.getStatus());
        entity.setDate(dto.getDate());
        entity.setDescription(dto.getDescription());
        if (dto.getPet() != null && dto.getPet().getId() != null) {
            PetEntity pet = new PetEntity();
            pet.setId(dto.getPet().getId());
            entity.setPet(pet);
        }
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
        return entity;
    }

    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        adoptionRequestService.deleteAdoptionRequest(id);
    }
}

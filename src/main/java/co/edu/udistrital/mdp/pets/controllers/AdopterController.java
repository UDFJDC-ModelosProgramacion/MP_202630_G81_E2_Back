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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udistrital.mdp.pets.dto.AdopterDTO;
import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.AdopterService;
import lombok.RequiredArgsConstructor;

/**
 * Exposes the Adopter resource. Adopters are users that request trial
 * cohabitations and adoptions, so the resource is needed to prepare the data
 * those processes depend on.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/adopters")
public class AdopterController {

    private final AdopterService adopterService;
    private final ModelMapper modelMapper;

    /**
     * Returns every adopter, optionally filtered by nationalId, housingType or occupation.
     */
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<AdopterDTO> findAll(@RequestParam(required = false) String nationalId,
            @RequestParam(required = false) String housingType, @RequestParam(required = false) String occupation)
            throws IllegalOperationException {
        List<AdopterEntity> adopters = adopterService.readAllAdopters(nationalId, housingType, occupation);
        return modelMapper.map(adopters, new TypeToken<List<AdopterDTO>>() {
        }.getType());
    }

    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public AdopterDTO findOne(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        AdopterEntity entity = adopterService.readAdopter(id);
        return modelMapper.map(entity, AdopterDTO.class);
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public AdopterDTO create(@RequestBody AdopterDTO dto) throws IllegalOperationException {
        AdopterEntity entity = adopterService.createAdopter(modelMapper.map(dto, AdopterEntity.class));
        return modelMapper.map(entity, AdopterDTO.class);
    }

    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public AdopterDTO update(@PathVariable Long id, @RequestBody AdopterDTO dto)
            throws EntityNotFoundException, IllegalOperationException {
        AdopterEntity entity = adopterService.updateAdopter(id, modelMapper.map(dto, AdopterEntity.class));
        return modelMapper.map(entity, AdopterDTO.class);
    }

    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        adopterService.deleteAdopter(id);
    }
}

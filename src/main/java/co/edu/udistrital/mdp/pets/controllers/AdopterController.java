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
import co.edu.udistrital.mdp.pets.dto.AdopterDetailDTO;
import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.AdopterService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/adopters")
public class AdopterController {

    private final AdopterService adopterService;
    private final ModelMapper modelMapper;

    // FindAll method with optional filters
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<AdopterDetailDTO> findAll(@RequestParam(required = false) String nationalId,
            @RequestParam(required = false) String housingType, @RequestParam(required = false) String occupation)
            throws IllegalOperationException {
        List<AdopterEntity> adopters = (nationalId == null && housingType == null && occupation == null)
                ? adopterService.readAllAdopters()
                : adopterService.readAllAdopters(nationalId, housingType, occupation);
        return modelMapper.map(adopters, new TypeToken<List<AdopterDetailDTO>>() {
        }.getType());
    }

    // findOne method
    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public AdopterDetailDTO findOne(@PathVariable Long id)
            throws EntityNotFoundException, IllegalOperationException {
        AdopterEntity adopterEntity = adopterService.readAdopter(id);
        return modelMapper.map(adopterEntity, AdopterDetailDTO.class);
    }

    // create method
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public AdopterDTO create(@RequestBody AdopterDTO adopterDTO) throws IllegalOperationException {
        AdopterEntity adopterEntity = adopterService.createAdopter(modelMapper.map(adopterDTO, AdopterEntity.class));
        return modelMapper.map(adopterEntity, AdopterDTO.class);
    }

    // update method
    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public AdopterDTO update(@PathVariable Long id, @RequestBody AdopterDTO adopterDTO)
            throws EntityNotFoundException, IllegalOperationException {
        AdopterEntity adopterEntity = adopterService.updateAdopter(id, modelMapper.map(adopterDTO, AdopterEntity.class));
        return modelMapper.map(adopterEntity, AdopterDTO.class);
    }

    // delete method
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        adopterService.deleteAdopter(id);
    }
}
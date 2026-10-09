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

import co.edu.udistrital.mdp.pets.dto.PetDTO;
import co.edu.udistrital.mdp.pets.dto.PetDetailDTO;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.PetService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
@RestController 
@RequestMapping("/pets")
public class PetController {
    
    private final PetService petService;
    private final ModelMapper modelMapper;

    // FindAll method
    @GetMapping 
    @ResponseStatus(code = HttpStatus.OK)
    public List<PetDetailDTO> findAll(
            @RequestParam(name = "species", required = false) String species,
            @RequestParam(name = "age", required = false) Integer age,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "requiredSpace", required = false) String requiredSpace,
            @RequestParam(name = "compatibilityChildren", required = false) Boolean compatibilityChildren,
            @RequestParam(name = "compatibilityOtherPets", required = false) Boolean compatibilityOtherPets,
            @RequestParam(name = "activityLevel", required = false) String activityLevel) {
        List<PetEntity> pets = petService.getPets(species, age, size, requiredSpace, compatibilityChildren,
                compatibilityOtherPets, activityLevel);
        return modelMapper.map(pets, new TypeToken<List<PetDetailDTO>>() {
        }.getType());
    }

    // finOne method
    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public PetDetailDTO findOne(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        PetEntity petEntity = petService.getPet(id);
        return modelMapper.map(petEntity, PetDetailDTO.class);
    }

    // create method
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public PetDTO create(@RequestBody PetDTO petDTO) throws IllegalOperationException, EntityNotFoundException {
        PetEntity petEntity = petService.createPet(modelMapper.map(petDTO, PetEntity.class));
        return modelMapper.map(petEntity, PetDTO.class);
    }

    // update method
    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public PetDTO update(@PathVariable Long id, @RequestBody PetDTO petDTO) throws EntityNotFoundException, IllegalOperationException {
        PetEntity petEntity = petService.updatePet(id, modelMapper.map(petDTO, PetEntity.class));
        return modelMapper.map(petEntity, PetDTO.class);
    }

    // delete method
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        petService.deletePet(id);
    }
}
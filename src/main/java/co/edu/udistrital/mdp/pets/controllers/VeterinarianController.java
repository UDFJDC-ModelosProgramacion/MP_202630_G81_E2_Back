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

import co.edu.udistrital.mdp.pets.dto.VeterinarianDTO;
import co.edu.udistrital.mdp.pets.dto.VeterinarianDetailDTO;
import co.edu.udistrital.mdp.pets.entities.VeterinarianEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.VeterinarianService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/veterinarians")
public class VeterinarianController {

    private final VeterinarianService veterinarianService;
    private final ModelMapper modelMapper;

    
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<VeterinarianDetailDTO> findAll(@RequestParam(required = false) String specialization,
            @RequestParam(required = false) String availability) {
        List<VeterinarianEntity> veterinarians = veterinarianService.getVeterinarians(specialization, availability);
        return modelMapper.map(veterinarians, new TypeToken<List<VeterinarianDetailDTO>>() {
        }.getType());
    }

    
    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public VeterinarianDetailDTO findOne(@PathVariable Long id)
            throws EntityNotFoundException, IllegalOperationException {
        VeterinarianEntity entity = veterinarianService.getVeterinarian(id);
        return modelMapper.map(entity, VeterinarianDetailDTO.class);
    }

    
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public VeterinarianDTO create(@RequestBody VeterinarianDTO dto) throws IllegalOperationException {
        VeterinarianEntity entity = veterinarianService.createVeterinarian(modelMapper.map(dto, VeterinarianEntity.class));
        return modelMapper.map(entity, VeterinarianDTO.class);
    }

    
    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public VeterinarianDTO update(@PathVariable Long id, @RequestBody VeterinarianDTO dto)
            throws EntityNotFoundException, IllegalOperationException {
        VeterinarianEntity entity = veterinarianService.updateVeterinarian(id,
                modelMapper.map(dto, VeterinarianEntity.class));
        return modelMapper.map(entity, VeterinarianDTO.class);
    }

    
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        veterinarianService.deleteVeterinarian(id);
    }
}
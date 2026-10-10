package co.edu.udistrital.mdp.pets.controllers;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udistrital.mdp.pets.dto.VaccineDTO;
import co.edu.udistrital.mdp.pets.entities.VaccineEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.VaccineService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/vaccines")
public class VaccineQueryController {

    private final VaccineService vaccineService;
    private final ModelMapper modelMapper;

    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<VaccineDTO> findAll(
            @RequestParam(required = false) Long vaccinationRecordId,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) String name)
            throws EntityNotFoundException, IllegalOperationException {
        List<VaccineEntity> vaccines = vaccineService.getVaccines(vaccinationRecordId, petId, status, name);
        return modelMapper.map(vaccines, new TypeToken<List<VaccineDTO>>() {
        }.getType());
    }

    @GetMapping("/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public VaccineDTO findOne(@PathVariable Long id)
            throws EntityNotFoundException, IllegalOperationException {
        return modelMapper.map(vaccineService.getVaccine(id), VaccineDTO.class);
    }
}
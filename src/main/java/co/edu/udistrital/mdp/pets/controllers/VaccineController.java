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

import co.edu.udistrital.mdp.pets.dto.VaccineDTO;
import co.edu.udistrital.mdp.pets.entities.VaccineEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.VaccineService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
@RestController 
@RequestMapping("/vaccinationRecords")
public class VaccineController {
    
    private final VaccineService vaccineService;
    private final ModelMapper modelMapper;

    // create method
    @PostMapping(value = "/{vaccinationRecordId}/vaccines")
    @ResponseStatus(code = HttpStatus.CREATED)
    public VaccineDTO createVaccine(@PathVariable Long vaccinationRecordId, @RequestBody VaccineDTO vaccineDTO) throws EntityNotFoundException, IllegalOperationException {
        VaccineEntity vaccineEntity = modelMapper.map(vaccineDTO, VaccineEntity.class);
        VaccineEntity newVaccine = vaccineService.createVaccine(vaccinationRecordId, vaccineEntity);
        return modelMapper.map(newVaccine, VaccineDTO.class);
    }

    // getAll method
    @GetMapping(value = "/{vaccinationRecordId}/vaccines")
    @ResponseStatus(code = HttpStatus.OK)
    public List<VaccineDTO> getVaccines(@PathVariable Long vaccinationRecordId) throws EntityNotFoundException, IllegalOperationException {
        List<VaccineEntity> vaccines = vaccineService.getVaccines(vaccinationRecordId);
        return modelMapper.map(vaccines, new TypeToken<List<VaccineDTO>>() {     
        }.getType());
    }

    // get method
    @GetMapping(value = "/{vaccinationRecordId}/vaccines/{vaccineId}")
    @ResponseStatus(code = HttpStatus.OK)
    public VaccineDTO getVaccine(@PathVariable Long vaccinationRecordId, @PathVariable Long vaccineId) throws EntityNotFoundException, IllegalOperationException {
        VaccineEntity vaccineEntity = vaccineService.getVaccine(vaccinationRecordId, vaccineId);
        return modelMapper.map(vaccineEntity, VaccineDTO.class);
    }

    // update method
    @PutMapping(value = "/{vaccinationRecordId}/vaccines/{vaccineId}")
    @ResponseStatus(code = HttpStatus.OK)
    public VaccineDTO updateVaccine(@PathVariable Long vaccinationRecordId, @PathVariable Long vaccineId, @RequestBody VaccineDTO vaccineDTO) throws EntityNotFoundException, IllegalOperationException {
        VaccineEntity vaccineEntity = modelMapper.map(vaccineDTO, VaccineEntity.class);
        VaccineEntity newVaccineEntity = vaccineService.updateVaccine(vaccinationRecordId, vaccineId, vaccineEntity);
        return modelMapper.map(newVaccineEntity, VaccineDTO.class);
    }

    // delete method
    @DeleteMapping(value = "/{vaccinationRecordId}/vaccines/{vaccineId}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deleteVaccine(@PathVariable Long vaccinationRecordId, @PathVariable Long vaccineId) throws EntityNotFoundException, IllegalOperationException {
        vaccineService.deleteVaccine(vaccinationRecordId, vaccineId);
    }
}
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

import co.edu.udistrital.mdp.pets.dto.VaccinationRecordDetailDTO;
import co.edu.udistrital.mdp.pets.entities.VaccinationRecordEntity;
import co.edu.udistrital.mdp.pets.exceptions.EntityNotFoundException;
import co.edu.udistrital.mdp.pets.exceptions.IllegalOperationException;
import co.edu.udistrital.mdp.pets.services.VaccinationRecordService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/vaccinationRecords")
public class VaccinationRecordController {

    private final VaccinationRecordService vaccinationRecordService;
    private final ModelMapper modelMapper;

    // FindAll method
    @GetMapping
    @ResponseStatus(code = HttpStatus.OK)
    public List<VaccinationRecordDetailDTO> findAll() {
        List<VaccinationRecordEntity> records = vaccinationRecordService.getVaccinationRecords();
        return modelMapper.map(records, new TypeToken<List<VaccinationRecordDetailDTO>>() {
        }.getType());
    }

    // findOne method
    @GetMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public VaccinationRecordDetailDTO findOne(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        VaccinationRecordEntity record = vaccinationRecordService.getVaccinationRecord(id);
        return modelMapper.map(record, VaccinationRecordDetailDTO.class);
    }

    // create method
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public VaccinationRecordDetailDTO create(@RequestBody VaccinationRecordDetailDTO recordDTO) throws EntityNotFoundException, IllegalOperationException {
        VaccinationRecordEntity record = vaccinationRecordService.createVaccinationRecord(modelMapper.map(recordDTO, VaccinationRecordEntity.class));
        return modelMapper.map(record, VaccinationRecordDetailDTO.class);
    }

    // update method
    @PutMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public VaccinationRecordDetailDTO update(@PathVariable Long id, @RequestBody VaccinationRecordDetailDTO recordDTO) throws EntityNotFoundException, IllegalOperationException {
        VaccinationRecordEntity record = vaccinationRecordService.updateVaccinationRecord(id, modelMapper.map(recordDTO, VaccinationRecordEntity.class));
        return modelMapper.map(record, VaccinationRecordDetailDTO.class);
    }

    // delete method
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) throws EntityNotFoundException, IllegalOperationException {
        vaccinationRecordService.deleteVaccinationRecord(id);
    }
}
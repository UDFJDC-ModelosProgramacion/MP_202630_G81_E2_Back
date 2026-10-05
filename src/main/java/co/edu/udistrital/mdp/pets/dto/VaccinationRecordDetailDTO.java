package co.edu.udistrital.mdp.pets.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class VaccinationRecordDetailDTO extends VaccinationRecordDTO {
    private PetDTO pet;
    private List<VaccineDTO> vaccines = new ArrayList<>();
}
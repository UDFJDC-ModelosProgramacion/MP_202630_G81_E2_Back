package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

@Data
public class VaccineDTO {
    private Long id;
    private String name;
    private Date addministrationDate;
    private Date nextAdministration;
    private Boolean status;

    private VaccinationRecordDTO vaccinationRecord;
}

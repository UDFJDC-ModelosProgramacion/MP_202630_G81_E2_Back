package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

@Data
public class TrialCohabitationRequestDTO {
    private Long id;
    private Date date;
    private String status;
    private String description;
    private Date startDate;
    private Date endDate;
}
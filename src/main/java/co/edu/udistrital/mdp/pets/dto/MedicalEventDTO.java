package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

@Data
public class MedicalEventDTO{
    private Long id;
    private Date date;
    private String type;
    private String description;

    private PetDTO pet;
    private VeterinarianDTO veterinarian;
}
package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;

@Data
public class MedicalEventDetailDTO extends MedicalEventDTO {

    private PetDTO pet;
    private VeterinarianDTO veterinarian;
}
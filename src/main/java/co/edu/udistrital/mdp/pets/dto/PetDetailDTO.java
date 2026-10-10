package co.edu.udistrital.mdp.pets.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class PetDetailDTO extends PetDTO{
    private List<PhotoDTO> photos = new ArrayList<>();
    // AdoptionRequests
    // Adoptions
    private List<ReviewDTO> reviews = new ArrayList<>();
    // MedicalEvents
}

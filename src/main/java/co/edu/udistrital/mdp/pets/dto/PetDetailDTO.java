package co.edu.udistrital.mdp.pets.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class PetDetailDTO extends PetDTO{
    private List<PhotoDTO> photos = new ArrayList<>();
    private List<AdoptionRequestDTO> adoptionRequests = new ArrayList<>();
    private List<AdoptionDTO> adoptions = new ArrayList<>();
    private List<ReviewDTO> reviews = new ArrayList<>();
    private List<MedicalEventDTO> medicalEvents = new ArrayList<>();
}

package co.edu.udistrital.mdp.pets.dto;

import java.util.List;
import java.util.ArrayList;

import lombok.Data;

@Data
public class ShelterDetailDTO extends ShelterDTO {

    private List<PetDTO> pets = new ArrayList<>();
    // Photos
    // CohabitationRequests
    // AdoptionRequests
    // TrialCohabitations
    // Adoptions
    // Returns duringtrial
    private List<EventDTO> events = new ArrayList<>();
    // Users
    // Adopters
    private List<VeterinarianDTO> veterinarians = new ArrayList<>();
}
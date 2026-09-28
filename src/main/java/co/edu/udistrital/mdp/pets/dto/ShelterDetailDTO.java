package co.edu.udistrital.mdp.pets.dto;

import java.util.List;
import java.util.ArrayList;

import lombok.Data;

@Data
public class ShelterDetailDTO extends ShelterDTO {

    private List<PetDTO> pets = new ArrayList<>();
    private List<PhotoDTO> photos = new ArrayList<>();
    // CohabitationRequests
    // AdoptionRequests
    private List<TrialCohabitationDTO> trialCohabitations = new ArrayList<>();
    // Adoptions
    // Returns duringtrial
    private List<EventDTO> events = new ArrayList<>();
    private List<AdopterDTO> adopters = new ArrayList<>();
    private List<VeterinarianDTO> veterinarians = new ArrayList<>();
}
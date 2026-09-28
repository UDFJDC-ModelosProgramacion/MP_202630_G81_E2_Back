package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;

@Data
public class TrialCohabitationRequestDetailDTO extends TrialCohabitationRequestDTO {

    private ShelterDTO shelter;
    private AdopterDTO adopter;
    private PetDTO pet;
    private TrialCohabitationDTO trialCohabitation;
}
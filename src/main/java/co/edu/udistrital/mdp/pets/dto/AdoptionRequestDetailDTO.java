package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdoptionRequestDetailDTO extends AdoptionRequestDTO {

    private PetDTO pet;
    private ShelterDTO shelter;
    private AdopterDTO adopter;
    private AdoptionDTO adoption;
    private TrialCohabitationDTO trialCohabition;
}

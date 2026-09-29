package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Detailed Data Transfer Object for Return.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReturnDetailDTO extends ReturnDTO {

    // Associated trial cohabitation
    private TrialCohabitationDTO trialCohabitation;
    
    // Associated adoption
    private AdoptionDTO adoption;
    
    // Shelter associated with the return
    private ShelterDTO shelter;
}

package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Detailed Data Transfer Object for Adoption Request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdoptionRequestDetailDTO extends AdoptionRequestDTO {

    // Pet requested
    private PetDTO pet;
    
    // Shelter involved
    private ShelterDTO shelter;
    
    // Adopter requesting
    private AdopterDTO adopter;
    
    // Associated adoption
    private AdoptionDTO adoption;
    
    // Associated trial cohabitation
    private TrialCohabitationDTO trialCohabition;
}

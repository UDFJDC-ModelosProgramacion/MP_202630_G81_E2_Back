package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;

/**
 * Data Transfer Object for Adoption Request.
 */
@Data
public class AdoptionRequestDTO extends RequestDTO {

    // Pet associated with the request
    private PetDTO pet;

    // Shelter associated with the request
    private ShelterDTO shelter;

    // Adopter associated with the request
    private AdopterDTO adopter;

    // Adoption associated with the request
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("adoptionRequest")
    private AdoptionDTO adoption;

    // Trial cohabitation associated with the request
    private TrialCohabitationDTO trialCohabitation;
}


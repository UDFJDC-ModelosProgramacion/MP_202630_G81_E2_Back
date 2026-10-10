package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

/**
 * Data Transfer Object for Adoption Request.
 */
@Data
public class AdoptionRequestDTO {

    // Unique identifier
    private Long id;
    
    // Status of the request
    private String status;
    
    // Date of the request
    private Date date;
    
    // Description of the request
    private String description;

    // Pet associated with the request
    private PetDTO pet;

    // Shelter associated with the request
    private ShelterDTO shelter;

    // Adopter associated with the request
    private AdopterDTO adopter;

    // Adoption associated with the request
    private AdoptionDTO adoption;

    // Trial cohabitation associated with the request
    private TrialCohabitationDTO trialCohabitation;
}


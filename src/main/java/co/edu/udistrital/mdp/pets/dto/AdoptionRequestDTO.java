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
}

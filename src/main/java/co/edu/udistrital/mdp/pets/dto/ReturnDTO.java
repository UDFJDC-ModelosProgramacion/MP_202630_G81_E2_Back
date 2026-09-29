package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

/**
 * Data Transfer Object for Return.
 */
@Data
public class ReturnDTO {

    // Unique identifier
    private Long id;
    
    // Date of the return
    private Date date;
    
    // Reason for the return
    private String reason;
    
    // Status of the return
    private String status;
}

package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

/**
 * Data Transfer Object for Message.
 */
@Data
public class MessageDTO {

    // Unique identifier
    private Long id;
    
    // Date of the message
    private Date date;
    
    // Time of the message
    private Date time;
    
    // Content of the message
    private String message;
    
    // Read status
    private Boolean isRead;
}

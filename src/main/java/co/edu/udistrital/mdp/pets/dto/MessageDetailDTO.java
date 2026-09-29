package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Detailed Data Transfer Object for Message.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageDetailDTO extends MessageDTO {

    // User who sent the message
    private UserDTO sendUser;
    
    // User who receives the message
    private UserDTO receivesUser;
}

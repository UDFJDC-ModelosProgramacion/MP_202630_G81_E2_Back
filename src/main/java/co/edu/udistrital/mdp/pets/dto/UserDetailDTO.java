package co.edu.udistrital.mdp.pets.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Detailed Data Transfer Object for User.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDetailDTO extends UserDTO {

    // Shelter associated with the user
    private ShelterDTO shelter;
    
    // Messages sent by the user
    private List<MessageDTO> sendMessages = new ArrayList<>();
    
    // Messages received by the user
    private List<MessageDTO> receiveMessages = new ArrayList<>();
    
    // Notifications of the user
    private List<NotificationDTO> notifications = new ArrayList<>();
}

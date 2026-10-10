package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

@Data
public class MessageDTO {

    private Long id;
    private Date date;
    private Date time;
    private String message;
    private Boolean isRead;
    
    // This
   private UserDTO sendUser;
   private UserDTO receivesUser;
}

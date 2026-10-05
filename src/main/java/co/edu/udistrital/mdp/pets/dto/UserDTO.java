package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

@Data
public class UserDTO{

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String passwrod;
    private String phone;
 
    private ShelterDTO shelter;
}
package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;

/**
 * Data Transfer Object for User.
 */
@Data
public class UserDTO {

    // Unique identifier
    private Long id;
    
    // First name of the user
    private String firstName;
    
    // Last name of the user
    private String lastName;
    
    // Email address of the user
    private String email;
    
    // Password of the user
    private String password;
    
    // Phone number of the user
    private String phone;

    private ShelterDTO shelter;
}

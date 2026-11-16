package co.edu.udistrital.mdp.pets.dto;

import co.edu.udistrital.mdp.pets.entities.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticationDTO {
	private Long id;
	private String email;
	private UserRole role;
	private Long shelterId;
}

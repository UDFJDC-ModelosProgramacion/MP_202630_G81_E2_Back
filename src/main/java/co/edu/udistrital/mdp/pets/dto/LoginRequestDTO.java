package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;
import lombok.ToString;

@Data
public class LoginRequestDTO {
	private String email;
	@ToString.Exclude
	private String password;
}

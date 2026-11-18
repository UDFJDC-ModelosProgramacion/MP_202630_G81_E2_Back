package co.edu.udistrital.mdp.pets.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import co.edu.udistrital.mdp.pets.entities.UserRole;
import lombok.Data;
import lombok.ToString;

@Data
public class UserRoleUpdateDTO {
	private UserRole role;
	private Long shelterId;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@ToString.Exclude
	private String password;
}

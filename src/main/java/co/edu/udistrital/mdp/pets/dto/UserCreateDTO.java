package co.edu.udistrital.mdp.pets.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserCreateDTO extends UserDTO {
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;
}

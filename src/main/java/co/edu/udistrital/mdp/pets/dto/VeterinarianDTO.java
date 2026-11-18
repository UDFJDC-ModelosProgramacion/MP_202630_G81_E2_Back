package co.edu.udistrital.mdp.pets.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
public class VeterinarianDTO extends UserDTO {
	private String specialization;
	private String availability;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@ToString.Exclude
	private String password;
	private Long shelterId;
}

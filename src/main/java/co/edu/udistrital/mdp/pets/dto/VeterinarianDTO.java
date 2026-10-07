package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VeterinarianDTO extends UserDTO {
	private String specialization;
	private String availability;
}

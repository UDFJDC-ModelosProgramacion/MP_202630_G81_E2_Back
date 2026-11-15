package co.edu.udistrital.mdp.pets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CsrfTokenDTO {
	private String token;
}

package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;
@Data
public class VeterinarianDTO {
	private Long id;
	private String firstName;
	private String lastName;
	private String email;
	private String password;
	private String phone;
	private String specialization;
	private String availability;
}
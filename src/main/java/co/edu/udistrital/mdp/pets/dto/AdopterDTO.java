package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;

@Data
public class AdopterDTO {
	private Long id;
	private String firstName;
	private String lastName;
	private String email;
	private String password;
	private String phone;
	private String address;
	private String nationalId;
	private String occupation;
	private Double earnings;
	private String housingType;
	private String allergies;
	private Boolean hasChildren;
	private Boolean hasOtherPets;
}
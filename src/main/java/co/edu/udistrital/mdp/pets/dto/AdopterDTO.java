package co.edu.udistrital.mdp.pets.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.ToString;

@Data
public class AdopterDTO {
	private Long id;
	private String firstName;
	private String lastName;
	private String email;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@ToString.Exclude
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
	private Boolean active;

	private ShelterDTO shelter;
}
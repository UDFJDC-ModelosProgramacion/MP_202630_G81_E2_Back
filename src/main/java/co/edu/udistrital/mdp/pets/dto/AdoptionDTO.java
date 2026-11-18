package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
public class AdoptionDTO {
	private Long id;
	private Date date;
	private String status;
	private String importantNotes;

	// Pet
	private PetDTO pet;

	// Shelter
	private ShelterDTO shelter;

	// Adopter
	private AdopterDTO adopter;

	// AdoptionRequest
	@JsonIgnoreProperties({ "adoption", "trialCohabitation" })
	private AdoptionRequestDTO adoptionRequest;
}

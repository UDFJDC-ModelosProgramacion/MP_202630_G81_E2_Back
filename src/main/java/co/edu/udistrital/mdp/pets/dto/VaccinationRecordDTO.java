package co.edu.udistrital.mdp.pets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
public class VaccinationRecordDTO {
	private Long id;

	
	@JsonIgnoreProperties("vaccinationRecord")
	private PetDTO pet;
}
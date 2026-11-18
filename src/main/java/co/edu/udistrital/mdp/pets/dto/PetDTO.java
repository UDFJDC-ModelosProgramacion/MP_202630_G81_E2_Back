package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PetDTO {
    private Long id;
    private String name;
	private String species;
	private String breed;
	private Integer age;
	private String sex;
	private String size;
	private String healthStatus;
	private String description;
	private Date admissionDate;
	private String temperament;
	private String specificNeeds;
	private Boolean compatibilityChildren;
	private Boolean compatibilityOtherPets;
	private String activityLevel;
	private String requiredSpace;
	
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private VaccinationRecordDTO vaccinationRecord;
	private ShelterDTO shelter;
	private Boolean active;
}
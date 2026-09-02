package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import lombok.Data;

@Data
@Entity
public class PetEntity extends BaseEntity {

	private Integer petID;
	private String name;
	private String species;
	private String breed;
	private Integer age;
	private String sex;
	private String size;
	private String healthStatus;
	private String description;

	@Temporal(TemporalType.DATE)
	private Date admissionDate;

	private String temperament;
	private String specificNeeds;
	private Boolean compatibilityChildren;
	private Boolean compatibilityOtherPets;
	private String activityLevel;
	private String requiredSpace;

}
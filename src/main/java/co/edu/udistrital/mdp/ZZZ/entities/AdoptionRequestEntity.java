package co.edu.udistrital.mdp.ZZZ.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import lombok.Data;
import uk.co.jemos.podam.common.PodamExclude;

@Data
@Entity
public class AdoptionRequestEntity extends RequestEntity {

	@PodamExclude
	@ManyToOne
	private PetEntity pet;

	@PodamExclude
	@ManyToOne
	private ShelterEntity shelter;

	@PodamExclude
	@ManyToOne
	private AdopterEntity adopter;

	@PodamExclude
	@OneToOne(mappedBy = "adoptionRequest")
	private AdoptionEntity adoption;

	@PodamExclude 
	@OneToOne(mappedBy = "adoptionRequest")
	private TrialCohabitationEntity trialCohabition;
}
package co.edu.udistrital.mdp.ZZZ.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.co.jemos.podam.common.PodamExclude;

@Data
@EqualsAndHashCode(callSuper = true)
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

	// Si la solicitud es aprobada, genera UNA adopción (0..1).
	@PodamExclude
	@OneToOne(mappedBy = "adoptionRequest", cascade = CascadeType.PERSIST, orphanRemoval = true)
	private AdoptionEntity adoption;
}
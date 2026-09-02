package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.co.jemos.podam.common.PodamExclude;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class AdoptionEntity extends BaseEntity {

	@Temporal(TemporalType.DATE)
	private Date date;

	private String status;

	private String importantNotes;

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
	@OneToOne
	private AdoptionRequestEntity adoptionRequest;

	@PodamExclude
	@OneToOne
	private TrialCohabitationEntity trialCohabitation;

	// Devolución posterior a la adopción ya formalizada (0..1). Dueña: ReturnEntity.
	@PodamExclude
	@OneToOne(mappedBy = "adoption", cascade = CascadeType.PERSIST, orphanRemoval = true)
	private ReturnEntity returnAfterAdoption;
}
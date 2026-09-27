package co.edu.udistrital.mdp.pets.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import lombok.Data;

import uk.co.jemos.podam.common.PodamExclude;

@Data
@Entity
public class TrialCohabitationRequestEntity extends RequestEntity {

	/** Fecha de inicio del periodo de convivencia solicitado. */
	@Temporal(TemporalType.DATE)
	private Date startDate;

	/** Fecha de fin del periodo de convivencia solicitado. */
	@Temporal(TemporalType.DATE)
	private Date endDate;

	@PodamExclude
	@ManyToOne
	private ShelterEntity shelter;

	@PodamExclude
	@ManyToOne
	private AdopterEntity adopter;

	@PodamExclude
	@OneToOne(mappedBy = "trialCohabitationRequest")
	private TrialCohabitationEntity trialCohabitation;

	@PodamExclude
	@ManyToOne
	private PetEntity pet;
}
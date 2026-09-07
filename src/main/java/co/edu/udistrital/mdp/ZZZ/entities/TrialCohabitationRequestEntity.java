package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Temporal;

import lombok.Data;

import uk.co.jemos.podam.common.PodamExclude;

@Data
@Entity
public class TrialCohabitationRequestEntity extends RequestEntity {

	private String status;

	@Temporal(TemporalType.DATE)
	private Date date;

	private String description;

	@PodamExclude
	@ManyToOne
	private ShelterEntity shelter;

	@PodamExclude
	@ManyToOne
	private AdopterEntity adopter;

	@PodamExclude
	@OneToOne(mappedBy = "trialCohabitationRequest")
	private TrialCohabitationEntity trialCohabitation;
}
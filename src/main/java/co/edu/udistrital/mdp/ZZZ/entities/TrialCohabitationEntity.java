package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import co.edu.udistrital.mdp.ZZZ.repositories.AdopterRepository;
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
public class TrialCohabitationEntity extends BaseEntity {

	@Temporal(TemporalType.DATE)
	private Date startDate;

	@Temporal(TemporalType.DATE)
	private Date endDate;

	private String status;

	private String observations;

	@PodamExclude
	@ManyToOne
	private ShelterEntity shelter;

	@PodamExclude
	@ManyToOne
	private AdopterRepository adopter;
	
	@PodamExclude
	@OneToOne
	private TrialCohabitationRequestEntity trialCohabitationRequest;
	
	@PodamExclude
	@OneToOne(mappedBy = "trialCohabitation")
	private ReturnEntity returnDuringTrial;

	@PodamExclude 
	@OneToOne 
	private AdoptionRequestEntity adoptionRequest;
}
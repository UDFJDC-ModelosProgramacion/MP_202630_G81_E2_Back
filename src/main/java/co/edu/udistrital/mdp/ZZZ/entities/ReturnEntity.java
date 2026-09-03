package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.co.jemos.podam.common.PodamExclude;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class ReturnEntity extends BaseEntity {

	@Temporal(TemporalType.DATE)
	private Date date;

	private String reason;

	
	@PodamExclude
	@OneToOne
	private TrialCohabitationEntity trialCohabitation;

	
	@PodamExclude
	@OneToOne
	private AdoptionEntity adoption;
}
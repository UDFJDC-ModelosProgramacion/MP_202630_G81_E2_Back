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

	// Devolución ocurrida durante la convivencia de prueba (0..1: mutuamente excluyente con "adoption").
	@PodamExclude
	@OneToOne
	private TrialCohabitationEntity trialCohabitation;

	// Devolución ocurrida después de una adopción ya formalizada (0..1: mutuamente excluyente con "trialCohabitation").
	@PodamExclude
	@OneToOne
	private AdoptionEntity adoption;
}
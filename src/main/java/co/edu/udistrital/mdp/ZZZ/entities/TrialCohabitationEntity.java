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
public class TrialCohabitationEntity extends BaseEntity {

	@Temporal(TemporalType.DATE)
	private Date startDate;

	@Temporal(TemporalType.DATE)
	private Date endDate;

	private String status;

	private String observations;

	@PodamExclude
	@ManyToOne
	private PetEntity pet;

	@PodamExclude
	@ManyToOne
	private ShelterEntity shelter;

	// Dueña: TrialCohabitationRequestEntity (mappedBy allá). Aquí solo el lado inverso.
	@PodamExclude
	@OneToOne
	private TrialCohabitationRequestEntity trialCohabitationRequest;

	// Puede terminar en una devolución durante el periodo de prueba (0..1). Dueña: ReturnEntity.
	@PodamExclude
	@OneToOne(mappedBy = "trialCohabitation", cascade = CascadeType.PERSIST, orphanRemoval = true)
	private ReturnEntity returnDuringTrial;

	// Puede derivar en una adopción exitosa (0..1). Dueña: AdoptionEntity.
	@PodamExclude
	@OneToOne(mappedBy = "trialCohabitation")
	private AdoptionEntity adoption;
}
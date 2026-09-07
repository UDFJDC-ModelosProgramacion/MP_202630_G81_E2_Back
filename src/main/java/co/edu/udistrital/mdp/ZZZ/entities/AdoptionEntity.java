package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
	@OneToOne(mappedBy = "adoption")
	private ReturnEntity returnAfterAdoption;

	@PodamExclude 
	@OneToMany(mappedBy = "adoption")
	private List<ReviewEntity> reviews = new ArrayList<>();

	@PodamExclude
	@OneToMany(mappedBy = "adoption")
	private List<FollowUpEntity> followUps = new ArrayList<>();
}
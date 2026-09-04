package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import uk.co.jemos.podam.common.PodamExclude;

import jakarta.persistence.Entity;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import co.edu.udistrital.mdp.ZZZ.entities.AdoptionRequestEntity;
import co.edu.udistrital.mdp.ZZZ.entities.AdoptionEntity;
import co.edu.udistrital.mdp.ZZZ.entities.ReviewEntity;
import co.edu.udistrital.mdp.ZZZ.entities.ShelterEntity;

import lombok.Data;

@Data
@Entity
public class PetEntity extends BaseEntity {

	private Integer petID;
	private String name;
	private String species;
	private String breed;
	private Integer age;
	private String sex;
	private String size;
	private String healthStatus;
	private String description;

	@Temporal(TemporalType.DATE)
	private Date admissionDate;

	private String temperament;
	private String specificNeeds;
	private Boolean compatibilityChildren;
	private Boolean compatibilityOtherPets;
	private String activityLevel;
	private String requiredSpace;
	
	@PodamExclude
	@OneToMany(mappedBy = "pet")
	private List<PhotoEntity> photos = new ArrayList<>();

	@PodamExclude
	@OneToOne(mappedBy = "pet")
	private VaccineRecordEntity vaccinationRecord;

	@PodamExclude
	@OneToMany(mappedBy = "pet")
	private List<AdoptionRequestEntity> adoptionRequests = new ArrayList<>();

	@PodamExclude
	@OneToMany(mappedBy = "pet")
	private List<AdoptionEntity> adoptions = new ArrayList<>();

	@PodamExclude
	@OneToMany(mappedBy = "pet")
	private List<ReviewEntity> reviews = new ArrayList<>();

	@PodamExclude
	@ManyToOne
	private ShelterEntity shelter;
}
package co.edu.udistrital.mdp.pets.entities;

import jakarta.persistence.Entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import uk.co.jemos.podam.common.PodamExclude;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
public class VaccinationRecordEntity extends BaseEntity {
	
	@PodamExclude
	@OneToOne
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private PetEntity pet;

	@PodamExclude
	@OneToMany(mappedBy = "vaccinationRecord", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<VaccineEntity> vaccines = new ArrayList<>();
}
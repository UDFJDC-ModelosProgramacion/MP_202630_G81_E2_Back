package co.edu.udistrital.mdp.ZZZ.entities;

import jakarta.persistence.Entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import uk.co.jemos.podam.common.PodamExclude;

import lombok.Data;

@Data
@Entity
public class VaccinationRecordEntity extends BaseEntity {
	
	@PodamExclude
	@OneToOne
	private PetEntity pet;

	@PodamExclude
	@OneToMany(mappedBy = "vaccinationRecord", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<VaccineEntity> vaccines = new ArrayList<>();
}
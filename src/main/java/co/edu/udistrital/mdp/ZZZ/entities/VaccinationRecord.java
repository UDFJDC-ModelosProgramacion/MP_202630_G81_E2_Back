package co.edu.udistrital.mdp.ZZZ.entities;

import jakarta.persistence.Entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import uk.co.jemos.podam.common.PodamExclude;

import lombok.Data;

@Data
@Entity
public class VaccinationRecord extends BaseEntity {

	private String recordId;
	
	@PodamExclude
	@OneToOne
	private PetEntity pet;

	@PodamExclude
	@OneToMany(mappedBy = "vaccinationRecord")
	private List<VaccineEntity> vaccines = new ArrayList<>();
}
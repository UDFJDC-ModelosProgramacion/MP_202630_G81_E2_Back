package co.edu.udistrital.mdp.ZZZ.entities;

import jakarta.persistence.Entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import uk.co.jemos.podam.common.PodamExclude;

import lombok.Data;

@Data
@Entity
public class VaccineRecordEntity extends BaseEntity {

	private String recordId;
	
	@PodamExclude
	@OneToOne
	@JoinColumn(name = "pet_id")
	private PetEntity pet;

	@PodamExclude
	@OneToMany(mappedBy = "vaccinationRecord")
	private List<VaccineEntity> vaccines = new ArrayList<>();

}
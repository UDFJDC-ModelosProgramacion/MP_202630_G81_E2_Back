package co.edu.udistrital.mdp.ZZZ.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import uk.co.jemos.podam.common.PodamExclude;

import lombok.Data;

@Data
@Entity
public class PhotoEntity extends BaseEntity {

	private Integer photoId;
	private String url;
	private String type;
	private String description;
	
	@PodamExclude
	@ManyToOne
	private PetEntity pet;

}
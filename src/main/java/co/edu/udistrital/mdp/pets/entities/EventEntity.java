package co.edu.udistrital.mdp.pets.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
 
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.co.jemos.podam.common.PodamExclude;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class EventEntity extends BaseEntity {

	private String name;
	private String type;
	@Temporal(TemporalType.DATE)
	private Date date;
	private String time;
	private String description;
	private String location;

	@PodamExclude 
	@ManyToOne
	private ShelterEntity shelter;
}

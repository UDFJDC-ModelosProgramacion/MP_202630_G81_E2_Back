package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
 
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.co.jemos.podam.common.PodamExclude;

// Event organized by a shelter
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class EventEntity extends BaseEntity {

	// Name of the event
	private String name;

	private String type;
	
	@Temporal(TemporalType.DATE)
	private Date date;

	// Time the event starts
	private String time;

	private String description;

	// Where the event is held
	private String location;

	@PodamExclude 
	@ManyToOne
	private ShelterEntity shelter;
}

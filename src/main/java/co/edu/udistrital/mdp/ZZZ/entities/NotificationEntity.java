package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import lombok.Data;
import uk.co.jemos.podam.common.PodamExclude;


@Data
@Entity
public class NotificationEntity extends BaseEntity {

	
	private String message;

	
	@Temporal(TemporalType.DATE)
	private Date date;

	@Temporal(TemporalType.TIMESTAMP)
	private Date time;

	
	private String content;

	
	private String channel;

	
	private Boolean sent;

	@PodamExclude 
	@ManyToOne
	private UserEntity user;
}
package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import lombok.Data;
import uk.co.jemos.podam.common.PodamExclude;

// Base notification class, extended by Email, SMS and Push notifications
@Data
@Entity
public class NotificationEntity extends BaseEntity {

	// Short notification message
	private String message;

	// Date the notification was sent
	@Temporal(TemporalType.DATE)
	private Date date;

	@Temporal(TemporalType.TIMESTAMP)
	private Date time;

	// Full content of the notification
	private String content;

	@PodamExclude 
	@ManyToOne
	private UserEntity user;
}
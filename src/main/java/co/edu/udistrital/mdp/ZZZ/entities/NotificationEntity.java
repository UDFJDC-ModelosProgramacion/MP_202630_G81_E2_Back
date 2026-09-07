package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import lombok.Data;
import lombok.EqualsAndHashCode;

// Base notification class, extended by Email, SMS and Push notifications
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class NotificationEntity extends BaseEntity {

	// Short notification message
	private String message;

	// Date the notification was sent
	@Temporal(TemporalType.DATE)
	private Date date;

	// Full content of the notification
	private String content;
}

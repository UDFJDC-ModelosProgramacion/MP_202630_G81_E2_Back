package co.edu.udistrital.mdp.ZZZ.entities;

import jakarta.persistence.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

// Notification sent via email
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class EmailNotificationEntity extends NotificationEntity {

	// Recipient email address
	private String email;
}

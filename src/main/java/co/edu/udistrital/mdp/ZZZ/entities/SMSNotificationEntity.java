package co.edu.udistrital.mdp.ZZZ.entities;

import jakarta.persistence.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

// Notification sent via SMS
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class SMSNotificationEntity extends NotificationEntity {

	// Recipient phone number
	private String phone;
}

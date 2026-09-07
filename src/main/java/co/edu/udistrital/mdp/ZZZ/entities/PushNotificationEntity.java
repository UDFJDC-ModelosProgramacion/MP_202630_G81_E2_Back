package co.edu.udistrital.mdp.ZZZ.entities;

import jakarta.persistence.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

// Notification sent as a push alert to a device
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class PushNotificationEntity extends NotificationEntity {

	// Target device identifier
	private String device;
}

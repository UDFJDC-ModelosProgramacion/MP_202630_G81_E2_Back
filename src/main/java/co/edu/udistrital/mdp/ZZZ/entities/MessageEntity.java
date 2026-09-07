package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import lombok.Data;
import uk.co.jemos.podam.common.PodamExclude;

// Message sent between users in the platform
@Data
@Entity
public class MessageEntity extends BaseEntity {

	// When the message was sent
	@Temporal(TemporalType.TIMESTAMP)
	private Date time;

	// Text content of the message
	private String message;

	// Date the message was created
	@Temporal(TemporalType.DATE)
	private Date date;

	// Whether the message has been read
	private Boolean isRead;

	@PodamExclude
	@ManyToOne
	private UserEntity sendUser;

	@PodamExclude
	@ManyToOne
	private UserEntity receivesUser;
}
// Review parameters in UML 
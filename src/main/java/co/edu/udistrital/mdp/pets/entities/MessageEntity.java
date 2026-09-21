package co.edu.udistrital.mdp.pets.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import lombok.Data;
import uk.co.jemos.podam.common.PodamExclude;

@Data
@Entity
public class MessageEntity extends BaseEntity {

	@Temporal(TemporalType.DATE)
	private Date date;
	@Temporal(TemporalType.TIMESTAMP)
	private Date time;
	private String message;
	private Boolean isRead;

	@PodamExclude
	private Boolean hiddenForSender = false;

	@PodamExclude
	private Boolean hiddenForReceiver = false;
		
	@PodamExclude
	@ManyToOne
	private UserEntity sendUser;

	@PodamExclude
	@ManyToOne
	private UserEntity receivesUser;
}
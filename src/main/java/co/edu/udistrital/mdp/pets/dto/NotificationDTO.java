package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;

import lombok.Data;

@Data
public class NotificationDTO {
	private Long id;
	private String message;
	private Date date;
	private Date time;
	private String content;
	private String channel;
	private Boolean sent;

	private UserDTO user;
}

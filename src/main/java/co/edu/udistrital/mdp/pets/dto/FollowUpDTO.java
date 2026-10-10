package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

@Data
public class FollowUpDTO {
	private Long id;
	private Date date;
	private String observation;
}
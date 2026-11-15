package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;

import lombok.Data;

@Data
public class FollowUpRequestDTO {
	private Date date;
	private String observation;
	private Long adoptionId;
	private AdoptionDTO adoption;
	private Long veterinarianId;
	private VeterinarianDTO veterinarian;
}

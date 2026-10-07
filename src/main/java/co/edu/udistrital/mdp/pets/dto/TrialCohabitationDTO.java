package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

@Data
public class TrialCohabitationDTO {
	private Long id;
	private Date startDate;
	private Date endDate;
	private String status;
	private String observations;

	private ShelterDTO shelter;
	private AdopterDTO adopter;
	// TrialCohabitationRequest
	// Return
	// AdoptionRequest
}
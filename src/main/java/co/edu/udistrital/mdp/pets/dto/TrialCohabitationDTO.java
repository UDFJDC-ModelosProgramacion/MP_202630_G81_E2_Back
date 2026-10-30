package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TrialCohabitationDTO {
	private Long id;
	private Date startDate;
	private Date endDate;
	private String status;
	private String observations;

	private AdopterDTO adopter;

	private ShelterDTO shelter;

	/** Se acepta en las peticiones, pero no se serializa para no ciclar con la solicitud. */
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private TrialCohabitationRequestDTO trialCohabitationRequest;
}
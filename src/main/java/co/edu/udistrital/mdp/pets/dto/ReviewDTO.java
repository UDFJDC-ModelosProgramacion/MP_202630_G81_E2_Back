package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ReviewDTO {
	private Long id;
	private Integer rating;
	private String comment;
	private Date date;
	private Date time;
	private PetDTO pet;
	private AdoptionDTO adoption;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private ReviewAuthorDTO author;
}
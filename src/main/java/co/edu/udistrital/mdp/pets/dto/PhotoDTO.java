package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;

@Data
public class PhotoDTO {
	private Long id;
	private String url;
	private String type;
	private String description;
}
package co.edu.udistrital.mdp.ZZZ.entities;

import jakarta.persistence.Entity;

import lombok.Data;

@Data
@Entity
public class PhotoEntity extends BaseEntity {

	private Integer photoId;
	private String url;
	private String type;
	private String description;

}
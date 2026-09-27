
package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

@Data
public class ReviewDTO {
	private Long id;
	private Integer rating;
	private String comment;
	private Date date;
	private Date time;
}
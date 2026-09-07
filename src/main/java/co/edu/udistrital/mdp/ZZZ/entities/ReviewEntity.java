package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import lombok.Data;
import uk.co.jemos.podam.common.PodamExclude;

@Data
@Entity
public class ReviewEntity extends BaseEntity {

    private Integer rating;
    private String comment;

    @Temporal(TemporalType.DATE)
    private Date date;

    @Temporal(TemporalType.TIMESTAMP)
	private Date time;

    @PodamExclude
    @ManyToOne
    private PetEntity pet;

    @PodamExclude 
    @ManyToOne
    private AdoptionEntity adoption;
}

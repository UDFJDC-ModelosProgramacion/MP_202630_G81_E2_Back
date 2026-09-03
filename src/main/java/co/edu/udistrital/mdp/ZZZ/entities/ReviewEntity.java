package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Data
@Entity
public class ReviewEntity extends BaseEntity{
    
    private Integer rating;
    private String comment;
    
    @Temporal(TemporalType.DATE)
    private Date date;

    private String time;
}
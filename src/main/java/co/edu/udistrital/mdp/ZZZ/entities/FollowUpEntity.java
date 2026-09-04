package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Data
@Entity
public class FollowUpEntity extends BaseEntity{
    
    @Temporal (TemporalType.DATE)
    private Date date;

    private String observation;

     public void setObservation(String observation){
        this.observation = observation;
    }
}

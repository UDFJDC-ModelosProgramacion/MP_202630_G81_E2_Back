package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Data
@Entity
public class MedicalEventEntity extends BaseEntity{
    
    @Temporal(TemporalType.DATE)
    private Date date;

    private String type;
    private String description;

    public void setDescription(String description){
        this.description = description;
    }
}

package co.edu.udistrital.mdp.pets.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import lombok.Data;

@Data
@Entity
public class EventEntity extends BaseEntity {

    private String name;
    private String type;

    @Temporal(TemporalType.DATE)
    private Date date;

    private String time;
    private String description;
    private String location;

    // TODO: verificar si pertenece a un Shelter (@ManyToOne)
}

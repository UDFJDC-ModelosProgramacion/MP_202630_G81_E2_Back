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

    private String time;

    @PodamExclude
    @ManyToOne
    private PetEntity pet;

    // TODO: ¿también referencia al Adopter que la escribió?
    // OJO: ya existe un ReviewEntity.java en tu proyecto (según tu captura de
    // pantalla) — compara con esta versión antes de sobrescribir, puede que
    // un compañero ya lo haya implementado.
}

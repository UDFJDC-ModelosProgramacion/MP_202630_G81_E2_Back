package co.edu.udistrital.mdp.pets.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import lombok.Data;
import uk.co.jemos.podam.common.PodamExclude;

@Data
@Entity
public class ShelterEntity extends BaseEntity {

    private String name;
    private String city;
    private String nit;

    @PodamExclude
    @OneToMany(mappedBy = "shelter", cascade = CascadeType.PERSIST)
    private List<PetEntity> pets = new ArrayList<>();

    // TODO: returnPolicy() es del patrón Strategy (Ciclo 2), probablemente
    // no se persiste como relación JPA
}

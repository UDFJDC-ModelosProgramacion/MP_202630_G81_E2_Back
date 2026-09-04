package co.edu.udistrital.mdp.pets.entities;

import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
public class VeterinarianEntity extends UserEntity {

    private String specialization;
    private String availability;

    // TODO: verificar si se asocia a un Shelter específico (@ManyToOne)
    // TODO: implementar ObserverFollowUp (patrón Observer, Ciclo 3)
}

package co.edu.udistrital.mdp.pets.entities;

import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
public class AdopterEntity extends UserEntity {

    private String address;
    private String nationalId;
    private String occupation;
    private Double earnings;
    private String housingType;
    private String allergies;
    private Boolean hasChildren;
    private Boolean hasOtherPets;

    // TODO: relación con AdoptionRequest/TrialCohabitationRequest (Camilo, "Request")
    // TODO: implementar ObserverRequest&Adoption y ObserverFollowUp (patrón Observer, Ciclo 3)
}

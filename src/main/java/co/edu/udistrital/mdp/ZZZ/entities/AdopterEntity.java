package co.edu.udistrital.mdp.ZZZ.entities;


import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Data;
import uk.co.jemos.podam.common.PodamExclude;

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

    @PodamExclude
    @OneToMany(mappedBy = "adopter")
    private List<TrialCohabitationRequestEntity> cohabitationRequests = new ArrayList<>();
}

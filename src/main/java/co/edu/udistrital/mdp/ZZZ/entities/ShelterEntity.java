package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.ArrayList;
import java.util.List;

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
    @OneToMany(mappedBy = "shelter")
    private List<PetEntity> pets = new ArrayList<>();

    @PodamExclude
    @OneToMany(mappedBy = "shelter")
    private List<PhotoEntity> photos = new ArrayList<>();

    @PodamExclude
    @OneToMany(mappedBy = "shelter")
    private List<TrialCohabitationRequestEntity> cohabitationRequests = new ArrayList<>();
}

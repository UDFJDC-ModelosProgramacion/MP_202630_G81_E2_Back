package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import uk.co.jemos.podam.common.PodamExclude;

@Data
@Entity
public class VeterinarianEntity extends UserEntity {

    private String specialization;
    private String availability;

    @PodamExclude 
    @OneToMany(mappedBy = "veterinarian")
    private List<FollowUpEntity> followUps = new ArrayList<>();

    @PodamExclude
    @OneToMany(mappedBy = "veterinarian")
    private List<MedicalEventEntity> medicalEvents = new ArrayList<>();

    @PodamExclude
    @ManyToOne
    private ShelterEntity shelter;
}

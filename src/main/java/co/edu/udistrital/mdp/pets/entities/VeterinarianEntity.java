package co.edu.udistrital.mdp.pets.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.co.jemos.podam.common.PodamExclude;

@Data
@Entity
public class VeterinarianEntity extends UserEntity {

    private String specialization;
    private String availability;

    @PodamExclude 
    @OneToMany(mappedBy = "veterinarian")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<FollowUpEntity> followUps = new ArrayList<>();

    @PodamExclude
    @OneToMany(mappedBy = "veterinarian")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MedicalEventEntity> medicalEvents = new ArrayList<>();
}
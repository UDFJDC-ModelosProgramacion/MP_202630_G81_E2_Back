package co.edu.udistrital.mdp.pets.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import uk.co.jemos.podam.common.PodamExclude;

@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class UserEntity extends BaseEntity {

    private String firstName;
    private String lastName;
    private String email;
    @JsonIgnore
    @ToString.Exclude
    private String password;
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private UserRole role = UserRole.ADOPTER;

    @PodamExclude 
    @ManyToOne
    private ShelterEntity shelter;

    @PodamExclude
    @OneToMany(mappedBy = "sendUser")
    private List<MessageEntity> sendMessages = new ArrayList<>();

    @OneToMany(mappedBy = "receivesUser")
    private List<MessageEntity> receiveMessages = new ArrayList<>();

    @PodamExclude
    @OneToMany(mappedBy = "user")
    private List<NotificationEntity> notifications = new ArrayList<>();

    @PrePersist
    private void assignDefaultRole() {
        if (role == null)
            role = UserRole.ADOPTER;
    }
}

package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import uk.co.jemos.podam.common.PodamExclude;

@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class UserEntity extends BaseEntity {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;

    @PodamExclude 
    @ManyToOne
    private ShelterEntity shelter;

    @PodamExclude
    @OneToMany(mappedBy = "user")
    private List<MessageEntity> sendMessages = new ArrayList<>();

    @PodamExclude
    @OneToMany(mappedBy = "user")
    private List<MessageEntity> receiveMessages = new ArrayList<>();

    @PodamExclude
    @OneToMany(mappedBy = "user")
    private List<NotificationEntity> notifications = new ArrayList<>();
}

package co.edu.udistrital.mdp.ZZZ.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import lombok.Data;

@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class UserEntity extends BaseEntity {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;

}

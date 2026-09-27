package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;

@Data
public class ShelterDTO {
    private Long id;
    private String name;
    private String city;
    private String nit;
    private String location;
}
package co.edu.udistrital.mdp.pets.dto;

import lombok.Data;

@Data
public class ShelterDTO {
    private Long id;
    private String name;
    private String city;
    private String nit;
    private String location;

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private java.util.List<UserDTO> users = new java.util.ArrayList<>();
}

package co.edu.udistrital.mdp.pets.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ShelterDTO {
    private Long id;
    private String name;
    private String city;
    private String nit;
    private String location;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<UserDTO> users = new ArrayList<>();
}
package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;

import lombok.Data;

@Data
public class RequestDTO {
    private Long id;
    private String status;
    private Date date;
    private String description;
}
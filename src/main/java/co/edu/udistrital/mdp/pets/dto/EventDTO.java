package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

@Data
public class EventDTO {
    private Long id;
    private String name;
    private String type;
    private Date date;
    private String time;
    private String description;
    private String location;

    private ShelterDTO shelter;
}
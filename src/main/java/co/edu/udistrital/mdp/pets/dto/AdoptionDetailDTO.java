package co.edu.udistrital.mdp.pets.dto;

import java.util.List;
import java.util.ArrayList;

import lombok.Data;

@Data
public class AdoptionDetailDTO extends AdoptionDTO {

    private List<ReviewDTO> reviews = new ArrayList<>();
    private List<FollowUpDTO> followUps = new ArrayList<>();
}
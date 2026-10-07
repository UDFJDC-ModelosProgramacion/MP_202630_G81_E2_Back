package co.edu.udistrital.mdp.pets.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class AdopterDetailDTO extends AdopterDTO {

    // CohabitationRequests
    // AdoptionRequests
    private List<TrialCohabitationDTO> trialCohabitions = new ArrayList<>();
    // Adoptions
}
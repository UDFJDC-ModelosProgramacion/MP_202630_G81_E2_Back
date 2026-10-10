package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

@Data
public class TrialCohabitationRequestDTO extends RequestDTO {
    private Date startDate;
    private Date endDate;

    private ShelterDTO shelter;
    private AdopterDTO adopter;
    private PetDTO pet;
    private TrialCohabitationDTO trialCohabitation;
}
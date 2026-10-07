package co.edu.udistrital.mdp.pets.dto;

import java.util.Date;
import lombok.Data;

/**
 * Data Transfer Object for Return.
 */
@Data
public class ReturnDTO{

    private Long id;
    private Date date;
    private String reazon;
    private String status;
    
    // This
   private TrialCohabitationDTO trialCohabitation;
   private AdoptionDTO adoption;
   private ShelterDTO shelter;
}

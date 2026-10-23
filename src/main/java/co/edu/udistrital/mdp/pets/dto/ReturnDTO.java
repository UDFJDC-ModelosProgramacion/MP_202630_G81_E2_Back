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
    private String reason;
    private String status;

    public String getReason() {
        return reason != null ? reason : reazon;
    }

    public void setReason(String reason) {
        this.reason = reason;
        if (this.reazon == null) {
            this.reazon = reason;
        }
    }

    public String getReazon() {
        return reazon != null ? reazon : reason;
    }

    public void setReazon(String reazon) {
        this.reazon = reazon;
        if (this.reason == null) {
            this.reason = reazon;
        }
    }
    
    // This
   private TrialCohabitationDTO trialCohabitation;
   @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"returnAfterAdoption", "returns", "adoptionRequest"})
   private AdoptionDTO adoption;
   private ShelterDTO shelter;
}

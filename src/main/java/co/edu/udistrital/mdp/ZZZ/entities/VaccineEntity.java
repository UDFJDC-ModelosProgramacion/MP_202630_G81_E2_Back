package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import lombok.Data;

@Data
@Entity
public class VaccineEntity extends BaseEntity {

	private Integer vaccineId;
	private String name;

	@Temporal(TemporalType.DATE)
	private Date administrationDate;

	@Temporal(TemporalType.DATE)
	private Date nextAdministration;

	private Boolean status;

}
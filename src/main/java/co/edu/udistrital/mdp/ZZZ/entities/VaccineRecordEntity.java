package co.edu.udistrital.mdp.ZZZ.entities;

import jakarta.persistence.Entity;

import lombok.Data;

@Data
@Entity
public class VaccineRecordEntity extends BaseEntity {

	private String recordId;

}
package co.edu.udistrital.mdp.ZZZ.entities;

import java.util.Date;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class RequestEntity extends BaseEntity {

	private String status;

	@Temporal(TemporalType.DATE)
	private Date date;

	private String description;

	public void setStatus(String status) {
		this.status = status;
	}
}
package co.edu.udistrital.mdp.pets.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.udistrital.mdp.pets.entities.VaccinationRecordEntity;

@Repository
public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecordEntity, Long> {

	Optional<VaccinationRecordEntity> findByPetId(Long petId);
}
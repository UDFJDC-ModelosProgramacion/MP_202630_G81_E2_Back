package co.edu.udistrital.mdp.pets.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.udistrital.mdp.pets.entities.VaccineEntity;

@Repository
public interface VaccineRepository extends JpaRepository<VaccineEntity, Long> {
}
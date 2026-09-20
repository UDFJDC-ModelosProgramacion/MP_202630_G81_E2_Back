package co.edu.udistrital.mdp.pets.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import co.edu.udistrital.mdp.pets.entities.AdoptionRequestEntity;

@Repository
public interface AdoptionRequestRepository extends JpaRepository<AdoptionRequestEntity, Long> {
    List<AdoptionRequestEntity> findByAdopterIdAndPetIdAndStatus(Long adopterId, Long petId, String status);
}
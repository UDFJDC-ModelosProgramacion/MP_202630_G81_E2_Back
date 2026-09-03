package co.edu.udistrital.mdp.ZZZ.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.udistrital.mdp.ZZZ.entities.TrialCohabitationRequestEntity;

@Repository
public interface TrialCohabitationRequestRepository extends JpaRepository<TrialCohabitationRequestEntity, Long> {
}
package co.edu.udistrital.mdp.pets.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.udistrital.mdp.pets.entities.EventEntity;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {
}

package Conversational_AI.Database_Integration.repository;

import Conversational_AI.Database_Integration.entity.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantRepository extends JpaRepository<Plant, Long> { // Use Long or Integer matching Plant.java
}
package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.RapportEnqueteSatisfaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RapportEnqueteSatisfactionRepository extends JpaRepository<RapportEnqueteSatisfaction, Long> {

    Optional<RapportEnqueteSatisfaction> findByStageId(Long stageId);
}

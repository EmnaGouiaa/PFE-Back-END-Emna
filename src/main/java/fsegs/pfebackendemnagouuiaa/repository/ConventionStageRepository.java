package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConventionStageRepository extends JpaRepository<ConventionStage, Long> {

    Optional<ConventionStage> findByStageId(Long stageId);

    Optional<ConventionStage> findByNumConv(Integer numConv);

    boolean existsByStageId(Long stageId);
}
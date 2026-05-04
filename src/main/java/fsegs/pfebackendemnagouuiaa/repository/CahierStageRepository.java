package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CahierStageRepository extends JpaRepository<CahierStage, Long> {

    Optional<CahierStage> findByStageId(Long stageId);

    boolean existsByStageId(Long stageId);
}
package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FicheEvaluationRepository extends JpaRepository<FicheEvaluation, Long> {

    List<FicheEvaluation> findByStageId(Long stageId);

    List<FicheEvaluation> findByReunionFinaleId(Long reunionFinaleId);

    List<FicheEvaluation> findByStageIdAndReunionFinaleId(Long stageId, Long reunionFinaleId);

    Optional<FicheEvaluation> findFirstByStageId(Long stageId);

    boolean existsByStageId(Long stageId);


}
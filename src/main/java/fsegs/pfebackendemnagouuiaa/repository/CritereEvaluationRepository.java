package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.CritereEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CritereEvaluationRepository extends JpaRepository<CritereEvaluation, Long> {

    List<CritereEvaluation> findByFicheId(Long ficheId);
}
package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.ReunionHebdomadaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReunionHebdomadaireRepository extends JpaRepository<ReunionHebdomadaire, Long> {

    List<ReunionHebdomadaire> findByStageId(Long stageId);

    List<ReunionHebdomadaire> findByCahierStageId(Long cahierStageId);

    boolean existsByStageIdAndDateAndHeure(Long stageId, LocalDate date, LocalTime heure);

    boolean existsByStageIdAndDateAndHeureAndIdNot(Long stageId, LocalDate date, LocalTime heure, Long id);
}

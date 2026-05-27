package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.PeriodeStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PeriodeStageRepository extends JpaRepository<PeriodeStage, Long> {

    /**
     * Renvoie toutes les périodes actives dont la fenêtre [dateDebut, dateFin] couvre
     * la date fournie.  Une liste non vide signifie que la date tombe dans au moins
     * une période de stage active.
     */
    @Query("SELECT p FROM PeriodeStage p " +
           "WHERE p.active = true " +
           "  AND :date >= p.dateDebut " +
           "  AND :date <= p.dateFin")
    List<PeriodeStage> findActivePeriodesCouvrant(@Param("date") LocalDate date);
}

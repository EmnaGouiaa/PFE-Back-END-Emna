package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReunionFinaleRepository extends JpaRepository<ReunionFinale, Long> {

    List<ReunionFinale> findByStageId(Long stageId);

    Optional<ReunionFinale> findFirstByStageIdOrderByIdAsc(Long stageId);

    Optional<ReunionFinale> findFirstByStageIdOrderByIdDesc(Long stageId);

    boolean existsByStageId(Long stageId);

    @Query("""
            select rf
            from ReunionFinale rf
            join rf.stage s
            where rf.date = :date
              and (
                (s.stagiaire is not null and s.stagiaire.id = :utilisateurId)
                or (s.encadrantAcademique is not null and s.encadrantAcademique.id = :utilisateurId)
                or (s.encadrantProfessionnel is not null and s.encadrantProfessionnel.id = :utilisateurId)
                or (s.tuteurEntreprise is not null and s.tuteurEntreprise.id = :utilisateurId)
              )
            """)
    List<ReunionFinale> findConcernedByUtilisateurIdAndDate(@Param("utilisateurId") Long utilisateurId,
                                                            @Param("date") LocalDate date);

    @Query("""
            select rf
            from ReunionFinale rf
            join rf.stage s
            where rf.date between :dateDebut and :dateFin
              and s.statut = fsegs.pfebackendemnagouuiaa.entities.StatutStage.TERMINE
              and (
                (s.stagiaire is not null and s.stagiaire.id = :utilisateurId)
                or (s.encadrantAcademique is not null and s.encadrantAcademique.id = :utilisateurId)
                or (s.encadrantProfessionnel is not null and s.encadrantProfessionnel.id = :utilisateurId)
                or (s.tuteurEntreprise is not null and s.tuteurEntreprise.id = :utilisateurId)
              )
            """)
    List<ReunionFinale> findAvailableForSatisfactionByUtilisateurId(@Param("utilisateurId") Long utilisateurId,
                                                                    @Param("dateDebut") LocalDate dateDebut,
                                                                    @Param("dateFin") LocalDate dateFin);

    @Query("""
            select rf
            from ReunionFinale rf
            join rf.stage s
            where rf.date <= :date
              and s.statut = fsegs.pfebackendemnagouuiaa.entities.StatutStage.TERMINE
              and (
                (s.stagiaire is not null and s.stagiaire.id = :utilisateurId)
                or (s.encadrantAcademique is not null and s.encadrantAcademique.id = :utilisateurId)
                or (s.encadrantProfessionnel is not null and s.encadrantProfessionnel.id = :utilisateurId)
                or (s.tuteurEntreprise is not null and s.tuteurEntreprise.id = :utilisateurId)
              )
            """)
    List<ReunionFinale> findPastForSatisfactionByUtilisateurId(@Param("utilisateurId") Long utilisateurId,
                                                               @Param("date") LocalDate date);
}

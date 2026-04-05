package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.TypeStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StageRepository extends JpaRepository<Stage, Long> {
    
    @Query("SELECT s FROM Stage s WHERE s.stagiaire.id = :stagiaireId")
    List<Stage> findByStagiaireId(@Param("stagiaireId") Long stagiaireId);
    
    @Query("SELECT s FROM Stage s WHERE s.entreprise.id = :entrepriseId")
    List<Stage> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
    
    @Query("SELECT s FROM Stage s WHERE s.encadrantAcademique.id = :encadrantId")
    List<Stage> findByEncadrantAcademiqueId(@Param("encadrantId") Long encadrantId);
    
    @Query("SELECT s FROM Stage s WHERE s.encadrantProfessionnel.id = :encadrantProId")
    List<Stage> findByEncadrantProfessionnelId(@Param("encadrantProId") Long encadrantProId);
    
    @Query("SELECT s FROM Stage s WHERE s.type = :type")
    List<Stage> findByType(@Param("type") TypeStage type);
    
    @Query("SELECT s FROM Stage s WHERE s.dateDebut BETWEEN :startDate AND :endDate")
    List<Stage> findByDateDebutBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT s FROM Stage s WHERE s.dureeSemaines = :duree")
    List<Stage> findByDureeSemaines(@Param("duree") Integer duree);
    
    @Query("SELECT COUNT(s) FROM Stage s WHERE s.type = :type")
    Long countByType(@Param("type") TypeStage type);
    
    @Query("SELECT s FROM Stage s WHERE LOWER(s.sujet) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Stage> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT s FROM Stage s WHERE s.stagiaire.id = :stagiaireId AND s.type IN :types")
    List<Stage> findByStagiaireIdAndTypeIn(@Param("stagiaireId") Long stagiaireId, @Param("types") List<TypeStage> types);
}

package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StageRepository extends JpaRepository<Stage, Long> {

    List<Stage> findByStagiaireId(Long stagiaireId);

    /** Stages dont le stagiaire a été supprimé (stagiaire_id IS NULL) — orphelins à nettoyer. */
    List<Stage> findByStagiaireIsNull();

    List<Stage> findByDuree(Integer duree);

    List<Stage> findByNbSemaine(Integer nbSemaine);

    List<Stage> findByNiveauSouhaite(String niveauSouhaite);

    List<Stage> findByTitre(String titre);

    List<Stage> findByStagiaireIdAndTitreIn(Long stagiaireId, List<String> titres);

    List<Stage> findByEntrepriseId(Long entrepriseId);

    List<Stage> findByEncadrantAcademiqueId(Long encadrantId);

    List<Stage> findByEncadrantProfessionnelId(Long encadrantId);

    List<Stage> findByTuteurEntrepriseId(Long tuteurId);

    List<Stage> findByOffreSourceId(Long offreSourceId);

    Optional<Stage> findFirstByStagiaireIdAndOffreSourceId(Long stagiaireId, Long offreSourceId);

    Optional<Stage> findFirstByOffreSourceIdOrderByIdDesc(Long offreSourceId);

    boolean existsByOffreSourceId(Long offreSourceId);

    boolean existsByStagiaireIdAndStatutIn(Long stagiaireId, List<StatutStage> statuts);

    List<Stage> findByDateFinLessThanEqualAndStatutIn(LocalDate dateFin, List<StatutStage> statuts);

    /** Retourne tous les stages dont le statut est dans la liste fournie. */
    List<Stage> findByStatutIn(List<StatutStage> statuts);

    /** Vérifie si un encadrant académique a déjà validé le sujet d'au moins un stage. */
    boolean existsBySujetValideParId(Long encadrantId);
}

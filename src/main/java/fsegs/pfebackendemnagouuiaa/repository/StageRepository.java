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

    /**
     * Les deux utilisateurs interviennent sur le même stage (stagiaire, encadrants, tuteur entreprise).
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Stage s
            WHERE (
                (s.stagiaire IS NOT NULL AND s.stagiaire.id = :viewerId)
                OR (s.encadrantAcademique IS NOT NULL AND s.encadrantAcademique.id = :viewerId)
                OR (s.encadrantProfessionnel IS NOT NULL AND s.encadrantProfessionnel.id = :viewerId)
                OR (s.tuteurEntreprise IS NOT NULL AND s.tuteurEntreprise.id = :viewerId)
            )
            AND (
                (s.stagiaire IS NOT NULL AND s.stagiaire.id = :targetId)
                OR (s.encadrantAcademique IS NOT NULL AND s.encadrantAcademique.id = :targetId)
                OR (s.encadrantProfessionnel IS NOT NULL AND s.encadrantProfessionnel.id = :targetId)
                OR (s.tuteurEntreprise IS NOT NULL AND s.tuteurEntreprise.id = :targetId)
            )
            """)
    boolean areCoParticipantsOnSameStage(Long viewerId, Long targetId);

    /**
     * Utilisateur présent comme acteur du stage dans une entreprise donnée (ex. stagiaire, EP sous ce contrat).
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Stage s
            WHERE s.entreprise.id = :entrepriseId
            AND (
                (s.stagiaire IS NOT NULL AND s.stagiaire.id = :userId)
                OR (s.encadrantAcademique IS NOT NULL AND s.encadrantAcademique.id = :userId)
                OR (s.encadrantProfessionnel IS NOT NULL AND s.encadrantProfessionnel.id = :userId)
                OR (s.tuteurEntreprise IS NOT NULL AND s.tuteurEntreprise.id = :userId)
            )
            """)
    boolean existsParticipantOnCompanyStage(Long entrepriseId, Long userId);
}

package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateStageRequest;
import fsegs.pfebackendemnagouuiaa.entities.OffreStage;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;

import java.util.List;
import java.util.Map;

public interface StageService {
    Stage createStage(CreateStageRequest request);
    Stage updateStage(Long id, CreateStageRequest request);
    Stage getStageById(Long id);
    List<Stage> getAllStages();
    void deleteStage(Long id);

    Stage affecterEncadrantAcademique(Long stageId, Long encadrantAcademiqueId);
    Stage affecterEncadrantProfessionnel(Long stageId, Long encadrantProfessionnelId);
    Stage validerStageParEntreprise(Long stageId);
    Stage validerStageParResponsable(Long stageId);

    List<Stage> getStagesByStagiaire(Long stagiaireId);
    List<Stage> getStagesByEntreprise(Long entrepriseId);
    List<Stage> getStagesByEncadrantAcademique(Long encadrantId);
    List<Stage> getStagesByEncadrantProfessionnel(Long encadrantId);
    List<Stage> getStagesPourEncadrantAcademiqueAuthentifie();
    List<Stage> getStagesPourEncadrantProfessionnelAuthentifie();
    List<Stage> getStagesPourStagiaireAuthentifie();
    Stage getStageCourantPourStagiaireAuthentifie();
    List<Stage> getStagesPourResponsableEntrepriseAuthentifie();

    Stage creerStageDepuisOffre(Long offreId, Long stagiaireId);
    Stage creerStageDepuisOffrePourEntreprise(OffreStage offre, Stagiaire stagiaire, ResponsableEntreprise responsableEntreprise);
    Stage validerSujetParEncadrantAcademique(Long stageId, Long encadrantId);
    Stage validerSujetParEncadrantAcademiqueAuthentifie(Long stageId);
    Stage refuserSujetParEncadrantAcademique(Long stageId, Long encadrantId);
    Stage refuserSujetParEncadrantAcademiqueAuthentifie(Long stageId);
    Map<String, Object> genererRapportStage(Long stageId);
    Map<String, Object> getResumeTrelloStage(Long stageId);
    Map<String, Object> createTrelloBoardIfNotExists(Long stageId);

    /**
     * Vérifie tous les stages en statut PAS_COMMENCE et déclenche (EN_COURS)
     * ceux qui remplissent toutes les conditions métier. Retourne le nombre
     * de stages effectivement déclenchés.
     */
    int declencherStagesEligibles();

}

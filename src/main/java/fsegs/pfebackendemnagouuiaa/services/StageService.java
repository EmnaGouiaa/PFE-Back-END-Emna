package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateStageRequest;
import fsegs.pfebackendemnagouuiaa.dto.RapportEnqueteSatisfactionResponse;
import fsegs.pfebackendemnagouuiaa.dto.StageEnqueteSectionStatusResponse;
import fsegs.pfebackendemnagouuiaa.entities.OffreStage;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

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

    Stage creerStageDepuisOffre(Long offreId, Long stagiaireId);
    Stage creerStageDepuisOffrePourEntreprise(OffreStage offre, Stagiaire stagiaire, ResponsableEntreprise responsableEntreprise);
    Stage validerSujetParEncadrantAcademique(Long stageId, Long encadrantId);
    Stage validerSujetParEncadrantAcademiqueAuthentifie(Long stageId);
    Stage refuserSujetParEncadrantAcademique(Long stageId, Long encadrantId);
    Stage refuserSujetParEncadrantAcademiqueAuthentifie(Long stageId);
    Map<String, Object> genererRapportStage(Long stageId);
    Map<String, Object> getResumeTrelloStage(Long stageId);
    Map<String, Object> createTrelloBoardIfNotExists(Long stageId);
    StageEnqueteSectionStatusResponse getEnqueteSectionStatus(Long stageId);
    RapportEnqueteSatisfactionResponse uploadRapportEnquete(Long stageId, MultipartFile file);
    Resource getRapportEnqueteResource(Long stageId);
    RapportEnqueteSatisfactionResponse getRapportEnqueteMetadata(Long stageId);

}

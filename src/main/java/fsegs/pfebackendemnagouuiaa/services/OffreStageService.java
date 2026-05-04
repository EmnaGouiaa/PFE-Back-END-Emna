package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateOffreStageRequest;
import fsegs.pfebackendemnagouuiaa.dto.AffecterEtudiantOffreResponse;
import fsegs.pfebackendemnagouuiaa.dto.AnnulerAffectationOffreResponse;
import fsegs.pfebackendemnagouuiaa.dto.OffreStageResponse;

import java.util.List;

public interface OffreStageService {

    OffreStageResponse createOffre(CreateOffreStageRequest request);

    OffreStageResponse updateOffre(Long id, CreateOffreStageRequest request);

    OffreStageResponse getOffreById(Long id);

    List<OffreStageResponse> getAllOffres();

    List<OffreStageResponse> getToutesOffresPourGestion();

    void deleteOffre(Long id);

    OffreStageResponse publierOffre(Long offreId, Long responsableEntrepriseId);

    OffreStageResponse validerOffre(Long offreId, Long responsableServiceStagesId);

    OffreStageResponse refuserOffre(Long offreId, String motifRefus);

    OffreStageResponse fermerOffre(Long offreId);

    List<OffreStageResponse> getOffresByEntreprise(Long entrepriseId);

    List<OffreStageResponse> getOffresOuvertes();

    List<OffreStageResponse> getOffresEnAttenteValidation();

    AffecterEtudiantOffreResponse affecterEtudiant(Long offreId, String emailEtudiant);

    AnnulerAffectationOffreResponse annulerAffectation(Long offreId);
}

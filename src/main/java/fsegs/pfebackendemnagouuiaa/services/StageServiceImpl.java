package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ConventionStageDto;
import fsegs.pfebackendemnagouuiaa.dto.CreateStageRequest;
import fsegs.pfebackendemnagouuiaa.dto.RapportEnqueteSatisfactionResponse;
import fsegs.pfebackendemnagouuiaa.dto.StageEnqueteSectionStatusResponse;
import fsegs.pfebackendemnagouuiaa.entities.*;
import fsegs.pfebackendemnagouuiaa.mapper.StageMapper;
import fsegs.pfebackendemnagouuiaa.repository.ConventionStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantAcademiqueRepository;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantProfessionnelRepository;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.OffreStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.RapportEnqueteSatisfactionRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.StagiaireRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class StageServiceImpl implements StageService {

    private static final LocalTime HEURE_REUNION_FINALE_PAR_DEFAUT = LocalTime.of(9, 0);
    private static final String TYPE_NOTIFICATION_OUVERTURE_ESPACES = "OUVERTURE_ESPACES_FIN_STAGE";

    private final StageRepository stageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final EncadrantAcademiqueRepository encadrantAcademiqueRepository;
    private final EncadrantProfessionnelRepository encadrantProfessionnelRepository;
    private final FicheEvaluationRepository ficheEvaluationRepository;
    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final OffreStageRepository offreStageRepository;
    private final ReunionFinaleRepository reunionFinaleRepository;
    private final RapportEnqueteSatisfactionRepository rapportEnqueteSatisfactionRepository;
    private final TrelloService trelloService;
    private final ConventionStageService conventionStageService;
    private final ConventionStageRepository conventionStageRepository;
    private final StagiaireResolutionService stagiaireResolutionService;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final EnqueteSatisfactionService enqueteSatisfactionService;
    private final JwtService jwtService;

    @Value("${app.upload.rapport-enquete-dir:C:/tmp/rapport-enquete-satisfaction}")
    private String rapportEnqueteUploadDir;

    @Scheduled(cron = "${app.stage.finalization.cron:0 0 */1 * * *}", zone = "${app.stage.finalization.zone:Africa/Tunis}")
    @Transactional
    public void synchroniserFinStagesPlanifies() {
        LocalDate today = LocalDate.now();
        List<Stage> stagesDus = stageRepository.findByDateFinLessThanEqualAndStatutIn(
                today,
                List.of(StatutStage.PAS_COMMENCE, StatutStage.EN_COURS, StatutStage.TERMINE)
        );

        for (Stage stage : stagesDus) {
            try {
                Stage savedStage = synchroniserAutomatisationReunionFinaleEtOuverture(stage);
                if (savedStage.getId() != null) {
                    log.debug("Synchronisation de fin de stage executee pour le stage {}", savedStage.getId());
                }
            } catch (RuntimeException ex) {
                Long stageId = stage != null ? stage.getId() : null;
                log.error("Echec de la synchronisation automatique de fin de stage. stageId={}", stageId, ex);
            }
        }
    }

    @Override
    public Stage createStage(CreateStageRequest request) {
        Stage stage = StageMapper.toEntity(request);
        stage.setStatut(StatutStage.PAS_COMMENCE);
        stage.setStatutSujet(StatutValidation.EN_ATTENTE);
        initialiserStatutsSections(stage);

        remplirRelations(stage, request);
        appliquerStatutMetier(stage);

        Stage persistedStage = enregistrerStageAvecDeclenchement(stage, null);
        notifierEncadrantsAffectes(persistedStage, null, null);
        return enrichStageSurveyStatus(persistedStage);
    }

    @Override
    public Stage updateStage(Long id, CreateStageRequest request) {
        Stage stage = stageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        StatutStage previousStatus = stage.getStatut();
        Long previousEncadrantAcademiqueId = stage.getEncadrantAcademique() != null ? stage.getEncadrantAcademique().getId() : null;
        Long previousEncadrantProfessionnelId = stage.getEncadrantProfessionnel() != null ? stage.getEncadrantProfessionnel().getId() : null;

        stage.setTitre(request.getTitre());
        stage.setDateDebut(request.getDateDebut());
        stage.setDateFin(request.getDateFin());
        stage.setDuree(request.getDuree());
        stage.setNbSemaine(request.getNbSemaine());
        stage.setNiveauSouhaite(request.getNiveauSouhaite());
        stage.setSujet(request.getSujet());
        remplirRelations(stage, request);
        appliquerStatutMetier(stage);

        Stage updatedStage = enregistrerStageAvecDeclenchement(stage, previousStatus);
        notifierEncadrantsAffectes(updatedStage, previousEncadrantAcademiqueId, previousEncadrantProfessionnelId);
        return enrichStageSurveyStatus(updatedStage);
    }

    @Override
    public Stage getStageById(Long id) {
        return stageRepository.findById(id)
                .map(this::enrichStageSurveyStatus)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
    }

    @Override
    public List<Stage> getAllStages() {
        return stageRepository.findAll().stream().map(this::enrichStageSurveyStatus).toList();
    }

    @Override
    @Transactional
    public void deleteStage(Long id) {
        Stage stage = stageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        detachReunionParticipants(stage);

        stageRepository.delete(stage);
    }

    @Override
    public Stage affecterEncadrantAcademique(Long stageId, Long encadrantAcademiqueId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        StatutStage previousStatus = stage.getStatut();
        Long previousEncadrantAcademiqueId = stage.getEncadrantAcademique() != null ? stage.getEncadrantAcademique().getId() : null;

        EncadrantAcademique encadrant = encadrantAcademiqueRepository.findById(encadrantAcademiqueId)
                .orElseThrow(() -> new EntityNotFoundException("EncadrantAcademique introuvable"));

        synchroniserEncadrantAcademiqueStagiaire(stage.getStagiaire(), encadrant);
        synchroniserStagesActifsDuStagiaire(stage.getStagiaire(), encadrant);
        stage.setEncadrantAcademique(encadrant);
        appliquerStatutMetier(stage);
        Stage savedStage = enregistrerStageAvecDeclenchement(stage, previousStatus);
        notifierEncadrantAffecte(
                savedStage,
                previousEncadrantAcademiqueId,
                savedStage.getEncadrantAcademique() != null ? savedStage.getEncadrantAcademique().getId() : null,
                "academique"
        );
        return enrichStageSurveyStatus(savedStage);
    }

    @Override
    public Stage affecterEncadrantProfessionnel(Long stageId, Long encadrantProfessionnelId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        StatutStage previousStatus = stage.getStatut();
        Long previousEncadrantProfessionnelId = stage.getEncadrantProfessionnel() != null ? stage.getEncadrantProfessionnel().getId() : null;

        EncadrantProfessionnel encadrant = encadrantProfessionnelRepository.findById(encadrantProfessionnelId)
                .orElseThrow(() -> new EntityNotFoundException("EncadrantProfessionnel introuvable"));

        stage.setEncadrantProfessionnel(encadrant);
        appliquerStatutMetier(stage);
        Stage savedStage = enregistrerStageAvecDeclenchement(stage, previousStatus);
        notifierEncadrantAffecte(
                savedStage,
                previousEncadrantProfessionnelId,
                savedStage.getEncadrantProfessionnel() != null ? savedStage.getEncadrantProfessionnel().getId() : null,
                "professionnel"
        );
        return enrichStageSurveyStatus(savedStage);
    }

    @Override
    public Stage validerStageParEntreprise(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        StatutStage previousStatus = stage.getStatut();

        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.RESPONSABLE_ENTREPRISE) {
            throw new AccessDeniedException("Acces refuse : role responsable entreprise requis.");
        }

        ensureEntrepriseScope(stage, utilisateur);

        appliquerStatutMetier(stage);
        Stage saved = enregistrerStageAvecDeclenchement(stage, previousStatus);
        notifierValidationEntreprise(saved);
        return enrichStageSurveyStatus(saved);
    }

    @Override
    public Stage validerStageParResponsable(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        StatutStage previousStatus = stage.getStatut();

        appliquerStatutMetier(stage);
        Stage saved = enregistrerStageAvecDeclenchement(stage, previousStatus);
        return enrichStageSurveyStatus(saved);
    }

    @Override
    public List<Stage> getStagesByStagiaire(Long stagiaireId) {
        return stageRepository.findByStagiaireId(stagiaireId).stream().map(this::enrichStageSurveyStatus).toList();
    }

    @Override
    public List<Stage> getStagesByEntreprise(Long entrepriseId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() == Role.RESPONSABLE_ENTREPRISE) {
            ensureEntrepriseScope(entrepriseId, utilisateur);
        }
        return stageRepository.findByEntrepriseId(entrepriseId).stream().map(this::enrichStageSurveyStatus).toList();
    }

    @Override
    public List<Stage> getStagesByEncadrantAcademique(Long encadrantId) {
        return stageRepository.findByEncadrantAcademiqueId(encadrantId).stream().map(this::enrichStageSurveyStatus).toList();
    }

    @Override
    public List<Stage> getStagesByEncadrantProfessionnel(Long encadrantId) {
        return stageRepository.findByEncadrantProfessionnelId(encadrantId).stream().map(this::enrichStageSurveyStatus).toList();
    }

    @Override
    public List<Stage> getStagesPourEncadrantAcademiqueAuthentifie() {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.ENCADRANT_ACADEMIQUE) {
            throw new RuntimeException("Acces refuse : role encadrant academique requis.");
        }
        return stageRepository.findByEncadrantAcademiqueId(utilisateur.getId()).stream().map(this::enrichStageSurveyStatus).toList();
    }

    @Override
    public List<Stage> getStagesPourEncadrantProfessionnelAuthentifie() {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.ENCADRANT_PROFESSIONNEL) {
            throw new RuntimeException("Acces refuse : role encadrant professionnel requis.");
        }
        return stageRepository.findByEncadrantProfessionnelId(utilisateur.getId()).stream().map(this::enrichStageSurveyStatus).toList();
    }

    @Override
    public List<Stage> getStagesPourStagiaireAuthentifie() {
        Stagiaire stagiaire = getAuthenticatedStagiaire();
        return stageRepository.findByStagiaireId(stagiaire.getId())
                .stream()
                .filter(stage -> stage.getStatut() != StatutStage.REFUSE)
                .map(this::enrichStageSurveyStatus)
                .toList();
    }

    @Override
    public Stage getStageCourantPourStagiaireAuthentifie() {
        List<Stage> stages = getStagesPourStagiaireAuthentifie();

        return stages.stream()
                .filter(stage -> stage.getStatut() == StatutStage.EN_COURS)
                .findFirst()
                .or(() -> stages.stream().filter(stage -> stage.getStatut() == StatutStage.PAS_COMMENCE).findFirst())
                .or(() -> stages.stream().filter(stage -> stage.getStatut() == StatutStage.TERMINE).findFirst())
                .map(this::enrichStageSurveyStatus)
                .orElseThrow(() -> new EntityNotFoundException("Aucun stage ne vous a encore ete affecte."));
    }

    @Override
    public Stage creerStageDepuisOffre(Long offreId, Long stagiaireId) {
        OffreStage offre = offreStageRepository.findById(offreId)
                .orElseThrow(() -> new EntityNotFoundException("Offre introuvable"));

        Stagiaire stagiaire = stagiaireRepository.findById(stagiaireId)
                .orElseThrow(() -> new EntityNotFoundException("Stagiaire introuvable"));

        Stage savedStage = creerStageDepuisOffrePourEntreprise(offre, stagiaire, offre.getPublieePar());

        OffreStage offreAssociee = savedStage.getOffreSource();
        if (offreAssociee != null) {
            offreAssociee.setStatut(StatutOffre.FERMEE);
            offreStageRepository.save(offreAssociee);
        }

        return stageRepository.save(savedStage);
    }

    @Override
    public Stage creerStageDepuisOffrePourEntreprise(OffreStage offre, Stagiaire stagiaire, ResponsableEntreprise responsableEntreprise) {
        if (offre == null) {
            throw new EntityNotFoundException("Offre introuvable");
        }

        if (stagiaire == null) {
            throw new EntityNotFoundException("Stagiaire introuvable");
        }

        if (stageRepository.existsByOffreSourceId(offre.getId())) {
            throw new IllegalStateException("Un stage existe deja pour cette offre.");
        }

        Stage stage = new Stage();
        stage.setTitre(offre.getTitre());
        stage.setSujet(offre.getDescriptionMissions());
        stage.setDuree(offre.getDuree());
        stage.setNbSemaine(offre.getDuree() != null ? offre.getDuree() * 4 : null);
        stage.setDateDebut(offre.getDateDebutPrevue());
        stage.setDateFin(calculerDateFin(offre.getDateDebutPrevue(), offre.getDuree()));
        stage.setNiveauSouhaite(offre.getProfilRecherche());
        stage.setEntreprise(offre.getEntreprise());
        stage.setStagiaire(stagiaire);
        stage.setEncadrantAcademique(stagiaire.getEncadrantAcademique());
        stage.setOffreSource(offre);
        stage.setTuteurEntreprise(responsableEntreprise);
        stage.setStatut(StatutStage.PAS_COMMENCE);
        stage.setStatutSujet(StatutValidation.EN_ATTENTE);
        initialiserStatutsSections(stage);
        appliquerStatutMetier(stage);

        Stage persistedStage = enregistrerStageAvecDeclenchement(stage, null);
        notifierEncadrantsAffectes(persistedStage, null, null);
        return enrichStageSurveyStatus(persistedStage);
    }

    private void remplirRelations(Stage stage, CreateStageRequest request) {
        Stagiaire stagiaire = stagiaireRepository.findById(request.getStagiaireId())
                .orElseThrow(() -> new EntityNotFoundException("Stagiaire introuvable"));
        stage.setStagiaire(stagiaire);

        Entreprise entreprise = entrepriseRepository.findById(request.getEntrepriseId())
                .orElseThrow(() -> new EntityNotFoundException("Entreprise introuvable"));
        stage.setEntreprise(entreprise);

        EncadrantAcademique encadrantAcademique = resoudreEncadrantAcademique(request.getEncadrantAcademiqueId(), stagiaire);
        stage.setEncadrantAcademique(encadrantAcademique);
        synchroniserEncadrantAcademiqueStagiaire(stagiaire, encadrantAcademique);

        if (request.getEncadrantProfessionnelId() != null) {
            EncadrantProfessionnel enc = encadrantProfessionnelRepository.findById(request.getEncadrantProfessionnelId())
                    .orElseThrow(() -> new EntityNotFoundException("EncadrantProfessionnel introuvable"));
            stage.setEncadrantProfessionnel(enc);
        } else {
            stage.setEncadrantProfessionnel(null);
        }

        if (request.getTuteurEntrepriseId() != null) {
            ResponsableEntreprise tuteur = responsableEntrepriseRepository.findById(request.getTuteurEntrepriseId())
                    .orElseThrow(() -> new EntityNotFoundException("TuteurEntreprise introuvable"));
            stage.setTuteurEntreprise(tuteur);
        } else {
            stage.setTuteurEntreprise(null);
        }

        if (request.getOffreSourceId() != null) {
            OffreStage offre = offreStageRepository.findById(request.getOffreSourceId())
                    .orElseThrow(() -> new EntityNotFoundException("Offre introuvable"));
            stage.setOffreSource(offre);
        } else {
            stage.setOffreSource(null);
        }
    }

    private void initialiserTrelloPourStage(Stage stage) {
        System.out.println("Création board Trello pour stage : " + stage.getTitre());

        Map<String, Object> board = trelloService.createBoard("Stage - " + stage.getTitre());

        stage.setTrelloBoardId((String) board.get("id"));
        stage.setTrelloBoardUrl((String) board.get("url"));

        Map<String, Object> todo = trelloService.createList(stage.getTrelloBoardId(), "To Do");
        Map<String, Object> doing = trelloService.createList(stage.getTrelloBoardId(), "In Progress");
        Map<String, Object> done = trelloService.createList(stage.getTrelloBoardId(), "Done");

        stage.setTrelloTodoListId((String) todo.get("id"));
        stage.setTrelloDoingListId((String) doing.get("id"));
        stage.setTrelloDoneListId((String) done.get("id"));
    }

    private LocalDate calculerDateFin(LocalDate dateDebut, Integer dureeMois) {
        if (dateDebut == null || dureeMois == null) {
            return null;
        }

        return dateDebut.plusMonths(dureeMois.longValue());
    }

    private Stagiaire getAuthenticatedStagiaire() {
        String email = getAuthenticatedUtilisateur().getEmail();

        return stagiaireResolutionService.findByEmail(email);
    }

    private Utilisateur getAuthenticatedUtilisateur() {
        return jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur authentifie introuvable."));
    }

    private void ensureEntrepriseScope(Stage stage, Utilisateur utilisateur) {
        Long stageEntrepriseId = stage.getEntreprise() != null ? stage.getEntreprise().getId() : null;
        ensureEntrepriseScope(stageEntrepriseId, utilisateur);
    }

    private void ensureEntrepriseScope(Long entrepriseId, Utilisateur utilisateur) {
        if (!(utilisateur instanceof ResponsableEntreprise responsableEntreprise)) {
            throw new AccessDeniedException("Acces refuse : compte responsable entreprise requis.");
        }

        Long expectedEntrepriseId = responsableEntreprise.getEntreprise() != null
                ? responsableEntreprise.getEntreprise().getId()
                : null;

        if (expectedEntrepriseId == null || entrepriseId == null || !expectedEntrepriseId.equals(entrepriseId)) {
            throw new AccessDeniedException("Acces refuse a un stage qui n'appartient pas a votre entreprise.");
        }
    }

    private void notifierValidationEntreprise(Stage stage) {
        if (stage.getStagiaire() != null && stage.getStagiaire().getId() != null) {
            notificationService.creerNotification(
                    stage.getStagiaire().getId(),
                    "Stage validé",
                    "Votre stage " + (stage.getTitre() == null ? "sélectionné" : stage.getTitre()) + " a été validé par votre entreprise.",
                    "VALIDATION_ENTREPRISE",
                    stage.getId(),
                    "STAGE"
            );
        }

        if (stage.getEncadrantAcademique() != null && stage.getEncadrantAcademique().getId() != null) {
            notificationService.creerNotification(
                    stage.getEncadrantAcademique().getId(),
                    "Stage validé côté entreprise",
                    "Le stage " + (stage.getTitre() == null ? "sélectionné" : stage.getTitre()) + " a été validé par l'entreprise.",
                    "VALIDATION_ENTREPRISE",
                    stage.getId(),
                    "STAGE"
            );
        }
    }

    private EncadrantAcademique resoudreEncadrantAcademique(Long encadrantAcademiqueId, Stagiaire stagiaire) {
        if (encadrantAcademiqueId != null) {
            return encadrantAcademiqueRepository.findById(encadrantAcademiqueId)
                    .orElseThrow(() -> new EntityNotFoundException("EncadrantAcademique introuvable"));
        }

        return stagiaire != null ? stagiaire.getEncadrantAcademique() : null;
    }

    private void synchroniserEncadrantAcademiqueStagiaire(Stagiaire stagiaire, EncadrantAcademique encadrant) {
        if (stagiaire == null || stagiaire.getId() == null || encadrant == null) {
            return;
        }

        Long currentId = stagiaire.getEncadrantAcademique() != null ? stagiaire.getEncadrantAcademique().getId() : null;
        if (encadrant.getId().equals(currentId)) {
            return;
        }

        stagiaire.setEncadrantAcademique(encadrant);
        stagiaireRepository.save(stagiaire);
    }

    private void synchroniserStagesActifsDuStagiaire(Stagiaire stagiaire, EncadrantAcademique encadrant) {
        if (stagiaire == null || stagiaire.getId() == null || encadrant == null) {
            return;
        }

        List<Stage> stagesActifs = stageRepository.findByStagiaireId(stagiaire.getId())
                .stream()
                .filter(this::isStageActifPourEncadrant)
                .toList();

        boolean updated = false;
        for (Stage current : stagesActifs) {
            Long currentId = current.getEncadrantAcademique() != null ? current.getEncadrantAcademique().getId() : null;
            if (!encadrant.getId().equals(currentId)) {
                current.setEncadrantAcademique(encadrant);
                updated = true;
            }
        }

        if (updated) {
            stageRepository.saveAll(stagesActifs);
        }
    }

    private boolean isStageActifPourEncadrant(Stage stage) {
        return stage != null
                && stage.getStatut() != null
                && stage.getStatut() != StatutStage.REFUSE
                && stage.getStatut() != StatutStage.ANNULE
                && stage.getStatut() != StatutStage.TERMINE;
    }

    private void detachReunionParticipants(Stage stage) {
        if (stage == null || stage.getReunions() == null) {
            return;
        }

        stage.getReunions().forEach(reunion -> {
            if (reunion.getParticipants() != null) {
                reunion.getParticipants().clear();
            }
        });
    }

    private void notifierEncadrantsAffectes(Stage stage,
                                            Long previousEncadrantAcademiqueId,
                                            Long previousEncadrantProfessionnelId) {
        Long currentEncadrantAcademiqueId = stage.getEncadrantAcademique() != null ? stage.getEncadrantAcademique().getId() : null;
        Long currentEncadrantProfessionnelId = stage.getEncadrantProfessionnel() != null ? stage.getEncadrantProfessionnel().getId() : null;

        notifierEncadrantAffecte(stage, previousEncadrantAcademiqueId, currentEncadrantAcademiqueId, "academique");
        notifierEncadrantAffecte(stage, previousEncadrantProfessionnelId, currentEncadrantProfessionnelId, "professionnel");
    }

    private void notifierEncadrantAffecte(Stage stage,
                                          Long previousEncadrantId,
                                          Long currentEncadrantId,
                                          String typeEncadrant) {
        if (currentEncadrantId == null || currentEncadrantId.equals(previousEncadrantId)) {
            return;
        }

        notificationService.creerNotification(
                currentEncadrantId,
                "Affectation encadrant",
                "Vous avez ete affecte comme encadrant " + typeEncadrant + " pour un stage.",
                "AFFECTATION_ENCADRANT_STAGE",
                stage.getId(),
                "STAGE"
        );
    }

    @Override
    public Stage validerSujetParEncadrantAcademique(Long stageId, Long encadrantId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        StatutStage previousStatus = stage.getStatut();

        EncadrantAcademique encadrant = encadrantAcademiqueRepository.findById(encadrantId)
                .orElseThrow(() -> new EntityNotFoundException("EncadrantAcademique introuvable"));

        if (stage.getEncadrantAcademique() == null || !stage.getEncadrantAcademique().getId().equals(encadrantId)) {
            throw new RuntimeException("Cet encadrant académique n'est pas affecté à ce stage");
        }

        stage.setSujetValidePar(encadrant);
        stage.setStatutSujet(StatutValidation.VALIDEE);
        appliquerStatutMetier(stage);

        Stage saved = enregistrerStageAvecDeclenchement(stage, previousStatus);
        return enrichStageSurveyStatus(saved);
    }

    @Override
    public Stage validerSujetParEncadrantAcademiqueAuthentifie(Long stageId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.ENCADRANT_ACADEMIQUE) {
            throw new RuntimeException("Acces refuse : seul l'encadrant academique affecte peut valider le sujet.");
        }
        return validerSujetParEncadrantAcademique(stageId, utilisateur.getId());
    }

    private void genererConventionSiAbsente(Stage stage) {
        if (stage == null || stage.getId() == null) return;
        if (conventionStageRepository.existsByStageId(stage.getId())) return;

        conventionStageService.createByStage(stage.getId(), new ConventionStageDto());
    }

    @Override
    public Stage refuserSujetParEncadrantAcademique(Long stageId, Long encadrantId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        EncadrantAcademique encadrant = encadrantAcademiqueRepository.findById(encadrantId)
                .orElseThrow(() -> new EntityNotFoundException("EncadrantAcademique introuvable"));

        if (stage.getEncadrantAcademique() == null || !stage.getEncadrantAcademique().getId().equals(encadrantId)) {
            throw new RuntimeException("Cet encadrant académique n'est pas affecté à ce stage");
        }

        stage.setSujetValidePar(encadrant);
        stage.setStatutSujet(StatutValidation.REFUSEE);
        stage.setStatut(StatutStage.REFUSE);

        return enrichStageSurveyStatus(stageRepository.save(stage));
    }

    @Override
    public Stage refuserSujetParEncadrantAcademiqueAuthentifie(Long stageId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.ENCADRANT_ACADEMIQUE) {
            throw new RuntimeException("Acces refuse : seul l'encadrant academique affecte peut refuser le sujet.");
        }
        return refuserSujetParEncadrantAcademique(stageId, utilisateur.getId());
    }

    @Override
    public Map<String, Object> createTrelloBoardIfNotExists(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        authorizeLinkedStageAccess(stage);
        ensureStageEnCoursForTrello(stage);

        if (hasText(stage.getTrelloBoardId())) {
            if (!hasText(stage.getTrelloBoardUrl())) {
                stage.setTrelloBoardUrl("https://trello.com/b/" + stage.getTrelloBoardId());
                stage = stageRepository.save(stage);
            }
            return buildTrelloBoardResponse(stage, false);
        }

        try {
            String boardName = buildTrelloBoardName(stage);
            Map<String, Object> board = trelloService.createBoard(boardName);
            if (board == null || board.get("id") == null) {
                throw new IllegalStateException("Réponse Trello invalide lors de la création du board.");
            }

            stage.setTrelloBoardId(String.valueOf(board.get("id")));
            stage.setTrelloBoardUrl(String.valueOf(board.getOrDefault("url", "")));

            Map<String, Object> todo = trelloService.createList(stage.getTrelloBoardId(), "To Do");
            Map<String, Object> doing = trelloService.createList(stage.getTrelloBoardId(), "In Progress");
            Map<String, Object> done = trelloService.createList(stage.getTrelloBoardId(), "Done");

            stage.setTrelloTodoListId(readTrelloId(todo));
            stage.setTrelloDoingListId(readTrelloId(doing));
            stage.setTrelloDoneListId(readTrelloId(done));

            Stage saved = stageRepository.save(stage);
            return buildTrelloBoardResponse(saved, true);
        } catch (RuntimeException ex) {
            throw new RuntimeException("Erreur lors de la création du board Trello", ex);
        }
    }

    @Override
    public Map<String, Object> getResumeTrelloStage(Long stageId) {
        createTrelloBoardIfNotExists(stageId);
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        List<Map<String, Object>> listes = trelloService.getListsByBoard(stage.getTrelloBoardId());
        List<Map<String, Object>> cartes = trelloService.getCardsByBoard(stage.getTrelloBoardId());

        String doneListId = stage.getTrelloDoneListId();
        String doingListId = stage.getTrelloDoingListId();
        String todoListId = stage.getTrelloTodoListId();

        List<Map<String, Object>> tachesTerminees = cartes.stream()
                .filter(c -> doneListId != null && doneListId.equals(c.get("idList")))
                .toList();

        List<Map<String, Object>> tachesEnCours = cartes.stream()
                .filter(c -> doingListId != null && doingListId.equals(c.get("idList")))
                .toList();

        List<Map<String, Object>> tachesAFaire = cartes.stream()
                .filter(c -> todoListId != null && todoListId.equals(c.get("idList")))
                .toList();

        Map<String, Object> resume = new LinkedHashMap<>();
        resume.put("stageId", stage.getId());
        resume.put("titreStage", stage.getTitre());
        resume.put("sujet", stage.getSujet());
        resume.put("trelloBoardId", stage.getTrelloBoardId());
        resume.put("trelloBoardUrl", stage.getTrelloBoardUrl());

        resume.put("nombreTotalTaches", cartes.size());
        resume.put("nombreTachesAFaire", tachesAFaire.size());
        resume.put("nombreTachesEnCours", tachesEnCours.size());
        resume.put("nombreTachesTerminees", tachesTerminees.size());

        resume.put("listes", listes);
        resume.put("tachesAFaire", tachesAFaire);
        resume.put("tachesEnCours", tachesEnCours);
        resume.put("tachesTerminees", tachesTerminees);

        return resume;
    }

    private Map<String, Object> buildTrelloBoardResponse(Stage stage, boolean created) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stageId", stage.getId());
        response.put("trelloBoardId", stage.getTrelloBoardId());
        response.put("trelloBoardUrl", stage.getTrelloBoardUrl());
        response.put("trelloTodoListId", stage.getTrelloTodoListId());
        response.put("trelloDoingListId", stage.getTrelloDoingListId());
        response.put("trelloDoneListId", stage.getTrelloDoneListId());
        response.put("created", created);
        return response;
    }

    private String buildTrelloBoardName(Stage stage) {
        String studentName = stage.getStagiaire() == null
                ? "Stagiaire"
                : buildFullName(stage.getStagiaire().getPrenom(), stage.getStagiaire().getNom());
        String companyName = stage.getEntreprise() == null || !hasText(stage.getEntreprise().getNom())
                ? "Entreprise"
                : stage.getEntreprise().getNom();
        return "Stage - " + studentName + " - " + companyName;
    }

    private String buildFullName(String prenom, String nom) {
        String fullName = ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
        return hasText(fullName) ? fullName : "Stagiaire";
    }

    private String readTrelloId(Map<String, Object> value) {
        return value == null || value.get("id") == null ? null : String.valueOf(value.get("id"));
    }

    private void appliquerStatutMetier(Stage stage) {
        if (stage == null || stage.getStatut() == StatutStage.TERMINE
                || stage.getStatut() == StatutStage.ANNULE
                || stage.getStatut() == StatutStage.REFUSE) {
            return;
        }

        resolveStageEndDate(stage);

        if (hasReachedStageEnd(stage)) {
            stage.setStatut(StatutStage.TERMINE);
            return;
        }

        if (peutDeclencherStage(stage)) {
            stage.setStatut(StatutStage.EN_COURS);
            return;
        }

        stage.setStatut(StatutStage.PAS_COMMENCE);
    }

    private boolean peutDeclencherStage(Stage stage) {
        return stage != null
                && stage.getEncadrantProfessionnel() != null
                && stage.getStatutSujet() == StatutValidation.VALIDEE;
    }

    private Stage enregistrerStageAvecDeclenchement(Stage stage, StatutStage previousStatus) {
        resolveStageEndDate(stage);
        initialiserStatutsSections(stage);
        synchroniserEtatEspaces(stage);
        appliquerStatutMetier(stage);

        StatutStage oldStatus = previousStatus != null ? previousStatus : stage.getStatut();
        Stage savedStage = stageRepository.save(stage);

        if (savedStage.getStatut() == StatutStage.EN_COURS) {
            if (!hasText(savedStage.getTrelloBoardId())) {
                initialiserTrelloPourStage(savedStage);
            }
            if (oldStatus != StatutStage.EN_COURS) {
                genererConventionSiAbsente(savedStage);
            }
            savedStage = stageRepository.save(savedStage);
        }

        savedStage = synchroniserAutomatisationReunionFinaleEtOuverture(savedStage);
        enqueteSatisfactionService.creerEnquetesPourStageSiNecessaire(savedStage);

        return savedStage;
    }

    private void initialiserStatutsSections(Stage stage) {
        if (stage == null) {
            return;
        }
        if (stage.getSectionEvaluationOuverte() == null) {
            stage.setSectionEvaluationOuverte(Boolean.FALSE);
        }
        if (stage.getSectionEnqueteOuverte() == null) {
            stage.setSectionEnqueteOuverte(Boolean.FALSE);
        }
        if (stage.getNotificationOuvertureEspacesEnvoyee() == null) {
            stage.setNotificationOuvertureEspacesEnvoyee(Boolean.FALSE);
        }
    }

    private void resolveStageEndDate(Stage stage) {
        if (stage == null) {
            return;
        }

        LocalDate computedDate = calculerDateFin(stage.getDateDebut(), stage.getDuree());
        if (computedDate != null) {
            stage.setDateFin(computedDate);
        }

        if (stage.getDateFin() == null) {
            throw new IllegalStateException("La date de fin du stage est absente.");
        }
    }

    private boolean hasReachedStageEnd(Stage stage) {
        return stage != null
                && stage.getDateFin() != null
                && !LocalDate.now().isBefore(stage.getDateFin());
    }

    private Stage synchroniserAutomatisationReunionFinaleEtOuverture(Stage stage) {
        if (stage == null) {
            return null;
        }

        resolveStageEndDate(stage);
        initialiserStatutsSections(stage);

        boolean stageChanged = false;
        ReunionFinale reunionFinale = ensureAutomaticFinalMeeting(stage);

        if (hasReachedStageEnd(stage)) {
            if (stage.getStatut() != StatutStage.TERMINE) {
                stage.setStatut(StatutStage.TERMINE);
                stageChanged = true;
            }
            if (ouvrirEspacesFinDeStage(stage)) {
                stageChanged = true;
            }
            if (initialiserFicheEvaluationSiNecessaire(stage, reunionFinale)) {
                stageChanged = true;
            }
        } else if (fermerEspacesAvantEcheance(stage)) {
            stageChanged = true;
        }

        if (stageChanged) {
            stage = stageRepository.save(stage);
        }

        if (Boolean.TRUE.equals(stage.getSectionEnqueteOuverte())) {
            enqueteSatisfactionService.creerEnquetesPourStageSiNecessaire(stage);
        }

        if (shouldNotifyStageEndOpening(stage)) {
            notifierOuvertureEspacesFinDeStage(stage);
            stage.setNotificationOuvertureEspacesEnvoyee(Boolean.TRUE);
            stage = stageRepository.save(stage);
        }

        return stage;
    }

    private ReunionFinale ensureAutomaticFinalMeeting(Stage stage) {
        if (stage == null || stage.getId() == null) {
            throw new EntityNotFoundException("Stage introuvable");
        }

        resolveStageEndDate(stage);

        ReunionFinale reunionFinale = reunionFinaleRepository.findFirstByStageIdOrderByIdAsc(stage.getId())
                .orElse(null);

        if (reunionFinale == null) {
            try {
                ReunionFinale created = new ReunionFinale();
                created.setStage(stage);
                created.setCahierStage(stage.getCahierStage());
                created.setNumReunion("RF-" + stage.getId());
                created.setDate(stage.getDateFin());
                created.setHeure(HEURE_REUNION_FINALE_PAR_DEFAUT);
                created.setTypeEncadrantCreateur("SYSTEME");
                created.setNomEncadrantCreateur("SYSTEME");
                created.setParticipants(resolveAutomaticFinalMeetingParticipants(stage));
                return reunionFinaleRepository.save(created);
            } catch (RuntimeException ex) {
                throw new IllegalStateException(
                        "Erreur technique lors de la creation automatique de la reunion finale pour le stage " + stage.getId() + ".",
                        ex
                );
            }
        }

        boolean changed = false;
        if (!Objects.equals(reunionFinale.getDate(), stage.getDateFin())) {
            reunionFinale.setDate(stage.getDateFin());
            changed = true;
        }
        if (reunionFinale.getHeure() == null) {
            reunionFinale.setHeure(HEURE_REUNION_FINALE_PAR_DEFAUT);
            changed = true;
        }

        Set<Utilisateur> expectedParticipants = resolveAutomaticFinalMeetingParticipants(stage);
        if (!Objects.equals(reunionFinale.getParticipants(), expectedParticipants)) {
            reunionFinale.setParticipants(expectedParticipants);
            changed = true;
        }

        if (changed) {
            reunionFinale.setCahierStage(stage.getCahierStage());
            reunionFinale = reunionFinaleRepository.save(reunionFinale);
        }

        return reunionFinale;
    }

    private Set<Utilisateur> resolveAutomaticFinalMeetingParticipants(Stage stage) {
        Set<Utilisateur> participants = new LinkedHashSet<>();
        if (stage == null) {
            return participants;
        }
        if (stage.getStagiaire() != null) {
            participants.add(stage.getStagiaire());
        }
        if (stage.getEncadrantAcademique() != null) {
            participants.add(stage.getEncadrantAcademique());
        }
        if (stage.getEncadrantProfessionnel() != null) {
            participants.add(stage.getEncadrantProfessionnel());
        }
        if (stage.getTuteurEntreprise() != null) {
            participants.add(stage.getTuteurEntreprise());
        }
        return participants;
    }

    private boolean ouvrirEspacesFinDeStage(Stage stage) {
        boolean changed = false;
        if (!Boolean.TRUE.equals(stage.getSectionEvaluationOuverte())) {
            stage.setSectionEvaluationOuverte(Boolean.TRUE);
            changed = true;
        }
        if (!Boolean.TRUE.equals(stage.getSectionEnqueteOuverte())) {
            stage.setSectionEnqueteOuverte(Boolean.TRUE);
            changed = true;
        }
        return changed;
    }

    private boolean fermerEspacesAvantEcheance(Stage stage) {
        boolean changed = false;
        if (!Boolean.FALSE.equals(stage.getSectionEvaluationOuverte())) {
            stage.setSectionEvaluationOuverte(Boolean.FALSE);
            changed = true;
        }
        if (!Boolean.FALSE.equals(stage.getSectionEnqueteOuverte())) {
            stage.setSectionEnqueteOuverte(Boolean.FALSE);
            changed = true;
        }
        if (Boolean.TRUE.equals(stage.getNotificationOuvertureEspacesEnvoyee())) {
            stage.setNotificationOuvertureEspacesEnvoyee(Boolean.FALSE);
            changed = true;
        }
        return changed;
    }

    private void synchroniserEtatEspaces(Stage stage) {
        if (stage == null) {
            return;
        }
        if (hasReachedStageEnd(stage)) {
            ouvrirEspacesFinDeStage(stage);
            return;
        }
        fermerEspacesAvantEcheance(stage);
    }

    private boolean initialiserFicheEvaluationSiNecessaire(Stage stage, ReunionFinale reunionFinale) {
        if (stage == null || stage.getId() == null || reunionFinale == null) {
            return false;
        }
        if (stage.getStatut() != StatutStage.TERMINE) {
            return false;
        }
        if (stage.getFicheEvaluation() != null || ficheEvaluationExiste(stage.getId())) {
            return false;
        }

        FicheEvaluation ficheEvaluation = new FicheEvaluation();
        ficheEvaluation.setStage(stage);
        ficheEvaluation.setReunionFinale(reunionFinale);
        ficheEvaluation.setPointFortEncadrantPro("");
        ficheEvaluation.setAxeAmeliorationEncadrantPro("");
        ficheEvaluation.setPointFortResponsableEntreprise("");
        ficheEvaluation.setAxeAmeliorationResponsableEntreprise("");
        ficheEvaluation.setNoteFinale(0.0);
        stage.setFicheEvaluation(ficheEvaluation);
        return true;
    }

    private boolean ficheEvaluationExiste(Long stageId) {
        return stageId != null && ficheEvaluationRepository.existsByStageId(stageId);
    }

    private boolean shouldNotifyStageEndOpening(Stage stage) {
        return stage != null
                && Boolean.TRUE.equals(stage.getSectionEvaluationOuverte())
                && Boolean.TRUE.equals(stage.getSectionEnqueteOuverte())
                && !Boolean.TRUE.equals(stage.getNotificationOuvertureEspacesEnvoyee());
    }

    private void notifierOuvertureEspacesFinDeStage(Stage stage) {
        if (stage == null || stage.getId() == null) {
            return;
        }

        String titreStage = hasText(stage.getTitre()) ? stage.getTitre() : "votre stage";
        String message = "Les espaces d'evaluation et d'enquete de satisfaction sont maintenant accessibles pour le stage "
                + titreStage + ".";

        for (Long destinataireId : resolveStageEndNotificationRecipients(stage)) {
            notificationService.creerNotification(
                    destinataireId,
                    "Espaces de fin de stage ouverts",
                    message,
                    TYPE_NOTIFICATION_OUVERTURE_ESPACES,
                    stage.getId(),
                    "STAGE"
            );
        }
    }

    private Set<Long> resolveStageEndNotificationRecipients(Stage stage) {
        Set<Long> destinataires = new LinkedHashSet<>();
        if (stage.getStagiaire() != null && stage.getStagiaire().getId() != null) {
            destinataires.add(stage.getStagiaire().getId());
        }
        if (stage.getEncadrantAcademique() != null && stage.getEncadrantAcademique().getId() != null) {
            destinataires.add(stage.getEncadrantAcademique().getId());
        }
        if (stage.getEncadrantProfessionnel() != null && stage.getEncadrantProfessionnel().getId() != null) {
            destinataires.add(stage.getEncadrantProfessionnel().getId());
        }
        if (stage.getTuteurEntreprise() != null && stage.getTuteurEntreprise().getId() != null) {
            destinataires.add(stage.getTuteurEntreprise().getId());
        }

        List<Role> managementRoles = List.of(Role.RESPONSABLE_SERVICE_STAGES, Role.RESPONSABLE_UNIVERSITAIRE_STAGES);
        for (Role role : managementRoles) {
            utilisateurRepository.findByRole(role).stream()
                    .map(Utilisateur::getId)
                    .filter(Objects::nonNull)
                    .forEach(destinataires::add);
        }
        return destinataires;
    }

    private void ensureStageEnCoursForTrello(Stage stage) {
        if (stage == null || stage.getStatut() != StatutStage.EN_COURS) {
            throw new IllegalStateException("Trello n'est disponible que pour un stage EN_COURS.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void authorizeLinkedStageAccess(Stage stage) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        Long userId = utilisateur.getId();
        Role role = utilisateur.getRole();

        boolean allowed = switch (role) {
            case STAGIAIRE -> stage.getStagiaire() != null && userId.equals(stage.getStagiaire().getId());
            case ENCADRANT_ACADEMIQUE -> stage.getEncadrantAcademique() != null && userId.equals(stage.getEncadrantAcademique().getId());
            case ENCADRANT_PROFESSIONNEL -> stage.getEncadrantProfessionnel() != null && userId.equals(stage.getEncadrantProfessionnel().getId());
            default -> false;
        };

        if (!allowed) {
            throw new AccessDeniedException("Acces refuse : ce stage n'est pas lie a votre compte.");
        }
    }

    @Override
    public Map<String, Object> genererRapportStage(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        if (stage.getDateFin() == null) {
            throw new RuntimeException("La date de fin du stage est absente");
        }

        if (stage.getDateFin().isAfter(LocalDate.now())) {
            throw new RuntimeException("Le rapport ne peut être généré qu'à la fin du stage");
        }

        return getResumeTrelloStage(stageId);
    }

    @Override
    public StageEnqueteSectionStatusResponse getEnqueteSectionStatus(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        authorizeSurveyActorsAndManagement(stage);

        LocalDate dateReunionFinale = getDateReunionFinale(stage);
        RapportEnqueteSatisfaction rapport = rapportEnqueteSatisfactionRepository.findByStageId(stageId).orElse(null);

        return new StageEnqueteSectionStatusResponse(
                stageId,
                isSectionEnqueteOuverte(stage),
                dateReunionFinale,
                stage.getDateFin(),
                rapport != null,
                rapport != null ? rapport.getNomFichier() : null
        );
    }

    @Override
    public RapportEnqueteSatisfactionResponse uploadRapportEnquete(Long stageId, MultipartFile file) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        ensureRepresentativeUploadAccess(stage, utilisateur);

        if (!isSectionEnqueteOuverte(stage)) {
            throw new AccessDeniedException("La section enquete de satisfaction sera accessible a partir du dernier jour du stage.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier du rapport d'enquête est obligatoire.");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        Path baseDir = Path.of(rapportEnqueteUploadDir).toAbsolutePath().normalize();
        Path stageDir = baseDir.resolve("stage-" + stageId);

        try {
            Files.createDirectories(stageDir);
            String storedFilename = System.currentTimeMillis() + "-" + originalFilename;
            Path target = stageDir.resolve(storedFilename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            RapportEnqueteSatisfaction rapport = rapportEnqueteSatisfactionRepository.findByStageId(stageId)
                    .orElseGet(RapportEnqueteSatisfaction::new);
            rapport.setStage(stage);
            rapport.setUploadedBy(utilisateur);
            rapport.setNomFichier(originalFilename);
            rapport.setCheminFichier(target.toString());
            rapport.setDateUpload(LocalDateTime.now());

            RapportEnqueteSatisfaction saved = rapportEnqueteSatisfactionRepository.save(rapport);
            notifierRapportEnqueteDisponible(stage);
            return toRapportResponse(saved);
        } catch (IOException ex) {
            throw new RuntimeException("Impossible d'enregistrer le rapport d'enquête de satisfaction.", ex);
        }
    }

    @Override
    public Resource getRapportEnqueteResource(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        authorizeSurveyActorsAndManagement(stage);

        RapportEnqueteSatisfaction rapport = rapportEnqueteSatisfactionRepository.findByStageId(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Aucun rapport d'enquête de satisfaction n'est disponible pour ce stage."));

        Path path = Path.of(rapport.getCheminFichier()).toAbsolutePath().normalize();
        Resource resource = new PathResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            throw new EntityNotFoundException("Le fichier du rapport d'enquête est introuvable.");
        }

        return resource;
    }

    @Override
    public RapportEnqueteSatisfactionResponse getRapportEnqueteMetadata(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        authorizeSurveyActorsAndManagement(stage);

        RapportEnqueteSatisfaction rapport = rapportEnqueteSatisfactionRepository.findByStageId(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Aucun rapport d'enquête de satisfaction n'est disponible pour ce stage."));

        return toRapportResponse(rapport);
    }

    private Stage enrichStageSurveyStatus(Stage stage) {
        if (stage != null) {
            stage.setSectionEvaluationOuverte(isSectionEvaluationOuverte(stage));
            stage.setSectionEnqueteOuverte(isSectionEnqueteOuverte(stage));
        }
        return stage;
    }

    private boolean isSectionEvaluationOuverte(Stage stage) {
        if (stage == null) {
            return false;
        }

        if (stage.getSectionEvaluationOuverte() != null) {
            return Boolean.TRUE.equals(stage.getSectionEvaluationOuverte());
        }

        return hasReachedStageEnd(stage);
    }

    private boolean isSectionEnqueteOuverte(Stage stage) {
        if (stage == null) {
            return false;
        }

        if (stage.getSectionEnqueteOuverte() != null) {
            return Boolean.TRUE.equals(stage.getSectionEnqueteOuverte());
        }

        LocalDate dateReunionFinale = getDateReunionFinale(stage);
        return dateReunionFinale != null && !LocalDate.now().isBefore(dateReunionFinale);
    }

    private LocalDate getDateReunionFinale(Stage stage) {
        if (stage == null || stage.getId() == null) {
            return null;
        }

        return reunionFinaleRepository.findFirstByStageIdOrderByIdAsc(stage.getId())
                .map(ReunionFinale::getDate)
                .orElse(stage.getDateFin());
    }

    private void authorizeSurveyActorsAndManagement(Stage stage) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        Long userId = utilisateur.getId();

        boolean allowed = Set.of(
                Role.ADMINISTRATEUR,
                Role.RESPONSABLE_SERVICE_STAGES,
                Role.RESPONSABLE_UNIVERSITAIRE_STAGES
        ).contains(utilisateur.getRole())
                || (stage.getStagiaire() != null && userId.equals(stage.getStagiaire().getId()))
                || (stage.getEncadrantAcademique() != null && userId.equals(stage.getEncadrantAcademique().getId()))
                || (stage.getEncadrantProfessionnel() != null && userId.equals(stage.getEncadrantProfessionnel().getId()))
                || (utilisateur instanceof ResponsableEntreprise responsableEntreprise
                    && responsableEntreprise.getEntreprise() != null
                    && stage.getEntreprise() != null
                    && Objects.equals(responsableEntreprise.getEntreprise().getId(), stage.getEntreprise().getId()));

        if (!allowed) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à consulter cette section d'enquête.");
        }
    }

    private void ensureRepresentativeUploadAccess(Stage stage, Utilisateur utilisateur) {
        if (!(utilisateur instanceof ResponsableEntreprise responsableEntreprise)
                || utilisateur.getRole() != Role.RESPONSABLE_ENTREPRISE) {
            throw new AccessDeniedException("Seul le représentant entreprise peut uploader le rapport d'enquête.");
        }

        Long userEntrepriseId = responsableEntreprise.getEntreprise() != null ? responsableEntreprise.getEntreprise().getId() : null;
        Long stageEntrepriseId = stage.getEntreprise() != null ? stage.getEntreprise().getId() : null;
        if (!Objects.equals(userEntrepriseId, stageEntrepriseId)) {
            throw new AccessDeniedException("Vous ne pouvez pas uploader un rapport pour une autre entreprise.");
        }
    }

    private RapportEnqueteSatisfactionResponse toRapportResponse(RapportEnqueteSatisfaction rapport) {
        Utilisateur uploadedBy = rapport.getUploadedBy();
        String uploadedByNomComplet = uploadedBy == null
                ? null
                : ((uploadedBy.getPrenom() == null ? "" : uploadedBy.getPrenom()) + " "
                + (uploadedBy.getNom() == null ? "" : uploadedBy.getNom())).trim();

        return new RapportEnqueteSatisfactionResponse(
                rapport.getId(),
                rapport.getNomFichier(),
                rapport.getDateUpload(),
                rapport.getStage() != null ? rapport.getStage().getId() : null,
                uploadedBy != null ? uploadedBy.getId() : null,
                uploadedByNomComplet == null || uploadedByNomComplet.isBlank() ? null : uploadedByNomComplet
        );
    }

    private void notifierRapportEnqueteDisponible(Stage stage) {
        java.util.LinkedHashSet<Long> destinataires = new java.util.LinkedHashSet<>();
        if (stage.getStagiaire() != null && stage.getStagiaire().getId() != null) {
            destinataires.add(stage.getStagiaire().getId());
        }
        if (stage.getEncadrantAcademique() != null && stage.getEncadrantAcademique().getId() != null) {
            destinataires.add(stage.getEncadrantAcademique().getId());
        }
        if (stage.getEncadrantProfessionnel() != null && stage.getEncadrantProfessionnel().getId() != null) {
            destinataires.add(stage.getEncadrantProfessionnel().getId());
        }
        if (stage.getTuteurEntreprise() != null && stage.getTuteurEntreprise().getId() != null) {
            destinataires.add(stage.getTuteurEntreprise().getId());
        }
        utilisateurRepository.findByRole(Role.ADMINISTRATEUR).forEach(user -> destinataires.add(user.getId()));
        utilisateurRepository.findByRole(Role.RESPONSABLE_SERVICE_STAGES).forEach(user -> destinataires.add(user.getId()));
        utilisateurRepository.findByRole(Role.RESPONSABLE_UNIVERSITAIRE_STAGES).forEach(user -> destinataires.add(user.getId()));

        for (Long destinataireId : destinataires) {
            notificationService.creerNotification(
                    destinataireId,
                    "Rapport d'enquête disponible",
                    "Le rapport d’enquête de satisfaction du stage est disponible.",
                    "ENQUETE_SATISFACTION",
                    stage.getId(),
                    "STAGE"
            );
        }
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "rapport-enquete.pdf";
        }

        String normalized = Path.of(originalFilename).getFileName().toString().trim();
        return normalized.isBlank() ? "rapport-enquete.pdf" : normalized.replaceAll("[\\r\\n]", "_");
    }
}

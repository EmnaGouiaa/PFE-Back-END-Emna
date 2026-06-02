package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ConventionStageDto;
import fsegs.pfebackendemnagouuiaa.dto.CreateStageRequest;
import fsegs.pfebackendemnagouuiaa.validation.StagePeriodValidation;
import fsegs.pfebackendemnagouuiaa.entities.*;
import fsegs.pfebackendemnagouuiaa.mapper.StageMapper;
import fsegs.pfebackendemnagouuiaa.repository.ConventionStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantAcademiqueRepository;
import fsegs.pfebackendemnagouuiaa.repository.EncadrantProfessionnelRepository;
import fsegs.pfebackendemnagouuiaa.repository.EntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.OffreStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.StagiaireRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import fsegs.pfebackendemnagouuiaa.exception.BusinessException;
import fsegs.pfebackendemnagouuiaa.support.DemoStageSupport;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
    /** Fuseau unique pour le calcul des statuts de stage (comparaison sur des dates calendaires). */
    private static final ZoneId STAGE_BUSINESS_ZONE = ZoneId.of("Africa/Tunis");

    private final StageRepository stageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final EncadrantAcademiqueRepository encadrantAcademiqueRepository;
    private final EncadrantProfessionnelRepository encadrantProfessionnelRepository;
    private final FicheEvaluationRepository ficheEvaluationRepository;
    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final OffreStageRepository offreStageRepository;
    private final ReunionFinaleRepository reunionFinaleRepository;
    private final TrelloService trelloService;
    private final ConventionStageService conventionStageService;
    private final ConventionStageRepository conventionStageRepository;
    private final StagiaireResolutionService stagiaireResolutionService;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final JwtService jwtService;

    // ────────────────────────────────────────────────────────────────────────
    //  Helpers métier
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Lève une {@link IllegalStateException} si le stage est déjà déclenché
     * (EN_COURS ou TERMINE). Les affectations d'encadrants deviennent
     * immuables une fois le stage démarré.
     *
     * @param stage  le stage à vérifier
     * @param action libellé de l'action tentée, inséré dans le message d'erreur
     */
    private void verifierAssignmentsNonVerrouilles(Stage stage, String action) {
        if (stage.getStatut() == StatutStage.EN_COURS || stage.getStatut() == StatutStage.TERMINE) {
            throw new IllegalStateException(
                    "Impossible de " + action + " : le stage est deja declenche (statut="
                    + stage.getStatut().name() + "). "
                    + "Les affectations d'encadrants sont verrouillees une fois le stage demarre.");
        }
    }

    /**
     * Si l'encadrant académique ou professionnel vient d'être modifié, que la date de fin du
     * stage est postérieure à aujourd'hui, et que le sujet était déjà validé, on réinitialise
     * la validation afin que le nouvel encadrant académique puisse la confirmer.
     */
    private void reinitialiserValidationSujetSiNecessaire(
            Stage stage, Long ancienEncadrantAcadId, Long ancienEncadrantProId) {
        if (stage.getStatutSujet() != StatutValidation.VALIDEE) return;
        if (stage.getDateFin() == null || !LocalDate.now().isBefore(stage.getDateFin())) return;

        Long newAcadId = stage.getEncadrantAcademique() != null ? stage.getEncadrantAcademique().getId() : null;
        Long newProId  = stage.getEncadrantProfessionnel() != null ? stage.getEncadrantProfessionnel().getId() : null;

        boolean superviseurChange = !Objects.equals(ancienEncadrantAcadId, newAcadId)
                                 || !Objects.equals(ancienEncadrantProId,  newProId);

        if (superviseurChange) {
            stage.setStatutSujet(StatutValidation.EN_ATTENTE);
            stage.setSujetValidePar(null);
            log.info("[SUJET] Validation réinitialisée pour le stage {} — encadrant acad: {} → {}, pro: {} → {}.",
                    stage.getId(), ancienEncadrantAcadId, newAcadId, ancienEncadrantProId, newProId);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Tâches planifiées
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Tâche planifiée quotidienne (par défaut à 01h00) qui parcourt tous les
     * stages en statut PAS_COMMENCE et les déclenche automatiquement lorsque
     * toutes les conditions sont réunies (voir {@link #peutDeclencherStage}).
     *
     * @return le nombre de stages effectivement déclenchés
     */
    @Override
    @Scheduled(cron = "${app.stage.declenchement.cron:0 0 1 * * *}", zone = "${app.stage.finalization.zone:Africa/Tunis}")
    @Transactional
    public int declencherStagesEligibles() {
        List<Stage> stagesEnAttente = stageRepository.findByStatutIn(List.of(StatutStage.PAS_COMMENCE));
        int declenches = 0;
        for (Stage stage : stagesEnAttente) {
            try {
                if (peutDeclencherStage(stage)) {
                    enregistrerStageAvecDeclenchement(stage, StatutStage.PAS_COMMENCE);
                    declenches++;
                }
            } catch (RuntimeException ex) {
                log.error("[DECLENCHEMENT] Echec pour le stage id={}", stage.getId(), ex);
            }
        }
        log.info("[DECLENCHEMENT] {} stage(s) declenche(s) automatiquement.", declenches);
        return declenches;
    }

    @Override
    public Stage synchronizeStageStatut(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable avec l'id : " + stageId));
        appliquerStatutMetier(stage);
        return stageRepository.save(stage);
    }

    @Scheduled(cron = "${app.stage.finalization.cron:0 0 */1 * * *}", zone = "${app.stage.finalization.zone:Africa/Tunis}")
    @Transactional
    public void synchroniserFinStagesPlanifies() {
        LocalDate today = LocalDate.now();
        List<Stage> stagesDus = stageRepository.findByDateFinLessThanEqualAndStatutIn(
                today,
                List.of(StatutStage.A_VENIR, StatutStage.PAS_COMMENCE, StatutStage.EN_COURS, StatutStage.TERMINE)
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
        // ── Règle métier : stagiaire ET encadrant professionnel obligatoires ──
        if (request.getStagiaireId() == null) {
            throw new IllegalArgumentException(
                    "La creation d'un stage requiert un stagiaire affecte. "
                    + "Veuillez selectionner un stagiaire avant de creer le stage.");
        }
        if (request.getEncadrantProfessionnelId() == null) {
            throw new IllegalArgumentException(
                    "La creation d'un stage requiert un encadrant professionnel affecte. "
                    + "Veuillez affecter un encadrant professionnel avant de creer le stage.");
        }

        validateStageDuration(request.getDuree());
        validateStageDates(request);

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

        validateStageDuration(request.getDuree());
        validateStageDates(request);

        stage.setTitre(request.getTitre());
        stage.setDateDebut(request.getDateDebut());
        stage.setDateFin(request.getDateFin());
        stage.setDuree(request.getDuree());
        stage.setNbSemaine(request.getNbSemaine());
        stage.setNiveauSouhaite(request.getNiveauSouhaite());
        stage.setSujet(request.getSujet());
        remplirRelations(stage, request);
        reinitialiserValidationSujetSiNecessaire(stage, previousEncadrantAcademiqueId, previousEncadrantProfessionnelId);
        appliquerStatutMetier(stage);

        Stage updatedStage = enregistrerStageAvecDeclenchement(stage, previousStatus);
        notifierEncadrantsAffectes(updatedStage, previousEncadrantAcademiqueId, previousEncadrantProfessionnelId);
        return enrichStageSurveyStatus(updatedStage);
    }

    @Override
    public Stage getStageById(Long id) {
        return stageRepository.findById(id)
                .filter(this::isStageVisibleDansListes)
                .map(this::synchroniserStatutSiNecessaire)
                .map(this::enrichStageSurveyStatus)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
    }

    @Override
    public List<Stage> getAllStages() {
        return getAllStages(null);
    }

    @Override
    public List<Stage> getAllStages(String statutSuivi) {
        StatutSuiviStage filter = StatutSuiviStage.parseFilter(statutSuivi);
        return stageRepository.findAll().stream()
                .map(this::mapStagePourListe)
                .filter(Objects::nonNull)
                .filter(stage -> filter == null || StatutSuiviStage.from(stage.getStatut()) == filter)
                .toList();
    }

    /**
     * Recalcule le statut du stage par les dates et persiste si la valeur a change.
     * Permet aux endpoints de listing / detail de toujours renvoyer un statut a jour
     * sans depender uniquement du cron horaire.
     */
    private Stage synchroniserStatutSiNecessaire(Stage stage) {
        if (stage == null) return null;
        StatutStage previous = stage.getStatut();
        appliquerStatutMetier(stage);
        if (stage.getStatut() != previous) {
            log.debug("Statut du stage {} synchronise automatiquement : {} -> {}",
                    stage.getId(), previous, stage.getStatut());
            return stageRepository.save(stage);
        }
        return stage;
    }

    @Override
    @Transactional
    public void deleteStage(Long id) {
        Stage stage = stageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        supprimerStageAutomatiquementApresRefus(stage);
    }

    @Override
    @Transactional
    public void supprimerStageAutomatiquementApresRefus(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        supprimerStageAutomatiquementApresRefus(stage);
    }

    /**
     * Règle métier : un stage refusé n'est pas conservé en base — suppression immédiate
     * avec nettoyage des relations (réunions, documents, évaluations) et libération de l'offre.
     */
    private void supprimerStageAutomatiquementApresRefus(Stage stage) {
        if (stage == null || stage.getId() == null) {
            return;
        }
        log.info("[Stage] Suppression automatique du stage id={} suite au refus.", stage.getId());
        libererOffreSourceApresSuppressionStage(stage);
        detachReunionParticipants(stage);
        stageRepository.delete(stage);
        stageRepository.flush();
    }

    private void libererOffreSourceApresSuppressionStage(Stage stage) {
        OffreStage offre = stage.getOffreSource();
        if (offre == null || offre.getId() == null) {
            return;
        }
        Stagiaire stagiaire = stage.getStagiaire();
        if (stagiaire == null || stagiaire.getId() == null
                || offre.getStagiaireAffecte() == null
                || !stagiaire.getId().equals(offre.getStagiaireAffecte().getId())) {
            return;
        }
        offre.setStagiaireAffecte(null);
        if (offre.getStatut() == StatutOffre.AFFECTEE) {
            offre.setStatut(StatutOffre.VALIDEE);
        }
        offreStageRepository.save(offre);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void purgerStagesRefusesObsoletes() {
        List<Stage> refuses = stageRepository.findByStatutIn(List.of(StatutStage.REFUSE));
        if (refuses.isEmpty()) {
            return;
        }
        log.info("Purge de {} stage(s) au statut REFUSE (suppression automatique)...", refuses.size());
        for (Stage stage : refuses) {
            supprimerStageAutomatiquementApresRefus(stage);
        }
    }

    private boolean isStageVisibleDansListes(Stage stage) {
        return stage != null && stage.getStatut() != StatutStage.REFUSE;
    }

    private Stage mapStagePourListe(Stage stage) {
        if (!isStageVisibleDansListes(stage)) {
            return null;
        }
        return enrichStageSurveyStatus(synchroniserStatutSiNecessaire(stage));
    }

    @Override
    @Transactional
    public Stage affecterEncadrantAcademique(Long stageId, Long encadrantAcademiqueId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        // ── Règle métier : verrou après déclenchement ──
        verifierAssignmentsNonVerrouilles(stage, "modifier l'encadrant academique");

        StatutStage previousStatus = stage.getStatut();
        Long previousEncadrantAcademiqueId = stage.getEncadrantAcademique() != null ? stage.getEncadrantAcademique().getId() : null;
        Long previousEncadrantProfessionnelId = stage.getEncadrantProfessionnel() != null ? stage.getEncadrantProfessionnel().getId() : null;

        EncadrantAcademique encadrant = encadrantAcademiqueRepository.findById(encadrantAcademiqueId)
                .orElseThrow(() -> new EntityNotFoundException("EncadrantAcademique introuvable"));

        synchroniserEncadrantAcademiqueStagiaire(stage.getStagiaire(), encadrant);
        synchroniserStagesActifsDuStagiaire(stage.getStagiaire(), encadrant);
        stage.setEncadrantAcademique(encadrant);
        reinitialiserValidationSujetSiNecessaire(stage, previousEncadrantAcademiqueId, previousEncadrantProfessionnelId);
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
    @Transactional
    public Stage affecterEncadrantProfessionnel(Long stageId, Long encadrantProfessionnelId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        // ── Règle métier : verrou après déclenchement ──
        verifierAssignmentsNonVerrouilles(stage, "modifier l'encadrant professionnel");

        StatutStage previousStatus = stage.getStatut();
        Long previousEncadrantAcademiqueId = stage.getEncadrantAcademique() != null ? stage.getEncadrantAcademique().getId() : null;
        Long previousEncadrantProfessionnelId = stage.getEncadrantProfessionnel() != null ? stage.getEncadrantProfessionnel().getId() : null;

        EncadrantProfessionnel encadrant = encadrantProfessionnelRepository.findById(encadrantProfessionnelId)
                .orElseThrow(() -> new EntityNotFoundException("EncadrantProfessionnel introuvable"));

        stage.setEncadrantProfessionnel(encadrant);
        reinitialiserValidationSujetSiNecessaire(stage, previousEncadrantAcademiqueId, previousEncadrantProfessionnelId);
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
        return stageRepository.findByStagiaireId(stagiaireId).stream()
                .map(this::mapStagePourListe)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<Stage> getStagesByEntreprise(Long entrepriseId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() == Role.RESPONSABLE_ENTREPRISE) {
            ensureEntrepriseScope(entrepriseId, utilisateur);
        }
        return stageRepository.findByEntrepriseId(entrepriseId).stream()
                .map(this::mapStagePourListe)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<Stage> getStagesByEncadrantAcademique(Long encadrantId) {
        return stageRepository.findByEncadrantAcademiqueId(encadrantId).stream()
                .map(this::mapStagePourListe)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<Stage> getStagesByEncadrantProfessionnel(Long encadrantId) {
        return stageRepository.findByEncadrantProfessionnelId(encadrantId).stream()
                .map(this::mapStagePourListe)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<Stage> getStagesPourEncadrantAcademiqueAuthentifie() {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.ENCADRANT_ACADEMIQUE) {
            throw new RuntimeException("Acces refuse : role encadrant academique requis.");
        }
        return stageRepository.findByEncadrantAcademiqueId(utilisateur.getId()).stream()
                .map(this::mapStagePourListe)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<Stage> getStagesPourEncadrantProfessionnelAuthentifie() {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.ENCADRANT_PROFESSIONNEL) {
            throw new RuntimeException("Acces refuse : role encadrant professionnel requis.");
        }
        return stageRepository.findByEncadrantProfessionnelId(utilisateur.getId()).stream()
                .map(this::mapStagePourListe)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<Stage> getStagesPourStagiaireAuthentifie() {
        Stagiaire stagiaire = getAuthenticatedStagiaire();
        log.debug("getStagesPourStagiaireAuthentifie: stagiaireId={}, email={}", stagiaire.getId(), stagiaire.getEmail());

        List<Stage> allStages = stageRepository.findByStagiaireId(stagiaire.getId());
        log.debug("Nombre total de stages pour le stagiaire: {}", allStages.size());
        allStages.forEach(stage -> log.debug("  - Stage id={}, titre={}, statut={}, stagiaireId={}",
                stage.getId(), stage.getTitre(), stage.getStatut(),
                stage.getStagiaire() != null ? stage.getStagiaire().getId() : null));

        List<Stage> filtered = allStages.stream()
                .map(this::mapStagePourListe)
                .filter(Objects::nonNull)
                .toList();

        log.debug("Stages apres filtrage (non-REFUSE): {}", filtered.size());
        return filtered;
    }

    @Override
    public Stage getStageCourantPourStagiaireAuthentifie() {
        List<Stage> stages = getStagesPourStagiaireAuthentifie();

        return stages.stream()
                .filter(stage -> stage.getStatut() == StatutStage.EN_COURS)
                .findFirst()
                .or(() -> stages.stream()
                        .filter(stage -> stage.getStatut() == StatutStage.A_VENIR
                                || stage.getStatut() == StatutStage.PAS_COMMENCE)
                        .findFirst())
                .or(() -> stages.stream().filter(stage -> stage.getStatut() == StatutStage.TERMINE).findFirst())
                .map(this::enrichStageSurveyStatus)
                .orElseThrow(() -> new EntityNotFoundException("Aucun stage ne vous a encore ete affecte."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Stage> getStagesPourResponsableEntrepriseAuthentifie() {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.RESPONSABLE_ENTREPRISE) {
            throw new AccessDeniedException("Acces refuse : role responsable entreprise requis.");
        }
        ResponsableEntreprise responsable = (ResponsableEntreprise) utilisateur;
        if (responsable.getEntreprise() == null) {
            throw new EntityNotFoundException("Aucune entreprise associee a ce compte.");
        }
        Long entrepriseId = responsable.getEntreprise().getId();
        return stageRepository.findByEntrepriseId(entrepriseId).stream()
                .map(this::mapStagePourListe)
                .filter(Objects::nonNull)
                .toList();
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

        if (offre.getStagiaireAffecte() != null
                && offre.getStagiaireAffecte().getId() != null
                && !Objects.equals(offre.getStagiaireAffecte().getId(), stagiaire.getId())) {
            throw new IllegalStateException("Cette offre est deja affectee a un autre etudiant.");
        }

        ensureNoActiveStageForStagiaire(stagiaire, offre);

        validateStageDuration(offre.getDuree());
        if (offre.getDateDebutPrevue() != null && offre.getDuree() != null) {
            StagePeriodValidation.validatePeriod(offre.getDateDebutPrevue(), offre.getDuree());
        }

        Stage stage = new Stage();
        stage.setTitre(offre.getTitre());
        stage.setSujet(offre.getTitre());
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
        if (offre.getEncadrantPro() != null) {
            stage.setEncadrantProfessionnel(offre.getEncadrantPro());
        }
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
        if (!trelloService.isEnabled()) {
            log.info("[Trello] Integration desactivee — board non cree pour le stage '{}'.", stage.getTitre());
            return;
        }

        log.info("[Trello] Creation du board pour le stage '{}'.", stage.getTitre());
        Map<String, Object> board = trelloService.createBoard("Stage - " + stage.getTitre());
        if (board == null || board.get("id") == null) {
            log.warn("[Trello] La creation du board a echoue (reponse nulle ou sans id) pour le stage '{}'.", stage.getTitre());
            return;
        }

        stage.setTrelloBoardId((String) board.get("id"));
        stage.setTrelloBoardUrl((String) board.get("url"));

        Map<String, Object> todo  = trelloService.createList(stage.getTrelloBoardId(), "To Do");
        Map<String, Object> doing = trelloService.createList(stage.getTrelloBoardId(), "In Progress");
        Map<String, Object> done  = trelloService.createList(stage.getTrelloBoardId(), "Done");

        stage.setTrelloTodoListId((String) todo.get("id"));
        stage.setTrelloDoingListId((String) doing.get("id"));
        stage.setTrelloDoneListId((String) done.get("id"));
    }

    /** Verifie la presence et le minimum de duree ; le plafond par periode est valide dans {@link #validateStageDates}. */
    private void validateStageDuration(Integer duree) {
        if (duree == null) {
            throw new IllegalArgumentException(
                    "La duree du stage est obligatoire (minimum "
                            + StagePeriodValidation.MIN_DURATION_MONTHS
                            + " mois ; maximum 3 mois en periode 1, 2 mois en periode 2).");
        }
        if (duree < StagePeriodValidation.MIN_DURATION_MONTHS) {
            throw new IllegalArgumentException(
                    "La duree du stage doit etre d'au moins "
                            + StagePeriodValidation.MIN_DURATION_MONTHS
                            + " mois (valeur recue : " + duree + " mois).");
        }
    }

    private void validateStageDates(CreateStageRequest request) {
        if (request.getDateDebut() == null) {
            return;
        }
        if (request.getDuree() != null) {
            StagePeriodValidation.validatePeriod(request.getDateDebut(), request.getDuree());
            return;
        }
        if (request.getDateFin() != null) {
            StagePeriodValidation.validatePeriod(request.getDateDebut(), request.getDateFin());
        }
    }

    private LocalDate calculerDateFin(LocalDate dateDebut, Integer dureeMois) {
        if (dateDebut == null || dureeMois == null) {
            return null;
        }

        return StagePeriodValidation.calculateEndDate(dateDebut, dureeMois);
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
                    "Stage validï¿½",
                    "Votre stage " + (stage.getTitre() == null ? "sï¿½lectionnï¿½" : stage.getTitre()) + " a ï¿½tï¿½ validï¿½ par votre entreprise.",
                    "VALIDATION_ENTREPRISE",
                    stage.getId(),
                    "STAGE"
            );
        }

        if (stage.getEncadrantAcademique() != null && stage.getEncadrantAcademique().getId() != null) {
            notificationService.creerNotification(
                    stage.getEncadrantAcademique().getId(),
                    "Stage validï¿½ cï¿½tï¿½ entreprise",
                    "Le stage " + (stage.getTitre() == null ? "sï¿½lectionnï¿½" : stage.getTitre()) + " a ï¿½tï¿½ validï¿½ par l'entreprise.",
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

        boolean isModification = previousEncadrantId != null;
        boolean isAcademique = "academique".equalsIgnoreCase(typeEncadrant);

        String typeNouvel = isAcademique
                ? (isModification ? "MODIFICATION_ENCADRANT_ACADEMIQUE" : "AFFECTATION_ENCADRANT_ACADEMIQUE")
                : (isModification ? "MODIFICATION_ENCADRANT_PROFESSIONNEL" : "AFFECTATION_ENCADRANT_PROFESSIONNEL");

        String stageTitre = (stage.getTitre() != null && !stage.getTitre().isBlank()) ? stage.getTitre() : "stage #" + stage.getId();

        notificationService.creerNotification(
                currentEncadrantId,
                isModification ? "Changement d'affectation" : "Nouvelle affectation encadrant",
                "Vous avez ete affecte comme encadrant " + typeEncadrant + " pour le stage : " + stageTitre + ".",
                typeNouvel,
                stage.getId(),
                "STAGE"
        );

        if (isModification && previousEncadrantId != null) {
            notificationService.creerNotification(
                    previousEncadrantId,
                    "Fin d'affectation encadrant",
                    "Vous n'etes plus encadrant " + typeEncadrant + " pour le stage : " + stageTitre + ".",
                    typeNouvel,
                    stage.getId(),
                    "STAGE"
            );
        }

        if (stage.getStagiaire() != null && stage.getStagiaire().getId() != null) {
            String messageStagiaire = isModification
                    ? "Votre encadrant " + typeEncadrant + " a ete modifie pour votre stage."
                    : "Un encadrant " + typeEncadrant + " a ete affecte a votre stage.";
            notificationService.creerNotification(
                    stage.getStagiaire().getId(),
                    "Encadrant " + typeEncadrant + (isModification ? " mis a jour" : " affecte"),
                    messageStagiaire,
                    typeNouvel,
                    stage.getId(),
                    "STAGE"
            );
        }
    }

    @Override
    public Stage validerSujetParEncadrantAcademique(Long stageId, Long encadrantId) {
        // E2 — stage introuvable (404)
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Aucun stage n'est associé à ce stagiaire."));
        StatutStage previousStatus = stage.getStatut();

        // E1 — encadrant introuvable (404)
        EncadrantAcademique encadrant = encadrantAcademiqueRepository.findById(encadrantId)
                .orElseThrow(() -> new EntityNotFoundException("Stagiaire introuvable."));

        // E4 — l'encadrant connecte n'est pas affecte a ce stage (403)
        if (stage.getEncadrantAcademique() == null || !stage.getEncadrantAcademique().getId().equals(encadrantId)) {
            throw new AccessDeniedException("Accès non autorisé à ce stage.");
        }

        // E3 — aucun sujet renseigne (400)
        String sujet = stage.getSujet();
        if (sujet == null || sujet.trim().isEmpty()) {
            throw new IllegalArgumentException("Aucun sujet de stage n'est renseigné.");
        }

        // E5 — sujet deja valide (400)
        if (stage.getStatutSujet() == StatutValidation.VALIDEE) {
            throw new IllegalArgumentException("Le sujet de stage est déjà validé.");
        }

        stage.setSujetValidePar(encadrant);
        stage.setStatutSujet(StatutValidation.VALIDEE);
        appliquerStatutMetier(stage);

        Stage saved;
        try {
            saved = enregistrerStageAvecDeclenchement(stage, previousStatus);
        } catch (RuntimeException ex) {
            // E6 — erreur technique
            log.error("Echec technique lors de la validation du sujet. stageId={}", stageId, ex);
            throw new RuntimeException("Une erreur technique est survenue lors de la mise à jour.", ex);
        }

        try {
            notifierValidationSujet(saved, true);
        } catch (RuntimeException ex) {
            // E7 — l'echec de notification ne doit pas annuler la validation deja persistee.
            log.warn("Validation effectuee mais echec d'envoi de notification. stageId={} : {}",
                    stageId, ex.getMessage());
        }
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
    public void refuserSujetParEncadrantAcademique(Long stageId, Long encadrantId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        EncadrantAcademique encadrant = encadrantAcademiqueRepository.findById(encadrantId)
                .orElseThrow(() -> new EntityNotFoundException("EncadrantAcademique introuvable"));

        if (stage.getEncadrantAcademique() == null || !stage.getEncadrantAcademique().getId().equals(encadrantId)) {
            throw new RuntimeException("Cet encadrant acadï¿½mique n'est pas affectï¿½ ï¿½ ce stage");
        }

        stage.setSujetValidePar(encadrant);
        stage.setStatutSujet(StatutValidation.REFUSEE);
        try {
            notifierValidationSujet(stage, false);
        } catch (RuntimeException ex) {
            log.warn("Refus du sujet effectue mais echec d'envoi de notification. stageId={} : {}",
                    stageId, ex.getMessage());
        }
        supprimerStageAutomatiquementApresRefus(stage);
    }

    @Override
    public void refuserSujetParEncadrantAcademiqueAuthentifie(Long stageId) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (utilisateur.getRole() != Role.ENCADRANT_ACADEMIQUE) {
            throw new RuntimeException("Acces refuse : seul l'encadrant academique affecte peut refuser le sujet.");
        }
        refuserSujetParEncadrantAcademique(stageId, utilisateur.getId());
    }

    @Override
    public Map<String, Object> createTrelloBoardIfNotExists(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        authorizeLinkedStageAccess(stage);
        rejectIfStageDateFinPassedForTrello(stage);

        if (hasText(stage.getTrelloBoardId())) {
            if (!hasText(stage.getTrelloBoardUrl())) {
                stage.setTrelloBoardUrl(buildTrelloBoardUrlFromId(stage.getTrelloBoardId()));
                stage = stageRepository.save(stage);
            }
            return buildTrelloBoardResponse(stage, false);
        }

        ensureStageEnCoursForTrello(stage);

        if (!trelloService.isEnabled()) {
            throw new IllegalStateException(
                    "L'integration Trello est desactivee (trello.enabled=false dans application.properties). "
                    + "Liberez de l'espace dans votre workspace Trello puis remettez trello.enabled=true.");
        }

        try {
            String boardName = buildTrelloBoardName(stage);
            Map<String, Object> board = trelloService.createBoard(boardName);
            if (board == null || board.get("id") == null) {
                throw new IllegalStateException("Reponse Trello invalide lors de la creation du board.");
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
            // On expose la vraie cause renvoyee par Trello (ex. 401 cle/token invalide,
            // 429 quota depasse, service injoignable) au lieu d'un message opaque.
            String cause = (ex.getMessage() != null && !ex.getMessage().isBlank())
                    ? ex.getMessage()
                    : ex.getClass().getSimpleName();
            throw new IllegalStateException(
                    "Echec de la creation du board Trello. Cause : " + cause
                            + ". Verifiez la cle et le token Trello (application.properties).",
                    ex);
        }
    }

    @Override
    public Map<String, Object> getResumeTrelloStage(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

        Map<String, Object> resume = new LinkedHashMap<>();
        resume.put("stageId", stage.getId());
        resume.put("titreStage", stage.getTitre());
        resume.put("sujet", stage.getSujet());

        // Trello est optionnel : s'il est indisponible (token invalide, service injoignable),
        // le suivi du stage reste accessible avec un resume vide plutot que de planter la page.
        try {
            // Lecture seule : un stage termine peut encore afficher son tableau existant.
            // On ne cree un board que tant que le stage est en cours.
            if (!hasText(stage.getTrelloBoardId()) && !isStageDateFinPassed(stage)) {
                createTrelloBoardIfNotExists(stageId);
            }
            stage = stageRepository.findById(stageId)
                    .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));

            if (!hasText(stage.getTrelloBoardId())) {
                fillResumeTrelloIndisponible(resume, stage,
                        "Aucun tableau Trello n'est associe a ce stage.");
                return resume;
            }

            List<Map<String, Object>> listes = trelloService.getListsByBoard(stage.getTrelloBoardId());
            List<Map<String, Object>> cartes = trelloService.getCardsByBoard(stage.getTrelloBoardId());
            if (cartes == null) {
                cartes = List.of();
            }

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

            resume.put("trelloDisponible", true);
            resume.put("trelloBoardId", stage.getTrelloBoardId());
            resume.put("trelloBoardUrl", stage.getTrelloBoardUrl());
            resume.put("nombreTotalTaches", cartes.size());
            resume.put("nombreTachesAFaire", tachesAFaire.size());
            resume.put("nombreTachesEnCours", tachesEnCours.size());
            resume.put("nombreTachesTerminees", tachesTerminees.size());
            resume.put("listes", listes != null ? listes : List.of());
            resume.put("tachesAFaire", tachesAFaire);
            resume.put("tachesEnCours", tachesEnCours);
            resume.put("tachesTerminees", tachesTerminees);
        } catch (RuntimeException ex) {
            // Degradation gracieuse : on renvoie un resume vide sans interrompre le suivi.
            log.warn("Trello indisponible pour le stage {} : {}", stageId, ex.getMessage());
            fillResumeTrelloIndisponible(resume, stage,
                    "Le tableau Trello est momentanement indisponible. Le suivi du stage reste accessible.");
        }

        return resume;
    }

    private void fillResumeTrelloIndisponible(Map<String, Object> resume, Stage stage, String message) {
        resume.put("trelloDisponible", false);
        resume.put("messageTrello", message);
        resume.put("trelloBoardId", stage != null ? stage.getTrelloBoardId() : null);
        resume.put("trelloBoardUrl", stage != null ? stage.getTrelloBoardUrl() : null);
        resume.put("nombreTotalTaches", 0);
        resume.put("nombreTachesAFaire", 0);
        resume.put("nombreTachesEnCours", 0);
        resume.put("nombreTachesTerminees", 0);
        resume.put("listes", List.of());
        resume.put("tachesAFaire", List.of());
        resume.put("tachesEnCours", List.of());
        resume.put("tachesTerminees", List.of());
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

    /**
     * Date du jour métier (sans heure) dans le fuseau {@link #STAGE_BUSINESS_ZONE}.
     */
    private LocalDate todayStageBusiness() {
        return LocalDate.now(STAGE_BUSINESS_ZONE);
    }

    /**
     * Statut d'un stage = fonction pure des dates calendaires :
     *   - aujourd'hui &lt; dateDebut              → {@link StatutStage#A_VENIR} (non commencé)
     *   - dateDebut ≤ aujourd'hui ≤ dateFin       → {@link StatutStage#EN_COURS}
     *   - aujourd'hui &gt; dateFin                → {@link StatutStage#TERMINE}
     *
     * Les états manuels ANNULE / REFUSE ne sont jamais écrasés.
     */
    private void appliquerStatutMetier(Stage stage) {
        if (stage == null) {
            return;
        }
        if (stage.getStatut() == StatutStage.ANNULE || stage.getStatut() == StatutStage.REFUSE) {
            return;
        }

        resolveStageEndDate(stage);

        LocalDate today = todayStageBusiness();
        LocalDate dateDebut = stage.getDateDebut();
        LocalDate dateFin = stage.getDateFin();

        if (dateDebut == null) {
            if (stage.getStatut() != StatutStage.PAS_COMMENCE) {
                stage.setStatut(StatutStage.A_VENIR);
            }
            return;
        }

        if (today.isBefore(dateDebut)) {
            stage.setStatut(StatutStage.A_VENIR);
        } else if (dateFin != null && today.isAfter(dateFin)) {
            stage.setStatut(StatutStage.TERMINE);
        } else {
            stage.setStatut(StatutStage.EN_COURS);
        }
    }

    /**
     * Retourne {@code true} si la fiche d'évaluation du stage est présente et
     * entièrement verrouillée (signée par ENCADRANT_PROFESSIONNEL <em>et</em>
     * RESPONSABLE_ENTREPRISE). Condition nécessaire pour passer à
     * {@link StatutStage#TERMINE}.
     */
    private boolean estEvaluationSigneeEtComplete(Stage stage) {
        if (stage == null || stage.getId() == null) {
            return false;
        }
        // Vérification en mémoire si la relation est déjà chargée (évite une requête)
        FicheEvaluation ficheEnMemoire = stage.getFicheEvaluation();
        if (ficheEnMemoire != null) {
            return ficheEnMemoire.estVerrouillee();
        }
        // Sinon interroger le dépôt
        return ficheEvaluationRepository.findFirstByStageId(stage.getId())
                .map(FicheEvaluation::estVerrouillee)
                .orElse(false);
    }

    /**
     * Conditions de déclenchement d'un stage (passage PAS_COMMENCE → EN_COURS) :
     * <ol>
     *   <li>Un stagiaire est affecté.</li>
     *   <li>Un encadrant professionnel est affecté.</li>
     *   <li>Un encadrant académique est affecté.</li>
     *   <li>Le sujet est validé (StatutValidation.VALIDEE).</li>
     *   <li>La date de début est définie et est aujourd'hui ou déjà passée.</li>
     * </ol>
     */
    private boolean peutDeclencherStage(Stage stage) {
        return stage != null
                && stage.getStagiaire() != null
                && stage.getEncadrantProfessionnel() != null
                && stage.getEncadrantAcademique() != null
                && stage.getStatutSujet() == StatutValidation.VALIDEE
                && stage.getDateDebut() != null
                && !todayStageBusiness().isBefore(stage.getDateDebut());
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
                try {
                    initialiserTrelloPourStage(savedStage);
                } catch (Exception ex) {
                    log.warn("[Trello] Echec de l'initialisation du board pour le stage '{}' : {}",
                            savedStage.getTitre(), ex.getMessage());
                }
            }
            savedStage = stageRepository.save(savedStage);
        }

        if (savedStage.getStatut() != StatutStage.REFUSE
                && savedStage.getStatut() != StatutStage.ANNULE) {
            genererConventionSiAbsente(savedStage);
        }

        savedStage = synchroniserAutomatisationReunionFinaleEtOuverture(savedStage);

        StatutStage newStatus = savedStage.getStatut();
        if (oldStatus != null && oldStatus != newStatus) {
            notifierChangementStatut(savedStage, oldStatus, newStatus);
        }

        return savedStage;
    }

    private void initialiserStatutsSections(Stage stage) {
        if (stage == null) {
            return;
        }
        if (stage.getSectionEvaluationOuverte() == null) {
            stage.setSectionEvaluationOuverte(Boolean.FALSE);
        }
        if (stage.getNotificationOuvertureEspacesEnvoyee() == null) {
            stage.setNotificationOuvertureEspacesEnvoyee(Boolean.FALSE);
        }
    }

    private void resolveStageEndDate(Stage stage) {
        if (stage == null) {
            return;
        }
        if (DemoStageSupport.mustPreserveExplicitEndDate(stage)) {
            if (stage.getDateFin() == null) {
                throw new IllegalStateException("La date de fin du stage de demonstration est absente.");
            }
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
            if (ouvrirEspacesFinDeStage(stage)) {
                stageChanged = true;
            }
            if (initialiserFicheEvaluationSiNecessaire(stage, reunionFinale)) {
                stageChanged = true;
            }
            if (stage.getStatut() != StatutStage.TERMINE
                    && estEvaluationSigneeEtComplete(stage)) {
                stage.setStatut(StatutStage.TERMINE);
                stageChanged = true;
            }
        } else if (fermerEspacesAvantEcheance(stage)) {
            stageChanged = true;
        }

        if (stageChanged) {
            stage = stageRepository.save(stage);
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
        return participants;
    }

    private boolean ouvrirEspacesFinDeStage(Stage stage) {
        boolean changed = false;
        if (!Boolean.TRUE.equals(stage.getSectionEvaluationOuverte())) {
            stage.setSectionEvaluationOuverte(Boolean.TRUE);
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
        if (!hasReachedStageEnd(stage)) {
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
                && !Boolean.TRUE.equals(stage.getNotificationOuvertureEspacesEnvoyee());
    }

    private void notifierOuvertureEspacesFinDeStage(Stage stage) {
        if (stage == null || stage.getId() == null) {
            return;
        }

        String titreStage = hasText(stage.getTitre()) ? stage.getTitre() : "votre stage";
        String message = "L'espace d'evaluation est maintenant accessible pour le stage " + titreStage + ".";

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

        List<Role> managementRoles = List.of(Role.RESPONSABLE_STAGE);
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

    /**
     * Aligné avec la règle « stage terminé » : {@code dateFin} strictement avant aujourd'hui.
     * Bloque la creation de board Trello ; le resume en lecture reste autorise.
     */
    private boolean isStageDateFinPassed(Stage stage) {
        return stage != null && stage.getDateFin() != null && stage.getDateFin().isBefore(todayStageBusiness());
    }

    private void rejectIfStageDateFinPassedForTrello(Stage stage) {
        if (isStageDateFinPassed(stage)) {
            throw new BusinessException("Le stage est terminé. Action impossible.");
        }
    }

    private String buildTrelloBoardUrlFromId(String boardId) {
        if (!hasText(boardId)) {
            return "";
        }
        return "https://trello.com/b/" + boardId.trim();
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
            throw new RuntimeException("Le rapport ne peut ï¿½tre gï¿½nï¿½rï¿½ qu'ï¿½ la fin du stage");
        }

        return getResumeTrelloStage(stageId);
    }

    private Stage enrichStageSurveyStatus(Stage stage) {
        if (stage != null) {
            stage.setSectionEvaluationOuverte(isSectionEvaluationOuverte(stage));
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

    private void notifierValidationSujet(Stage stage, boolean valide) {
        if (stage == null || stage.getId() == null) {
            return;
        }
        String type = valide ? "VALIDATION_SUJET" : "REFUS_SUJET";
        String titre = valide ? "Sujet de stage valide" : "Sujet de stage refuse";
        String message = valide
                ? "Le sujet de votre stage a ete valide par l'encadrant academique."
                : "Le sujet de votre stage a ete refuse par l'encadrant academique.";

        if (stage.getStagiaire() != null && stage.getStagiaire().getId() != null) {
            notificationService.creerNotification(stage.getStagiaire().getId(), titre, message, type, stage.getId(), "STAGE");
        }
        utilisateurRepository.findByRole(Role.RESPONSABLE_STAGE).forEach(u ->
                notificationService.creerNotification(u.getId(), titre,
                        "Le sujet du stage #" + stage.getId() + " a ete " + (valide ? "valide" : "refuse") + ".",
                        type, stage.getId(), "STAGE")
        );
    }

    private void notifierChangementStatut(Stage stage, StatutStage ancienStatut, StatutStage nouveauStatut) {
        if (stage == null || stage.getId() == null || ancienStatut == nouveauStatut) {
            return;
        }
        String message = "Le statut du stage est passe de " + ancienStatut.name() + " a " + nouveauStatut.name() + ".";

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
        utilisateurRepository.findByRole(Role.RESPONSABLE_STAGE).forEach(u -> destinataires.add(u.getId()));

        for (Long destinataireId : destinataires) {
            notificationService.creerNotification(destinataireId, "Changement de statut du stage", message,
                    "CHANGEMENT_STATUT_STAGE", stage.getId(), "STAGE");
        }
    }

    private void ensureNoActiveStageForStagiaire(Stagiaire stagiaire, OffreStage offre) {
        if (stagiaire == null || stagiaire.getId() == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        boolean hasStageActiveToday = stageRepository.findByStagiaireId(stagiaire.getId()).stream()
                .filter(stage -> stage.getStatut() != StatutStage.ANNULE && stage.getStatut() != StatutStage.REFUSE)
                .anyMatch(stage -> isDateWithinStagePeriod(today, stage));
        if (hasStageActiveToday) {
            throw new IllegalStateException(
                    "Cet etudiant ne peut pas etre affecte a un nouveau stage car il est deja dans une periode de stage active."
            );
        }

    }

    private boolean isDateWithinStagePeriod(LocalDate date, Stage stage) {
        if (date == null || stage == null || stage.getDateDebut() == null) {
            return false;
        }
        LocalDate stageEnd = stage.getDateFin() != null
                ? stage.getDateFin()
                : calculerDateFin(stage.getDateDebut(), stage.getDuree());
        if (stageEnd == null) {
            return false;
        }
        return !date.isBefore(stage.getDateDebut()) && !date.isAfter(stageEnd);
    }

}

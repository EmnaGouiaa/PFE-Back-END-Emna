package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.EnqueteSatisfactionDto;
import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleDto;
import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleFormulairesUpdateRequest;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.EnqueteSatisfaction;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutEnqueteSatisfaction;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.ReunionFinaleMapper;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.EnqueteSatisfactionRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReunionFinaleServiceImpl implements ReunionFinaleService {

    private static final Duration MIN_UPDATE_DELAY = Duration.ofHours(24);
    private static final int DUREE_VISIBILITE_ENQUETE_JOURS = 7;
    private static final String STATUT_DISPONIBLE = "Disponible";
    private static final String STATUT_NON_DISPONIBLE = "Non disponible";
    private static final String MESSAGE_INDISPONIBLE = "Aucune enquête de satisfaction disponible pour le moment.";
    private static final String MESSAGE_URL_INVALIDE = "Le lien de l’enquête est indisponible.";
    private static final String TITRE_ENQUETE_PAR_DEFAUT = "Enquête de satisfaction";
    private static final String DESCRIPTION_ENQUETE_PAR_DEFAUT = "Veuillez répondre à l’enquête de satisfaction liée à votre réunion finale.";

    private final ReunionFinaleRepository reunionFinaleRepository;
    private final StageRepository stageRepository;
    private final CahierStageRepository cahierStageRepository;
    private final EnqueteSatisfactionRepository enqueteSatisfactionRepository;
    private final ReunionFinaleMapper reunionFinaleMapper;
    private final NotificationService notificationService;
    private final JwtService jwtService;

    @Override
    public ReunionFinaleDto create(ReunionFinaleDto dto) {
        throw new AccessDeniedException("La reunion finale est creee automatiquement par le systeme.");
    }

    @Override
    public ReunionFinaleDto getById(Long id) {
        ReunionFinale entity = reunionFinaleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reunion finale introuvable avec l'id : " + id));

        authorizeStageAccess(entity.getStage());
        log.info("Consultation de la reunion finale id={}", id);
        return toDtoForCurrentUser(entity);
    }

    @Override
    public List<ReunionFinaleDto> getAll() {
        List<ReunionFinaleDto> reunions = reunionFinaleRepository.findAll()
                .stream()
                .map(reunionFinaleMapper::toDto)
                .toList();

        log.info("{} reunion(s) finale(s) chargee(s) pour la gestion des formulaires", reunions.size());
        return reunions;
    }

    @Override
    public List<ReunionFinaleDto> getByStageId(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + stageId));

        authorizeStageAccess(stage);

        List<ReunionFinaleDto> reunions = reunionFinaleRepository.findByStageId(stageId)
                .stream()
                .map(this::toDtoForCurrentUser)
                .toList();

        log.info("{} reunion(s) finale(s) chargee(s) pour le stage {}", reunions.size(), stageId);
        return reunions;
    }

    @Override
    public ReunionFinaleDto update(Long id, ReunionFinaleDto dto) {
        ReunionFinale entity = reunionFinaleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reunion finale introuvable avec l'id : " + id));

        authorizeFinalMeetingManagement(entity.getStage());

        if (entity.getFicheEvaluation() != null
                && entity.getFicheEvaluation().stream().anyMatch(FicheEvaluation::estVerrouillee)) {
            throw new RuntimeException("Impossible de modifier la reunion finale : au moins une fiche d'evaluation est verrouillee");
        }

        Stage stage = entity.getStage();
        if (stage == null) {
            throw new RuntimeException("Aucun stage n'est associe a cette reunion finale");
        }

        if (stage.getEncadrantProfessionnel() == null) {
            throw new RuntimeException("Le stage ne possede pas d'encadrant professionnel");
        }

        if (stage.getTuteurEntreprise() == null) {
            throw new RuntimeException("Le stage ne possede pas de representant de l'entreprise");
        }

        if (dto != null) {
            if (dto.getStageId() != null && !dto.getStageId().equals(stage.getId())) {
                throw new AccessDeniedException("Le stage associe a une reunion finale automatique ne peut pas etre modifie.");
            }
            if (dto.getDate() != null && !dto.getDate().equals(entity.getDate())) {
                throw new AccessDeniedException("La date de la reunion finale est geree automatiquement par le systeme.");
            }
            if (dto.getHeure() != null && !dto.getHeure().equals(entity.getHeure())) {
                throw new AccessDeniedException("L'heure de la reunion finale est geree automatiquement par le systeme.");
            }
            if (dto.getNumReunion() != null && !dto.getNumReunion().equals(entity.getNumReunion())) {
                throw new AccessDeniedException("Le numero de la reunion finale est gere automatiquement par le systeme.");
            }
        }

        entity.setObservation(dto != null ? dto.getObservation() : entity.getObservation());
        entity.setCompteRendu(dto != null ? dto.getCompteRendu() : entity.getCompteRendu());
        entity.setNote(dto != null ? dto.getNote() : entity.getNote());
        entity.setUrlFormEvaluation(normalizeUrl(dto != null ? dto.getUrlFormEvaluation() : entity.getUrlFormEvaluation(), "Le lien du formulaire d'evaluation est invalide"));
        entity.setUrlFormSatisfaction(normalizeUrl(dto != null ? dto.getUrlFormSatisfaction() : entity.getUrlFormSatisfaction(), "Le lien du formulaire de satisfaction est invalide"));
        entity.setTitreEnqueteSatisfaction(normalizeText(dto != null ? dto.getTitreEnqueteSatisfaction() : entity.getTitreEnqueteSatisfaction()));
        entity.setDescriptionEnqueteSatisfaction(normalizeText(dto != null ? dto.getDescriptionEnqueteSatisfaction() : entity.getDescriptionEnqueteSatisfaction()));
        entity.setCahierStage(resolveCahierStage(stage));

        ReunionFinale updated = reunionFinaleRepository.save(entity);
        log.info("Reunion finale mise a jour. id={}", updated.getId());
        return reunionFinaleMapper.toDto(updated);
    }

    @Override
    public ReunionFinaleDto updateFormulaires(Long id, ReunionFinaleFormulairesUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Les donnees des formulaires sont obligatoires");
        }

        ReunionFinale entity = reunionFinaleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reunion finale introuvable avec l'id : " + id));

        entity.setUrlFormEvaluation(normalizeUrl(request.getUrlFormEvaluation(), "Le lien du formulaire d'evaluation est invalide"));
        entity.setUrlFormSatisfaction(normalizeUrl(request.getUrlFormSatisfaction(), "Le lien du formulaire de satisfaction est invalide"));
        entity.setTitreEnqueteSatisfaction(normalizeText(request.getTitreEnqueteSatisfaction()));
        entity.setDescriptionEnqueteSatisfaction(normalizeText(request.getDescriptionEnqueteSatisfaction()));

        ReunionFinale updated = reunionFinaleRepository.save(entity);
        log.info(
                "Formulaires externes de la reunion finale {} mis a jour. evaluationDefini={}, satisfactionDefini={}",
                updated.getId(),
                updated.getUrlFormEvaluation() != null,
                updated.getUrlFormSatisfaction() != null
        );

        return reunionFinaleMapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public EnqueteSatisfactionDto getEnqueteSatisfaction(Long reunionFinaleId) {
        ReunionFinale reunionFinale = reunionFinaleRepository.findById(reunionFinaleId)
                .orElseThrow(() -> new RuntimeException("Réunion finale introuvable avec l'id : " + reunionFinaleId));

        Utilisateur utilisateur = authorizeSatisfactionActorForStage(reunionFinale.getStage());
        return buildEnqueteSatisfactionDto(reunionFinale, utilisateur);
    }

    @Override
    public void delete(Long id) {
        throw new AccessDeniedException("La suppression manuelle de la reunion finale est interdite.");
    }

    private void validateFinalMeetingDate(Stage stage, java.time.LocalDate meetingDate) {
        if (stage.getDateFin() == null || meetingDate == null || !meetingDate.equals(stage.getDateFin())) {
            throw new RuntimeException("La réunion finale doit être planifiée le dernier jour du stage.");
        }
    }

    private void ensureFinalMeetingCanBeModified(LocalDate date, java.time.LocalTime heure) {
        if (date == null || heure == null) {
            throw new RuntimeException("La date et l'heure de la reunion finale sont obligatoires.");
        }

        LocalDateTime meetingStart = LocalDateTime.of(date, heure);
        if (Duration.between(LocalDateTime.now(), meetingStart).compareTo(MIN_UPDATE_DELAY) < 0) {
            throw new RuntimeException("Vous ne pouvez pas modifier une reunion moins de 24 heures avant son horaire.");
        }
    }

    private void ensureFinalMeetingCanBeCancelled(LocalDate date, java.time.LocalTime heure) {
        if (date == null || heure == null) {
            throw new RuntimeException("La date et l'heure de la reunion finale sont obligatoires.");
        }

        LocalDateTime meetingStart = LocalDateTime.of(date, heure);
        if (Duration.between(LocalDateTime.now(), meetingStart).compareTo(MIN_UPDATE_DELAY) < 0) {
            throw new RuntimeException("Vous ne pouvez pas annuler une reunion moins de 24 heures avant son horaire.");
        }
    }

    private ReunionFinaleDto toDtoForCurrentUser(ReunionFinale entity) {
        ReunionFinaleDto dto = reunionFinaleMapper.toDto(entity);
        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur().orElse(null);
        if (!isEvaluationSectionOpen(entity)) {
            dto.setUrlFormEvaluation(null);
        }
        if (utilisateur != null && isSatisfactionActorRole(utilisateur.getRole()) && !isSatisfactionAvailableToday(entity)) {
            dto.setUrlFormSatisfaction(null);
        }
        return dto;
    }

    private EnqueteSatisfactionDto buildEnqueteSatisfactionDto(ReunionFinale reunionFinale, Utilisateur utilisateur) {
        EnqueteSatisfactionDto dto = new EnqueteSatisfactionDto();
        dto.setReunionFinaleId(reunionFinale.getId());

        Stage stage = reunionFinale.getStage();
        if (stage != null) {
            dto.setStageId(stage.getId());
            dto.setStageTitre(stage.getTitre());
        }

        dto.setDateAtteinte(isSatisfactionDateReached(reunionFinale));
        dto.setSectionEnqueteOuverte(isSatisfactionSectionOpen(reunionFinale));
        dto.setTitre(resolveSurveyTitle(reunionFinale));
        dto.setDescription(resolveSurveyDescription(reunionFinale));

        if (stage == null || stage.getId() == null || utilisateur == null || utilisateur.getId() == null) {
            dto.setDisponible(false);
            dto.setStatut(STATUT_NON_DISPONIBLE);
            dto.setMessage(MESSAGE_INDISPONIBLE);
            return dto;
        }

        EnqueteSatisfaction enquete = enqueteSatisfactionRepository.findByStageIdAndUtilisateurId(stage.getId(), utilisateur.getId())
                .orElse(null);

        if (enquete == null) {
            dto.setDisponible(false);
            dto.setStatut(STATUT_NON_DISPONIBLE);
            dto.setMessage(MESSAGE_INDISPONIBLE);
            return dto;
        }

        dto.setEnqueteId(enquete.getId());
        dto.setDisponible(enquete.getStatutEnquete() == StatutEnqueteSatisfaction.EN_ATTENTE);
        dto.setStatut(enquete.getStatutEnquete() == StatutEnqueteSatisfaction.REMPLIE ? "REMPLIE" : "EN_ATTENTE");
        dto.setMessage(enquete.getStatutEnquete() == StatutEnqueteSatisfaction.REMPLIE
                ? "Enquete deja remplie."
                : "Enquete de satisfaction a remplir dans l'application.");
        return dto;
    }

    private boolean isSatisfactionAvailableToday(ReunionFinale reunionFinale) {
        return isSatisfactionDateReached(reunionFinale)
                && !isSatisfactionExpired(reunionFinale)
                && isValidHttpUrl(reunionFinale.getUrlFormSatisfaction());
    }

    private boolean isEvaluationSectionOpen(ReunionFinale reunionFinale) {
        Stage stage = reunionFinale != null ? reunionFinale.getStage() : null;
        if (stage == null) {
            return false;
        }

        if (stage.getSectionEvaluationOuverte() != null) {
            return Boolean.TRUE.equals(stage.getSectionEvaluationOuverte());
        }

        return stage.getDateFin() != null && !stage.getDateFin().isAfter(LocalDate.now());
    }

    private boolean isSatisfactionSectionOpen(ReunionFinale reunionFinale) {
        Stage stage = reunionFinale != null ? reunionFinale.getStage() : null;
        if (stage != null && stage.getSectionEnqueteOuverte() != null) {
            return Boolean.TRUE.equals(stage.getSectionEnqueteOuverte());
        }
        return isSatisfactionDateReached(reunionFinale);
    }

    private boolean isSatisfactionDateReached(ReunionFinale reunionFinale) {
        Stage stage = reunionFinale != null ? reunionFinale.getStage() : null;
        if (stage != null && stage.getDateFin() != null) {
            return !stage.getDateFin().isAfter(LocalDate.now());
        }
        return reunionFinale.getDate() != null && !reunionFinale.getDate().isAfter(LocalDate.now());
    }

    private boolean isSatisfactionExpired(ReunionFinale reunionFinale) {
        return reunionFinale.getDate() != null
                && reunionFinale.getDate().plusDays(DUREE_VISIBILITE_ENQUETE_JOURS).isBefore(LocalDate.now());
    }

    private String resolveSurveyTitle(ReunionFinale reunionFinale) {
        String title = normalizeText(reunionFinale.getTitreEnqueteSatisfaction());
        return title != null ? title : TITRE_ENQUETE_PAR_DEFAUT;
    }

    private String resolveSurveyDescription(ReunionFinale reunionFinale) {
        String description = normalizeText(reunionFinale.getDescriptionEnqueteSatisfaction());
        return description != null ? description : DESCRIPTION_ENQUETE_PAR_DEFAUT;
    }

    private String normalizeUrl(String rawUrl, String invalidMessage) {
        if (rawUrl == null) {
            return null;
        }

        String normalized = rawUrl.trim();
        if (normalized.isEmpty() || "string".equalsIgnoreCase(normalized)) {
            return null;
        }

        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException(invalidMessage);
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException(invalidMessage);
            }
            return normalized;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(invalidMessage);
        }
    }

    private boolean isValidHttpUrl(String rawUrl) {
        String normalized = normalizeText(rawUrl);
        if (normalized == null) {
            return false;
        }

        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            return scheme != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty() || "string".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private CahierStage resolveCahierStage(Stage stage) {
        if (stage == null || stage.getId() == null) {
            return null;
        }

        if (stage.getCahierStage() != null) {
            return stage.getCahierStage();
        }

        return cahierStageRepository.findByStageId(stage.getId())
                .map(cahierStage -> {
                    stage.setCahierStage(cahierStage);
                    return cahierStage;
                })
                .orElse(null);
    }

    private void notifierStagiaireReunionFinale(ReunionFinale reunionFinale) {
        Stage stage = reunionFinale.getStage();
        if (stage == null || stage.getStagiaire() == null || stage.getStagiaire().getId() == null) {
            return;
        }

        String nomEncadrant = stage.getEncadrantProfessionnel() == null
                ? "Non renseigné"
                : ((stage.getEncadrantProfessionnel().getPrenom() != null ? stage.getEncadrantProfessionnel().getPrenom() : "")
                + " "
                + (stage.getEncadrantProfessionnel().getNom() != null ? stage.getEncadrantProfessionnel().getNom() : "")).trim();

        notificationService.notifierStagiaireReunionFixee(
                stage.getStagiaire().getId(),
                reunionFinale.getId(),
                "Une nouvelle réunion a été planifiée avec votre encadrant professionnel "
                        + (nomEncadrant.isBlank() ? "Non renseigné" : nomEncadrant)
                        + " le " + reunionFinale.getDate() + " à " + reunionFinale.getHeure() + "."
        );
    }

    private void notifierStagiaireReunionFinaleModifiee(ReunionFinale reunionFinale) {
        Stage stage = reunionFinale.getStage();
        if (stage == null || stage.getStagiaire() == null || stage.getStagiaire().getId() == null) {
            return;
        }

        notificationService.creerNotification(
                stage.getStagiaire().getId(),
                "Réunion finale modifiée",
                "Votre réunion finale a été modifiée. Nouvelle date : "
                        + reunionFinale.getDate() + " à " + reunionFinale.getHeure() + ".",
                "REUNION_FIXEE",
                reunionFinale.getId(),
                "REUNION_FINALE"
        );
    }

    private void notifierStagiaireReunionFinaleAnnulee(ReunionFinale reunionFinale) {
        Stage stage = reunionFinale.getStage();
        if (stage == null || stage.getStagiaire() == null || stage.getStagiaire().getId() == null) {
            return;
        }

        notificationService.creerNotification(
                stage.getStagiaire().getId(),
                "Réunion finale annulée",
                "Votre réunion finale prévue le " + reunionFinale.getDate() + " à " + reunionFinale.getHeure()
                        + " a été annulée.",
                "REUNION_FIXEE",
                reunionFinale.getId(),
                "REUNION_FINALE"
        );
    }

    private void authorizeFinalMeetingManagement(Stage stage) {
        if (stage == null || stage.getId() == null) {
            throw new AccessDeniedException("Aucun stage n'est associe a cette reunion finale.");
        }

        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));

        boolean academicSupervisor = utilisateur.getRole() == Role.ENCADRANT_ACADEMIQUE
                && stage.getEncadrantAcademique() != null
                && utilisateur.getId().equals(stage.getEncadrantAcademique().getId());
        boolean professionalSupervisor = utilisateur.getRole() == Role.ENCADRANT_PROFESSIONNEL
                && stage.getEncadrantProfessionnel() != null
                && utilisateur.getId().equals(stage.getEncadrantProfessionnel().getId());

        if (!academicSupervisor && !professionalSupervisor) {
            throw new AccessDeniedException("Seul un encadrant associe au stage peut gerer cette reunion finale.");
        }
    }

    private void authorizeStageAccess(Stage stage) {
        if (stage == null || stage.getId() == null) {
            throw new AccessDeniedException("Aucun stage n'est associe a cette reunion finale.");
        }

        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));

        if (isManagementRole(utilisateur.getRole())) {
            return;
        }

        if (!isConcernedActor(stage, utilisateur)) {
            throw new AccessDeniedException("Accès refusé à une réunion finale qui n'est pas liée à votre stage.");
        }
    }

    private Utilisateur authorizeSatisfactionActorForStage(Stage stage) {
        if (stage == null || stage.getId() == null) {
            throw new AccessDeniedException("Aucun stage n'est associé à cette réunion finale.");
        }

        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifié introuvable."));

        if (!isSatisfactionActorRole(utilisateur.getRole()) || !isConcernedActor(stage, utilisateur)) {
            throw new AccessDeniedException("Accès refusé à l’enquête de satisfaction de ce stage.");
        }

        return utilisateur;
    }

    private boolean isConcernedActor(Stage stage, Utilisateur utilisateur) {
        if (utilisateur.getRole() == Role.STAGIAIRE) {
            return isSameUser(stage.getStagiaire(), utilisateur);
        }

        if (utilisateur.getRole() == Role.ENCADRANT_ACADEMIQUE) {
            return isSameUser(stage.getEncadrantAcademique(), utilisateur);
        }

        if (utilisateur.getRole() == Role.ENCADRANT_PROFESSIONNEL) {
            return isSameUser(stage.getEncadrantProfessionnel(), utilisateur);
        }

        if (utilisateur.getRole() == Role.RESPONSABLE_ENTREPRISE) {
            return isSameUser(stage.getTuteurEntreprise(), utilisateur);
        }

        return false;
    }

    private boolean isSameUser(Utilisateur expected, Utilisateur actual) {
        return expected != null
                && expected.getId() != null
                && actual != null
                && expected.getId().equals(actual.getId());
    }

    private boolean isSatisfactionActorRole(Role role) {
        return role == Role.STAGIAIRE
                || role == Role.ENCADRANT_ACADEMIQUE
                || role == Role.ENCADRANT_PROFESSIONNEL
                || role == Role.RESPONSABLE_ENTREPRISE;
    }

    private boolean isManagementRole(Role role) {
        return role == Role.ADMINISTRATEUR
                || role == Role.RESPONSABLE_SERVICE_STAGES
                || role == Role.RESPONSABLE_UNIVERSITAIRE_STAGES;
    }
}

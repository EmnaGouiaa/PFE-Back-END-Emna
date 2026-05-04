package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.EnqueteSatisfactionDto;
import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleDto;
import fsegs.pfebackendemnagouuiaa.dto.ReunionFinaleFormulairesUpdateRequest;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.mapper.ReunionFinaleMapper;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReunionFinaleServiceImpl implements ReunionFinaleService {

    private static final String STATUT_DISPONIBLE = "Disponible";
    private static final String STATUT_NON_DISPONIBLE = "Non disponible";
    private static final String MESSAGE_INDISPONIBLE = "Aucune enquête de satisfaction disponible pour le moment.";
    private static final String MESSAGE_URL_INVALIDE = "Le lien de l’enquête est indisponible.";
    private static final String TITRE_ENQUETE_PAR_DEFAUT = "Enquête de satisfaction";
    private static final String DESCRIPTION_ENQUETE_PAR_DEFAUT = "Veuillez répondre à l’enquête de satisfaction liée à votre réunion finale.";

    private final ReunionFinaleRepository reunionFinaleRepository;
    private final StageRepository stageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ReunionFinaleMapper reunionFinaleMapper;
    private final JwtService jwtService;

    @Override
    public ReunionFinaleDto create(ReunionFinaleDto dto) {
        if (dto == null) {
            throw new RuntimeException("Les donnees de la reunion finale sont obligatoires");
        }

        if (dto.getStageId() == null) {
            throw new RuntimeException("Le stage est obligatoire");
        }

        if (dto.getDate() == null) {
            throw new RuntimeException("La date est obligatoire");
        }

        if (dto.getHeure() == null) {
            throw new RuntimeException("L'heure est obligatoire");
        }

        Stage stage = stageRepository.findById(dto.getStageId())
                .orElseThrow(() -> new RuntimeException("Stage introuvable avec l'id : " + dto.getStageId()));

        if (stage.getStatut() == null || !stage.getStatut().name().equals("TERMINE")) {
            throw new RuntimeException("La reunion finale ne peut etre planifiee que pour un stage termine");
        }

        if (stage.getEncadrantProfessionnel() == null) {
            throw new RuntimeException("Le stage ne possede pas d'encadrant professionnel");
        }

        if (stage.getTuteurEntreprise() == null) {
            throw new RuntimeException("Le stage ne possede pas de representant de l'entreprise");
        }

        validateFinalMeetingDate(stage, dto.getDate());

        boolean reunionExiste = reunionFinaleRepository.findByStageId(dto.getStageId())
                .stream()
                .findAny()
                .isPresent();

        if (reunionExiste) {
            throw new RuntimeException("Une reunion finale existe deja pour ce stage");
        }

        ReunionFinale entity = new ReunionFinale();
        entity.setNumReunion(dto.getNumReunion());
        entity.setDate(dto.getDate());
        entity.setHeure(dto.getHeure());
        entity.setObservation(dto.getObservation());
        entity.setCompteRendu(dto.getCompteRendu());
        entity.setNote(dto.getNote());
        entity.setUrlFormEvaluation(normalizeUrl(dto.getUrlFormEvaluation(), "Le lien du formulaire d'evaluation est invalide"));
        entity.setUrlFormSatisfaction(normalizeUrl(dto.getUrlFormSatisfaction(), "Le lien du formulaire de satisfaction est invalide"));
        entity.setTitreEnqueteSatisfaction(normalizeText(dto.getTitreEnqueteSatisfaction()));
        entity.setDescriptionEnqueteSatisfaction(normalizeText(dto.getDescriptionEnqueteSatisfaction()));
        entity.setStage(stage);
        entity.setParticipants(new HashSet<>());

        ReunionFinale saved = reunionFinaleRepository.save(entity);
        log.info("Reunion finale creee avec succes. id={}, stageId={}", saved.getId(), stage.getId());
        return reunionFinaleMapper.toDto(saved);
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

        if (dto.getStageId() != null && !dto.getStageId().equals(stage.getId())) {
            throw new RuntimeException("Le stage d'une reunion finale ne peut pas etre modifie");
        }

        entity.setNumReunion(dto.getNumReunion());
        entity.setDate(dto.getDate());
        entity.setHeure(dto.getHeure());
        validateFinalMeetingDate(stage, entity.getDate());
        entity.setObservation(dto.getObservation());
        entity.setCompteRendu(dto.getCompteRendu());
        entity.setNote(dto.getNote());
        entity.setUrlFormEvaluation(normalizeUrl(dto.getUrlFormEvaluation(), "Le lien du formulaire d'evaluation est invalide"));
        entity.setUrlFormSatisfaction(normalizeUrl(dto.getUrlFormSatisfaction(), "Le lien du formulaire de satisfaction est invalide"));
        entity.setTitreEnqueteSatisfaction(normalizeText(dto.getTitreEnqueteSatisfaction()));
        entity.setDescriptionEnqueteSatisfaction(normalizeText(dto.getDescriptionEnqueteSatisfaction()));

        Set<Utilisateur> participants = new HashSet<>();
        if (dto.getParticipantIds() != null) {
            participants.addAll(utilisateurRepository.findAllById(dto.getParticipantIds()));
        } else if (entity.getParticipants() != null) {
            participants.addAll(entity.getParticipants());
        }

        participants.add(stage.getEncadrantProfessionnel());
        participants.add(stage.getTuteurEntreprise());
        entity.setParticipants(participants);

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
        if (!reunionFinaleRepository.existsById(id)) {
            throw new RuntimeException("Reunion finale introuvable avec l'id : " + id);
        }

        throw new AccessDeniedException("La suppression d'une reunion de suivi est interdite.");
    }

    private void validateFinalMeetingDate(Stage stage, java.time.LocalDate meetingDate) {
        if (stage.getDateFin() == null || meetingDate == null || !meetingDate.equals(stage.getDateFin())) {
            throw new RuntimeException("La réunion finale doit être planifiée le dernier jour du stage.");
        }
    }

    private ReunionFinaleDto toDtoForCurrentUser(ReunionFinale entity) {
        ReunionFinaleDto dto = reunionFinaleMapper.toDto(entity);
        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur().orElse(null);
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

        boolean dateAtteinte = isSatisfactionDateReached(reunionFinale);
        boolean hasUrl = normalizeText(reunionFinale.getUrlFormSatisfaction()) != null;
        boolean validUrl = hasUrl && isValidHttpUrl(reunionFinale.getUrlFormSatisfaction());
        boolean disponible = dateAtteinte && hasUrl && validUrl;
        dto.setDateAtteinte(dateAtteinte);

        if (!dateAtteinte) {
            dto.setDisponible(false);
            dto.setStatut(STATUT_NON_DISPONIBLE);
            dto.setMessage(MESSAGE_INDISPONIBLE);
            return dto;
        }

        if (!hasUrl) {
            dto.setDisponible(false);
            dto.setStatut(STATUT_NON_DISPONIBLE);
            dto.setMessage(MESSAGE_URL_INVALIDE);
            return dto;
        }

        if (!validUrl) {
            dto.setDisponible(false);
            dto.setStatut(STATUT_NON_DISPONIBLE);
            dto.setMessage(MESSAGE_URL_INVALIDE);
            return dto;
        }

        dto.setTitre(resolveSurveyTitle(reunionFinale));
        dto.setDescription(resolveSurveyDescription(reunionFinale));
        dto.setDisponible(true);
        dto.setUrlFormulaire(reunionFinale.getUrlFormSatisfaction().trim());
        dto.setStatut(STATUT_DISPONIBLE);
        return dto;
    }

    private boolean isSatisfactionAvailableToday(ReunionFinale reunionFinale) {
        return isSatisfactionDateReached(reunionFinale) && isValidHttpUrl(reunionFinale.getUrlFormSatisfaction());
    }

    private boolean isSatisfactionDateReached(ReunionFinale reunionFinale) {
        return reunionFinale.getDate() != null && reunionFinale.getDate().equals(LocalDate.now());
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
        if (normalized.isEmpty()) {
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
        return normalized.isEmpty() ? null : normalized;
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

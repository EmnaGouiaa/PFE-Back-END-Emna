package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CreateEnqueteSatisfactionRequest;
import fsegs.pfebackendemnagouuiaa.dto.EnqueteSatisfactionResponse;
import fsegs.pfebackendemnagouuiaa.dto.RemplirEnqueteSatisfactionRequest;
import fsegs.pfebackendemnagouuiaa.entities.EnqueteSatisfaction;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutEnqueteSatisfaction;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.EnqueteSatisfactionRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EnqueteSatisfactionServiceImpl implements EnqueteSatisfactionService {

    private static final Set<Role> MANAGEMENT_ROLES = Set.of(
            Role.ADMINISTRATEUR,
            Role.RESPONSABLE_SERVICE_STAGES,
            Role.RESPONSABLE_UNIVERSITAIRE_STAGES
    );
    private static final String DEFAULT_SURVEY_TITLE = EnqueteSatisfaction.TITRE_PAR_DEFAUT;
    private static final String DEFAULT_SURVEY_DESCRIPTION = EnqueteSatisfaction.DESCRIPTION_PAR_DEFAUT;
    private static final String DEFAULT_SURVEY_URL = EnqueteSatisfaction.URL_FORMULAIRE_PAR_DEFAUT;

    private final EnqueteSatisfactionRepository enqueteSatisfactionRepository;
    private final StageRepository stageRepository;
    private final ReunionFinaleRepository reunionFinaleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public List<EnqueteSatisfactionResponse> getEnquetesByStage(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable."));

        authorizeStageVisibility(stage);

        return enqueteSatisfactionRepository.findByStageIdOrderByDateCreationAsc(stageId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnqueteSatisfactionResponse> getEnquetesByUtilisateur(Long utilisateurId) {
        Utilisateur connectedUser = getAuthenticatedUtilisateur();
        if (!connectedUser.getId().equals(utilisateurId) && !isManagementRole(connectedUser.getRole())) {
            throw new AccessDeniedException("Acces refuse aux enquetes de cet utilisateur.");
        }

        return enqueteSatisfactionRepository.findByUtilisateurIdOrderByDateCreationDesc(utilisateurId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public EnqueteSatisfactionResponse remplirEnquete(Long enqueteId, RemplirEnqueteSatisfactionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Les donnees de reponse sont obligatoires.");
        }

        Utilisateur connectedUser = getAuthenticatedUtilisateur();
        EnqueteSatisfaction enquete = enqueteSatisfactionRepository.findById(enqueteId)
                .orElseThrow(() -> new EntityNotFoundException("Enquete de satisfaction introuvable."));

        if (enquete.getUtilisateur() == null || !connectedUser.getId().equals(enquete.getUtilisateur().getId())) {
            throw new AccessDeniedException("Vous n'etes pas autorise a remplir cette enquete.");
        }

        if (enquete.getStatutEnquete() == StatutEnqueteSatisfaction.REMPLIE) {
            throw new IllegalArgumentException("Cette enquete a deja ete remplie.");
        }

        if (!isSectionEnqueteOuverte(enquete.getStage())) {
            throw new AccessDeniedException("L'enquete de satisfaction sera accessible a partir du dernier jour du stage.");
        }

        enquete.setReponses(normalizeNullableText(request.getReponses()));
        enquete.setCommentaireGlobal(normalizeNullableText(request.getCommentaireGlobal()));
        enquete.setStatutEnquete(StatutEnqueteSatisfaction.REMPLIE);
        enquete.setDateSoumission(LocalDateTime.now());

        return toResponse(enqueteSatisfactionRepository.save(enquete));
    }

    @Override
    @Transactional
    public List<EnqueteSatisfactionResponse> creerEnquetesPourStageSiNecessaire(Stage stage) {
        if (!isSurveyEligibleStage(stage)) {
            return List.of();
        }

        List<CreateEnqueteSatisfactionRequest> requests = buildSurveyRequests(stage);
        List<EnqueteSatisfactionResponse> createdSurveys = new ArrayList<>();

        for (CreateEnqueteSatisfactionRequest request : requests) {
            if (enqueteSatisfactionRepository.existsByStageIdAndUtilisateurId(
                    request.getStageId(),
                    request.getUtilisateurId()
            )) {
                continue;
            }

            EnqueteSatisfactionResponse created = createPendingSurvey(request);
            createdSurveys.add(created);
        }

        return createdSurveys;
    }

    @Override
    @Transactional
    public EnqueteSatisfactionResponse createPendingSurvey(CreateEnqueteSatisfactionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La creation d'enquete requiert des donnees.");
        }

        Stage stage = stageRepository.findById(request.getStageId())
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable."));
        Utilisateur utilisateur = utilisateurRepository.findById(request.getUtilisateurId())
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable."));

        if (enqueteSatisfactionRepository.existsByStageIdAndUtilisateurId(stage.getId(), utilisateur.getId())) {
            return enqueteSatisfactionRepository.findByStageIdAndUtilisateurId(stage.getId(), utilisateur.getId())
                    .map(this::toResponse)
                    .orElseThrow(() -> new IllegalStateException("La contrainte d'unicite des enquetes est incoherente."));
        }

        EnqueteSatisfaction enquete = new EnqueteSatisfaction();
        enquete.setStage(stage);
        enquete.setUtilisateur(utilisateur);
        enquete.setRoleRepondant(request.getRoleRepondant() != null ? request.getRoleRepondant() : utilisateur.getRole());
        enquete.setStatutEnquete(StatutEnqueteSatisfaction.EN_ATTENTE);
        enquete.setTitre(resolveSurveyTitle(stage));
        enquete.setDescription(resolveSurveyDescription(stage));
        enquete.setUrlFormulaire(resolveSurveyUrl(stage));
        enquete.setReponses(normalizeNullableText(request.getReponses()));
        enquete.setCommentaireGlobal(normalizeNullableText(request.getCommentaireGlobal()));

        return toResponse(enqueteSatisfactionRepository.save(enquete));
    }

    private List<CreateEnqueteSatisfactionRequest> buildSurveyRequests(Stage stage) {
        Map<Long, CreateEnqueteSatisfactionRequest> requestsByUserId = new LinkedHashMap<>();

        addSurveyRequest(requestsByUserId, stage, stage.getStagiaire(), Role.STAGIAIRE);
        addSurveyRequest(requestsByUserId, stage, stage.getEncadrantAcademique(), Role.ENCADRANT_ACADEMIQUE);
        addSurveyRequest(requestsByUserId, stage, stage.getEncadrantProfessionnel(), Role.ENCADRANT_PROFESSIONNEL);
        addSurveyRequest(requestsByUserId, stage, stage.getTuteurEntreprise(), Role.RESPONSABLE_ENTREPRISE);

        for (Role managementRole : MANAGEMENT_ROLES) {
            utilisateurRepository.findByRole(managementRole)
                    .forEach(utilisateur -> addSurveyRequest(requestsByUserId, stage, utilisateur, managementRole));
        }

        return new ArrayList<>(requestsByUserId.values());
    }

    private void addSurveyRequest(Map<Long, CreateEnqueteSatisfactionRequest> requestsByUserId,
                                  Stage stage,
                                  Utilisateur utilisateur,
                                  Role roleRepondant) {
        if (stage == null || stage.getId() == null || utilisateur == null || utilisateur.getId() == null) {
            return;
        }

        requestsByUserId.putIfAbsent(
                utilisateur.getId(),
                new CreateEnqueteSatisfactionRequest(
                        stage.getId(),
                        utilisateur.getId(),
                        roleRepondant,
                        null,
                        null
                )
        );
    }

    private EnqueteSatisfactionResponse toResponse(EnqueteSatisfaction enquete) {
        Utilisateur connectedUser = resolveAuthenticatedUtilisateurSafely();
        Utilisateur utilisateur = enquete.getUtilisateur();
        Stage stage = enquete.getStage();

        String fullName = utilisateur == null
                ? null
                : (safeText(utilisateur.getPrenom(), "") + " " + safeText(utilisateur.getNom(), "")).trim();

        return new EnqueteSatisfactionResponse(
                enquete.getId(),
                enquete.getDateCreation(),
                enquete.getDateSoumission(),
                enquete.getStatutEnquete(),
                enquete.getTitre(),
                enquete.getDescription(),
                enquete.getUrlFormulaire(),
                enquete.getReponses(),
                enquete.getCommentaireGlobal(),
                enquete.getRoleRepondant(),
                stage != null ? stage.getId() : null,
                stage != null ? stage.getTitre() : null,
                utilisateur != null ? utilisateur.getId() : null,
                fullName == null || fullName.isBlank() ? null : fullName,
                connectedUser != null
                        && utilisateur != null
                        && connectedUser.getId() != null
                        && connectedUser.getId().equals(utilisateur.getId()),
                enquete.getStatutEnquete() == StatutEnqueteSatisfaction.EN_ATTENTE && isSectionEnqueteOuverte(stage),
                isSectionEnqueteOuverte(stage)
        );
    }

    private boolean isSectionEnqueteOuverte(Stage stage) {
        if (stage == null) {
            return false;
        }

        if (stage.getSectionEnqueteOuverte() != null) {
            return Boolean.TRUE.equals(stage.getSectionEnqueteOuverte());
        }

        LocalDate today = LocalDate.now();
        if (stage.getDateFin() != null && !today.isBefore(stage.getDateFin())) {
            return true;
        }

        if (stage.getId() == null) {
            return false;
        }

        return reunionFinaleRepository.findByStageId(stage.getId())
                .stream()
                .map(fsegs.pfebackendemnagouuiaa.entities.ReunionFinale::getDate)
                .filter(Objects::nonNull)
                .anyMatch(date -> !today.isBefore(date));
    }

    private void authorizeStageVisibility(Stage stage) {
        Utilisateur utilisateur = getAuthenticatedUtilisateur();
        if (isManagementRole(utilisateur.getRole())) {
            return;
        }

        Long userId = utilisateur.getId();
        Set<Long> allowedIds = new LinkedHashSet<>();
        if (stage.getStagiaire() != null && stage.getStagiaire().getId() != null) {
            allowedIds.add(stage.getStagiaire().getId());
        }
        if (stage.getEncadrantAcademique() != null && stage.getEncadrantAcademique().getId() != null) {
            allowedIds.add(stage.getEncadrantAcademique().getId());
        }
        if (stage.getEncadrantProfessionnel() != null && stage.getEncadrantProfessionnel().getId() != null) {
            allowedIds.add(stage.getEncadrantProfessionnel().getId());
        }
        if (stage.getTuteurEntreprise() != null && stage.getTuteurEntreprise().getId() != null) {
            allowedIds.add(stage.getTuteurEntreprise().getId());
        }

        if (userId == null || !allowedIds.contains(userId)) {
            throw new AccessDeniedException("Acces refuse aux enquetes de ce stage.");
        }
    }

    private Utilisateur getAuthenticatedUtilisateur() {
        return jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));
    }

    private boolean isManagementRole(Role role) {
        return MANAGEMENT_ROLES.contains(role);
    }

    private boolean isSurveyEligibleStage(Stage stage) {
        return stage != null
                && stage.getId() != null
                && stage.getStatut() != null
                && (stage.getStatut() == StatutStage.EN_COURS || stage.getStatut() == StatutStage.TERMINE);
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String resolveSurveyTitle(Stage stage) {
        ReunionFinale reunionFinale = findReferenceFinalMeeting(stage);
        String configured = reunionFinale != null ? normalizeNullableText(reunionFinale.getTitreEnqueteSatisfaction()) : null;
        return configured != null ? configured : DEFAULT_SURVEY_TITLE;
    }

    private String resolveSurveyDescription(Stage stage) {
        ReunionFinale reunionFinale = findReferenceFinalMeeting(stage);
        String configured = reunionFinale != null ? normalizeNullableText(reunionFinale.getDescriptionEnqueteSatisfaction()) : null;
        return configured != null ? configured : DEFAULT_SURVEY_DESCRIPTION;
    }

    private String resolveSurveyUrl(Stage stage) {
        ReunionFinale reunionFinale = findReferenceFinalMeeting(stage);
        String configured = reunionFinale == null ? null : normalizeNullableText(reunionFinale.getUrlFormSatisfaction());
        return configured != null ? configured : DEFAULT_SURVEY_URL;
    }

    private ReunionFinale findReferenceFinalMeeting(Stage stage) {
        if (stage == null || stage.getId() == null) {
            return null;
        }

        return reunionFinaleRepository.findByStageId(stage.getId())
                .stream()
                .filter(Objects::nonNull)
                .sorted((left, right) -> {
                    if (left.getDate() == null && right.getDate() == null) {
                        return 0;
                    }
                    if (left.getDate() == null) {
                        return 1;
                    }
                    if (right.getDate() == null) {
                        return -1;
                    }
                    return right.getDate().compareTo(left.getDate());
                })
                .findFirst()
                .orElse(null);
    }

    private Utilisateur resolveAuthenticatedUtilisateurSafely() {
        try {
            return jwtService.getAuthenticatedUtilisateur().orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

}

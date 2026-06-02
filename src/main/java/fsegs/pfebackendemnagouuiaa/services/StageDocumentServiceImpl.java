package fsegs.pfebackendemnagouuiaa.services;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import fsegs.pfebackendemnagouuiaa.dto.CahierStageDto;
import fsegs.pfebackendemnagouuiaa.dto.ConventionStageDto;
import fsegs.pfebackendemnagouuiaa.dto.FicheEvaluationDto;
import fsegs.pfebackendemnagouuiaa.dto.DocumentSignatoryStatusDto;
import fsegs.pfebackendemnagouuiaa.dto.StageDocumentActionResponseDto;
import fsegs.pfebackendemnagouuiaa.dto.StageDocumentStatusDto;
import fsegs.pfebackendemnagouuiaa.dto.StageDocumentsOverviewDto;
import fsegs.pfebackendemnagouuiaa.entities.Absence;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.RoleSignature;
import fsegs.pfebackendemnagouuiaa.entities.Signature;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutDocument;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.AbsenceRepository;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ConventionStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.FicheEvaluationRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.repository.UtilisateurRepository;
import fsegs.pfebackendemnagouuiaa.service.LogbookMeetingSupport;
import fsegs.pfebackendemnagouuiaa.services.pdf.InternshipPdfEvaluationFormat;
import fsegs.pfebackendemnagouuiaa.exception.BusinessException;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StageDocumentServiceImpl implements StageDocumentService {

//    1. findById(stageId)
//   → cherche le stage en base de données
//   → si introuvable : exception 404 "Stage introuvable"
//         ↓
//2. authorizeLinkedStageAccess(stage)
//   → récupère l'utilisateur connecté depuis le JWT
//   → vérifie qu'il est bien lié à ce stage
//   → si non autorisé : exception 403 "Accès refusé"
//         ↓
//3. ensureStageEligibleForDocuments(stage)
//   → vérifie que le stage n'est pas REFUSE
//   → si refusé : exception "Documents indisponibles"
    private static final List<RoleSignature> CONVENTION_SIGNATORIES = List.of(
            RoleSignature.STAGIAIRE,
            RoleSignature.ENCADRANT_ACADEMIQUE,
            RoleSignature.ENCADRANT_PROFESSIONNEL,
            RoleSignature.RESPONSABLE_ENTREPRISE,
            RoleSignature.RESPONSABLE_UNIVERSITAIRE
    );

    private static final List<RoleSignature> LOGBOOK_SIGNATORIES = List.of(
            RoleSignature.STAGIAIRE,
            RoleSignature.ENCADRANT_ACADEMIQUE,
            RoleSignature.ENCADRANT_PROFESSIONNEL,
            RoleSignature.RESPONSABLE_ENTREPRISE
    );

    private static final List<RoleSignature> EVALUATION_SIGNATORIES = List.of(
            RoleSignature.ENCADRANT_PROFESSIONNEL,
            RoleSignature.RESPONSABLE_ENTREPRISE
    );

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color PDF_PRIMARY = new DeviceRgb(15, 76, 129);
    private static final Color PDF_PRIMARY_SOFT = new DeviceRgb(232, 241, 249);
    private static final Color PDF_SECONDARY = new DeviceRgb(21, 128, 101);
    private static final Color PDF_SECONDARY_SOFT = new DeviceRgb(236, 253, 245);
    private static final Color PDF_SLATE_SOFT = new DeviceRgb(248, 250, 252);
    private static final Color PDF_BORDER = new DeviceRgb(203, 213, 225);
    private static final Color PDF_TEXT = new DeviceRgb(15, 23, 42);
    private static final Color PDF_MUTED = new DeviceRgb(100, 116, 139);
    private static final Color PDF_PENDING_BG = new DeviceRgb(239, 246, 255);
    private static final Color PDF_PENDING_TEXT = new DeviceRgb(30, 64, 175);
    private static final Color PDF_SIGNED_BG = new DeviceRgb(220, 252, 231);
    private static final Color PDF_SIGNED_TEXT = new DeviceRgb(22, 101, 52);

    private final StageRepository stageRepository;
    private final ConventionStageRepository conventionStageRepository;
    private final FicheEvaluationRepository ficheEvaluationRepository;
    private final CahierStageRepository cahierStageRepository;
    private final ReunionFinaleRepository reunionFinaleRepository;
    private final ReunionRepository reunionRepository;
    private final AbsenceRepository absenceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ConventionStageService conventionStageService;
    private final ConventionStagePdfService conventionStagePdfService;
    private final CahierStagePdfService cahierStagePdfService;
    private final FicheEvaluationPdfService ficheEvaluationPdfService;
    private final FicheEvaluationService ficheEvaluationService;
    private final EvaluationSheetBootstrapService evaluationSheetBootstrapService;
    private final CahierStageService cahierStageService;
    private final TrelloService trelloService;
    private final JwtService jwtService;
    private final FinalStageDocumentPdfAccessService finalStageDocumentPdfAccessService;
    private final ConventionStagePdfAccessService conventionStagePdfAccessService;
    private final SignatureImagePdfHelper signatureImagePdfHelper;

    private record SignaturePdfRow(
            String signer,
            String role,
            boolean signed,
            java.time.temporal.TemporalAccessor signedAt,
            String signatureImage
    ) {}

    private record SignatureRequirement(boolean present, String message) {}

    private record TrelloSnapshot(boolean synchronizedBoard, List<Map<String, Object>> tasks, Map<String, String> listNames) {}

    @Override
    @Transactional
    public List<StageDocumentsOverviewDto> listStageDocuments() {
        return stageRepository.findAll()
                .stream()
                .filter(this::isEligibleForStageDocuments)
                .sorted(Comparator.comparing(Stage::getId).reversed())
                .map(this::toOverview)
                .filter(this::hasVisibleDocuments)
                .toList();
    }

    @Override
    @Transactional
    public StageDocumentsOverviewDto getStageDocuments(Long stageId) {
        return toOverview(getAuthorizedStage(stageId));
    }

    @Override
    @Transactional
    public StageDocumentActionResponseDto generateConventionPdf(Long stageId) {
        Stage stage = getAuthorizedStage(stageId);
        ensureConventionVisible(stage, conventionStageRepository.findByStageId(stageId));
        getOrCreateConventionForProcess(stage);
        return new StageDocumentActionResponseDto(
                "La convention de stage est initialisee.",
                getStageDocuments(stageId)
        );
    }

    @Override
    @Transactional
    public StageDocumentActionResponseDto generateEvaluationPdf(Long stageId) {
        Stage stage = getAuthorizedStage(stageId);
        ensureEvaluationVisible(stage, ficheEvaluationRepository.findFirstByStageId(stageId));
        FicheEvaluation fiche = getOrCreateEvaluationForProcess(stage);
        validateNoBlockingReasons(getEvaluationPdfBlockingReasons(stage, fiche));
        return new StageDocumentActionResponseDto(
                "La fiche d'evaluation finale est prete pour generation.",
                getStageDocuments(stageId)
        );
    }

    @Override
    @Transactional
    public StageDocumentActionResponseDto generateLogbookPdf(Long stageId) {
        Stage stage = getAuthorizedStage(stageId);
        ensureLogbookVisible(stage, cahierStageRepository.findByStageId(stageId));
        CahierStage cahierStage = getOrCreateLogbookForProcess(stage);
        validateNoBlockingReasons(getLogbookPdfBlockingReasons(stage, cahierStage));
        return new StageDocumentActionResponseDto(
                "Le cahier de stage final est pret pour generation.",
                getStageDocuments(stageId)
        );
    }

    @Override
    public byte[] getConventionPdf(Long stageId) {
        Stage stage = getAuthorizedStage(stageId);
        ConventionStage convention = conventionStageRepository.findByStageId(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Convention manquante pour ce stage."));
        ensureConventionVisible(stage, Optional.of(convention));
        validateNoBlockingReasons(getConventionPdfBlockingReasons(stage, convention));

        return generateConventionPdfBytes(stage, convention);
    }

    @Override
    public byte[] getConventionPdfByConventionId(Long conventionId) {
        ConventionStage convention = conventionStageRepository.findById(conventionId)
                .orElseThrow(() -> new EntityNotFoundException("Convention introuvable."));
        if (convention.getStage() == null || convention.getStage().getId() == null) {
            throw new EntityNotFoundException("Stage introuvable pour cette convention.");
        }
        Long stageId = convention.getStage().getId();
        Stage stage = getAuthorizedStage(stageId);
        ensureConventionVisible(stage, Optional.of(convention));
        validateNoBlockingReasons(getConventionPdfBlockingReasons(stage, convention));
        return generateConventionPdfBytes(stage, convention);
    }

    private byte[] generateConventionPdfBytes(Stage stage, ConventionStage convention) {
        try {
            FicheEvaluation evaluation = ficheEvaluationRepository.findFirstByStageId(stage.getId()).orElse(null);
            return conventionStagePdfService.generer(stage, convention, evaluation);
        } catch (java.io.IOException e) {
            throw new BusinessException("Impossible de generer le PDF de la convention.");
        }
    }

    @Override
    public byte[] getEvaluationPdf(Long stageId) {
        Stage stage = getAuthorizedStage(stageId);
        FicheEvaluation fiche = ficheEvaluationRepository.findFirstByStageId(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Fiche d'evaluation manquante pour ce stage."));
        ensureEvaluationVisible(stage, Optional.of(fiche));
        validateNoBlockingReasons(getEvaluationPdfBlockingReasons(stage, fiche));

        try {
            return ficheEvaluationPdfService.generer(stage, fiche);
        } catch (java.io.IOException e) {
            throw new BusinessException("Impossible de generer le PDF de la fiche d'evaluation.");
        }
    }

    @Override
    public byte[] getLogbookPdf(Long stageId) {
        Stage stage = getAuthorizedStage(stageId);
        CahierStage cahierStage = cahierStageRepository.findByStageId(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Cahier de stage manquant pour ce stage."));
        ensureLogbookVisible(stage, Optional.of(cahierStage));
        validateNoBlockingReasons(getLogbookPdfBlockingReasons(stage, cahierStage));

        return createLogbookPdf(stage, cahierStage);
    }

    /**
     * Cree la convention en base si le stage y est eligible et qu'elle n'existe pas encore
     * (aligne l'UI « generation autorisee » avec un document initialisable / signable).
     */
    private Optional<ConventionStage> resolveConventionForDocuments(Stage stage) {
        Optional<ConventionStage> convention = conventionStageRepository.findByStageId(stage.getId());
        if (convention.isPresent() || !isConventionProcessTriggered(stage)) {
            return convention;
        }
        return Optional.of(getOrCreateConventionForProcess(stage));
    }

    private StageDocumentsOverviewDto toOverview(Stage stage) {
        Optional<ConventionStage> convention = resolveConventionForDocuments(stage);
        Optional<FicheEvaluation> evaluation = ficheEvaluationRepository.findFirstByStageId(stage.getId());
        Optional<CahierStage> cahierStage = cahierStageRepository.findByStageId(stage.getId());
        Role role = getCurrentRole();

        return new StageDocumentsOverviewDto(
                stage.getId(),
                safeText(stage.getTitre()),
                stage.getStatut() != null ? stage.getStatut().name() : "-",
                stage.getDateFin(),
                buildFullName(stage.getStagiaire() != null ? stage.getStagiaire().getPrenom() : null, stage.getStagiaire() != null ? stage.getStagiaire().getNom() : null),
                stage.getEntreprise() != null ? safeText(stage.getEntreprise().getNom()) : "-",
                buildFullName(stage.getEncadrantAcademique() != null ? stage.getEncadrantAcademique().getPrenom() : null, stage.getEncadrantAcademique() != null ? stage.getEncadrantAcademique().getNom() : null),
                buildFullName(stage.getEncadrantProfessionnel() != null ? stage.getEncadrantProfessionnel().getPrenom() : null, stage.getEncadrantProfessionnel() != null ? stage.getEncadrantProfessionnel().getNom() : null),
                canSeeConvention(role, convention) ? buildConventionStatus(stage, convention) : null,
                canSeeEvaluation(role, evaluation) ? buildEvaluationStatus(stage, evaluation) : null,
                canSeeLogbook(role, cahierStage) ? buildLogbookStatus(stage, cahierStage) : null
        );
    }

    private boolean hasVisibleDocuments(StageDocumentsOverviewDto overview) {
        return overview.getConvention() != null
                || overview.getFicheEvaluation() != null
                || overview.getCahierStage() != null;
    }

    private StageDocumentStatusDto buildConventionStatus(Stage stage, Optional<ConventionStage> convention) {
        if (convention.isEmpty()) {
            StageDocumentStatusDto missing = missingStatus(
                    "CONVENTION",
                    "Convention de stage",
                    "Convention non generee pour ce stage.",
                    isConventionProcessTriggered(stage)
            );
            applyPendingConventionSignatureFlags(missing);
            return missing;
        }

        ConventionStage item = convention.get();
        List<String> blockers = getConventionPdfBlockingReasons(stage, item);
        StageDocumentStatusDto status = documentStatus(
                "CONVENTION",
                "Convention de stage",
                item.getId(),
                blockers.isEmpty(),
                true,
                isConventionProcessTriggered(stage),
                blockers.isEmpty() ? "Disponible" : "PDF indisponible",
                joinReasons(blockers)
        );
        status.setSigneeParResponsableUniversitaire(item.estSignePar(RoleSignature.RESPONSABLE_UNIVERSITAIRE));
        status.setDateSignatureResponsableUniversitaire(formatDateTime(
                item.getSignaturePour(RoleSignature.RESPONSABLE_UNIVERSITAIRE)
                        .map(Signature::getDateSignature).orElse(null)));
        applyConventionSignatureFlags(status, item);
        return status;
    }

    private StageDocumentStatusDto buildEvaluationStatus(Stage stage, Optional<FicheEvaluation> evaluation) {
        if (evaluation.isPresent()) {
            List<String> blockers = getEvaluationPdfBlockingReasons(stage, evaluation.get());
            FicheEvaluation fiche = evaluation.get();
            String libelle = blockers.isEmpty() ? "Disponible" : "PDF indisponible";
            StageDocumentStatusDto status = documentStatus(
                    "FICHE_EVALUATION",
                    "Fiche d'evaluation",
                    fiche.getId(),
                    blockers.isEmpty(),
                    true,
                    blockers.isEmpty(),
                    libelle,
                    joinReasons(blockers)
            );
            applyEvaluationSignatureFlags(status, fiche);
            return status;
        }

        String reason = "Fiche d'evaluation non generee pour ce stage.";
        if (!EvaluationStageAccessRules.isEvaluationPeriodOpen(stage)) {
            reason = EvaluationStageAccessRules.UNAVAILABLE_MESSAGE;
        }

        StageDocumentStatusDto missing = missingStatus(
                "FICHE_EVALUATION",
                "Fiche d'evaluation",
                reason,
                canCreateEvaluationDraft(stage)
        );
        applyPendingEvaluationSignatureFlags(missing);
        return missing;
    }

    private StageDocumentStatusDto buildLogbookStatus(Stage stage, Optional<CahierStage> cahierStage) {
        boolean stageEndReached = FinalStageDocumentPdfAccessService.isStageEndDateReached(stage);

        if (cahierStage.isEmpty()) {
            List<String> draftBlockers = getLogbookDraftCreationBlockingReasons(stage);
            String reason;
            if (!stageEndReached) {
                reason = "Le cahier de stage est en preparation jusqu'a la date de fin du stage.";
            } else if (draftBlockers.isEmpty()) {
                reason = "Le cahier de stage n'est pas encore initialise.";
            } else {
                reason = "Le cahier de stage n'est pas encore disponible. " + joinReasons(draftBlockers);
            }
            boolean generationAutorisee = stageEndReached && draftBlockers.isEmpty();
            StageDocumentStatusDto missing = missingStatus(
                    "CAHIER_STAGE",
                    "Cahier de stage",
                    reason,
                    generationAutorisee
            );
            missing.setStatut(stageEndReached ? "Manquant" : "En préparation");
            applyPendingLogbookSignatureFlags(missing);
            return missing;
        }

        CahierStage item = cahierStage.get();
        List<String> blockers = getLogbookPdfBlockingReasons(stage, item);
        boolean ready = blockers.isEmpty();
        String statutLabel = ready
                ? "Disponible"
                : (stageEndReached ? "Conditions non satisfaites" : "En préparation");

        StageDocumentStatusDto status = documentStatus(
                "CAHIER_STAGE",
                "Cahier de stage",
                item.getId(),
                ready,
                true,
                ready,
                statutLabel,
                joinReasons(blockers)
        );
        applyLogbookSignatureFlags(status, item);
        return status;
    }

    private StageDocumentStatusDto documentStatus(String code,
                                                  String libelle,
                                                  Long documentId,
                                                  boolean disponible,
                                                  boolean genere,
                                                  boolean generationAutorisee,
                                                  String statut,
                                                  String raison) {
        StatutDocument statutDocument = resolveStatutDocument(disponible, genere, raison);
        StageDocumentStatusDto dto = new StageDocumentStatusDto(
                code, libelle, documentId, disponible, genere, generationAutorisee,
                statut, raison, false, "",
                new ArrayList<>(),
                statutDocument);
        return dto;
    }

    private StageDocumentStatusDto missingStatus(String code, String libelle, String reason, boolean generationAutorisee) {
        StageDocumentStatusDto dto = new StageDocumentStatusDto(
                code, libelle, null, false, false, generationAutorisee,
                "Manquant", reason, false, "",
                new ArrayList<>(),
                StatutDocument.BROUILLON);
        return dto;
    }

    private void applyConventionSignatureFlags(StageDocumentStatusDto dto, ConventionStage convention) {
        dto.setSignataires(buildSignatoryStatuses(CONVENTION_SIGNATORIES, convention::estSignePar));
    }

    private void applyPendingConventionSignatureFlags(StageDocumentStatusDto dto) {
        dto.setSignataires(buildPendingSignatoryStatuses(CONVENTION_SIGNATORIES));
    }

    private void applyLogbookSignatureFlags(StageDocumentStatusDto dto, CahierStage cahier) {
        dto.setSignataires(buildLogbookSignatoryStatuses(cahier));
    }

    private void applyPendingLogbookSignatureFlags(StageDocumentStatusDto dto) {
        dto.setSignataires(buildPendingSignatoryStatuses(LOGBOOK_SIGNATORIES));
    }

    private void applyEvaluationSignatureFlags(StageDocumentStatusDto dto, FicheEvaluation fiche) {
        dto.setSignataires(buildSignatoryStatuses(EVALUATION_SIGNATORIES, fiche::estSignePar));
    }

    private void applyPendingEvaluationSignatureFlags(StageDocumentStatusDto dto) {
        dto.setSignataires(buildPendingSignatoryStatuses(EVALUATION_SIGNATORIES));
    }

    private List<DocumentSignatoryStatusDto> buildSignatoryStatuses(
            List<RoleSignature> roles,
            java.util.function.Predicate<RoleSignature> signedPredicate) {
        List<DocumentSignatoryStatusDto> result = new ArrayList<>();
        for (RoleSignature role : roles) {
            result.add(new DocumentSignatoryStatusDto(
                    role.name(),
                    signatoryLabel(role),
                    signedPredicate.test(role)));
        }
        return result;
    }

    private List<DocumentSignatoryStatusDto> buildPendingSignatoryStatuses(List<RoleSignature> roles) {
        return buildSignatoryStatuses(roles, role -> false);
    }

    private String signatoryLabel(RoleSignature role) {
        return switch (role) {
            case STAGIAIRE -> "Stagiaire";
            case ENCADRANT_ACADEMIQUE -> "Encadrant academique";
            case ENCADRANT_PROFESSIONNEL -> "Encadrant professionnel";
            case RESPONSABLE_ENTREPRISE -> "Responsable entreprise";
            case RESPONSABLE_UNIVERSITAIRE -> "Responsable universitaire";
        };
    }

    /**
     * Détermine le {@link StatutDocument} à partir des indicateurs de disponibilité.
     * La logique est purement automatique : aucune validation manuelle du responsable
     * des stages n'entre en jeu.
     */
    private StatutDocument resolveStatutDocument(boolean disponible, boolean genere, String raisonAbsence) {
        if (disponible) {
            return StatutDocument.DISPONIBLE_IMPRESSION;
        }
        if (!genere) {
            // Le document n'existe pas encore — brouillon ou non initialisé
            return StatutDocument.BROUILLON;
        }
        // Le document existe mais des signatures manquent encore
        boolean signaturesManquantes = raisonAbsence != null && raisonAbsence.toLowerCase().contains("signature");
        return signaturesManquantes ? StatutDocument.EN_ATTENTE_SIGNATURES : StatutDocument.SIGNATURES_COMPLETES;
    }

    private Role getCurrentRole() {
        return jwtService.getAuthenticatedUtilisateur()
                .map(Utilisateur::getRole)
                .orElse(null);
    }

    private boolean canSeeConvention(Role role, Optional<ConventionStage> convention) {
        if (role == null) {
            return false;
        }

        return switch (role) {
            case ADMINISTRATEUR,
                    STAGIAIRE,
                    ENCADRANT_ACADEMIQUE,
                    ENCADRANT_PROFESSIONNEL,
                    RESPONSABLE_STAGE,
                    RESPONSABLE_ENTREPRISE -> true;
            default -> false;
        };
    }

    private boolean canSeeEvaluation(Role role, Optional<FicheEvaluation> evaluation) {
        if (role == null) {
            return false;
        }

        return switch (role) {
            case ADMINISTRATEUR,
                    STAGIAIRE,
                    ENCADRANT_PROFESSIONNEL,
                    ENCADRANT_ACADEMIQUE,
                    RESPONSABLE_STAGE,
                    RESPONSABLE_ENTREPRISE -> true;
            default -> false;
        };
    }

    private boolean canSeeLogbook(Role role, Optional<CahierStage> cahierStage) {
        if (role == null) {
            return false;
        }

        return switch (role) {
            case ADMINISTRATEUR,
                    STAGIAIRE,
                    ENCADRANT_ACADEMIQUE,
                    ENCADRANT_PROFESSIONNEL,
                    RESPONSABLE_STAGE,
                    RESPONSABLE_ENTREPRISE -> true;
            default -> false;
        };
    }

    private void ensureConventionVisible(Stage stage, Optional<ConventionStage> convention) {
        if (!canSeeConvention(getCurrentRole(), convention)) {
            throw new AccessDeniedException("Convention de stage non autorisee pour votre role.");
        }
    }

    private void ensureEvaluationVisible(Stage stage, Optional<FicheEvaluation> evaluation) {
        if (!canSeeEvaluation(getCurrentRole(), evaluation)) {
            throw new AccessDeniedException("Fiche d'evaluation non autorisee pour votre role.");
        }
    }

    private void ensureLogbookVisible(Stage stage, Optional<CahierStage> cahierStage) {
        if (!canSeeLogbook(getCurrentRole(), cahierStage)) {
            throw new AccessDeniedException("Cahier de stage non autorise pour votre role.");
        }
    }

    private ConventionStage getOrCreateConventionForProcess(Stage stage) {
        validateConventionProcessTriggered(stage);
        if (conventionStageRepository.findByStageId(stage.getId()).isEmpty()) {
            conventionStageService.createByStage(stage.getId(), new ConventionStageDto());
        }
        return conventionStageRepository.findByStageId(stage.getId())
                .orElseThrow(() -> new IllegalStateException("Convention introuvable pour ce stage."));
    }

    private CahierStage getOrCreateLogbookForProcess(Stage stage) {
        List<String> blockers = getLogbookDraftCreationBlockingReasons(stage);
        if (!blockers.isEmpty()) {
            throw new IllegalStateException(joinReasons(blockers));
        }
        if (cahierStageRepository.findByStageId(stage.getId()).isEmpty()) {
            cahierStageService.createByStage(stage.getId(), new CahierStageDto());
        }
        return cahierStageRepository.findByStageId(stage.getId())
                .orElseThrow(() -> new IllegalStateException("Cahier de stage introuvable pour ce stage."));
    }

    private FicheEvaluation getOrCreateEvaluationForProcess(Stage stage) {
        EvaluationStageAccessRules.ensureEvaluationPeriodOpen(stage);
        if (!canCreateEvaluationDraft(stage)) {
            throw new IllegalStateException(buildEvaluationDraftMissingReason(stage));
        }
        return evaluationSheetBootstrapService.ensureSheetExists(stage.getId());
    }

    private void validateConventionProcessTriggered(Stage stage) {
        if (!isConventionProcessTriggered(stage)) {
            throw new IllegalStateException("La convention n'est pas disponible pour un stage refuse ou annule.");
        }
    }

    /**
     * Autorise l'initialisation de la convention dès la création du stage,
     * sans attendre la date de début ni le passage en {@link StatutStage#EN_COURS}.
     */
    private boolean isConventionProcessTriggered(Stage stage) {
        if (stage == null || stage.getId() == null) {
            return false;
        }
        StatutStage statut = stage.getStatut();
        return statut != StatutStage.REFUSE && statut != StatutStage.ANNULE;
    }

    private boolean canCreateEvaluationDraft(Stage stage) {
        return EvaluationStageAccessRules.isEvaluationPeriodOpen(stage);
    }

    private String buildEvaluationDraftMissingReason(Stage stage) {
        if (!EvaluationStageAccessRules.isEvaluationPeriodOpen(stage)) {
            return EvaluationStageAccessRules.UNAVAILABLE_MESSAGE;
        }
        return "La fiche d'evaluation n'est pas encore disponible pour ce stage.";
    }

    private List<String> getConventionPdfBlockingReasons(Stage stage, ConventionStage convention) {
        return conventionStagePdfAccessService.collectBlockingReasons(stage, convention);
    }

    private List<String> getEvaluationPdfBlockingReasons(Stage stage, FicheEvaluation fiche) {
        return finalStageDocumentPdfAccessService.collectEvaluationPdfBlockingReasons(stage, fiche);
    }

    private List<String> getLogbookDraftCreationBlockingReasons(Stage stage) {
        List<String> reasons = new ArrayList<>();
        if (stage == null || stage.getStatut() == StatutStage.REFUSE || stage.getStatut() == StatutStage.ANNULE) {
            reasons.add("Le cahier de stage n'est pas disponible pour un stage refuse ou annule.");
        }
        return reasons;
    }

    private List<String> getLogbookPdfBlockingReasons(Stage stage, CahierStage cahierStage) {
        return finalStageDocumentPdfAccessService.collectLogbookPdfBlockingReasons(stage, cahierStage);
    }

    private List<DocumentSignatoryStatusDto> buildLogbookSignatoryStatuses(CahierStage cahier) {
        List<DocumentSignatoryStatusDto> result = new ArrayList<>();
        for (RoleSignature role : LOGBOOK_SIGNATORIES) {
            var signature = cahier.getSignaturePour(role);
            result.add(new DocumentSignatoryStatusDto(
                    role.name(),
                    signatoryLabel(role),
                    signature.isPresent(),
                    signature.map(Signature::getDateSignature).orElse(null)
            ));
        }
        return result;
    }

    private void validateNoBlockingReasons(List<String> reasons) {
        if (!reasons.isEmpty()) {
            throw new BusinessException(joinReasons(reasons));
        }
    }

    private void validateCondition(boolean condition, List<String> reasons, String message) {
        if (!condition) {
            reasons.add(message);
        }
    }

    private SignatureRequirement signatureRequirement(boolean present, String message) {
        return new SignatureRequirement(present, message);
    }

    private void addMissingSignatureReasons(List<String> reasons, List<SignatureRequirement> requirements) {
        requirements.stream()
                .filter(requirement -> !requirement.present())
                .map(SignatureRequirement::message)
                .forEach(reasons::add);
    }

    private String joinReasons(List<String> reasons) {
        List<String> filtered = reasons.stream()
                .filter(reason -> reason != null && !reason.isBlank())
                .distinct()
                .toList();
        if (filtered.isEmpty()) {
            return "";
        }
        return String.join(" ", filtered);
    }

    private byte[] createLogbookPdf(Stage stage, CahierStage cahierStage) {
        TrelloSnapshot trelloSnapshot = getTrelloSnapshot(stage);
        List<Reunion> meetings = getWeeklyMeetingsForLogbook(stage.getId());
        List<Absence> absences = absenceRepository.findByStageId(stage.getId())
                .stream()
                .sorted(Comparator.comparing(Absence::getDateAbsence, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        CahierStagePdfService.TrelloSnapshot trello = new CahierStagePdfService.TrelloSnapshot(
                trelloSnapshot.synchronizedBoard(),
                trelloSnapshot.tasks(),
                trelloSnapshot.listNames()
        );
        try {
            FicheEvaluation evaluation = ficheEvaluationRepository.findFirstByStageId(stage.getId()).orElse(null);
            return cahierStagePdfService.generer(stage, cahierStage, trello, meetings, absences, evaluation);
        } catch (java.io.IOException e) {
            throw new BusinessException("Impossible de generer le PDF du cahier de stage.");
        }
    }

    private Paragraph heroMeta(String label, String value) {
        return new Paragraph()
                .add(new Text(label.toUpperCase() + "\n")
                        .setBold()
                        .setFontSize(8)
                        .setFontColor(new DeviceRgb(209, 250, 229)))
                .add(new Text(safeText(value))
                        .setFontSize(10)
                        .setBold()
                        .setFontColor(ColorConstants.WHITE))
                .setMarginBottom(8);
    }

    private Cell summaryCell(String label, String value) {
        return new Cell()
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(PDF_BORDER, 1))
                .setPadding(12)
                .add(new Paragraph(label)
                        .setFontSize(8)
                        .setBold()
                        .setFontColor(PDF_MUTED)
                        .setMarginBottom(5))
                .add(new Paragraph(safeText(value))
                        .setFontSize(10)
                        .setBold()
                        .setFontColor(PDF_TEXT));
    }

    private Table buildSectionCard(String title, Table content, Color headerColor) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1f}))
                .useAllAvailableWidth()
                .setMarginBottom(12)
                .setBorder(new SolidBorder(PDF_BORDER, 1));

        table.addCell(new Cell()
                .setBackgroundColor(headerColor)
                .setBorder(Border.NO_BORDER)
                .setPadding(11)
                .add(new Paragraph(title)
                        .setBold()
                        .setFontSize(11)
                        .setFontColor(PDF_PRIMARY)));

        table.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(ColorConstants.WHITE)
                .setPadding(12)
                .add(content));

        return table;
    }

    private Table buildDualCard(Table leftCard, Table rightCard) {
        Table wrapper = new Table(UnitValue.createPercentArray(new float[]{1f, 1f}))
                .useAllAvailableWidth()
                .setMarginBottom(2);

        wrapper.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingRight(6).add(leftCard));
        wrapper.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingLeft(6).add(rightCard));
        return wrapper;
    }

    private Table buildInfoContentTable(List<String[]> rows) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1.15f, 2.15f}))
                .useAllAvailableWidth();

        for (String[] row : rows) {
            table.addCell(new Cell()
                    .setBackgroundColor(PDF_SLATE_SOFT)
                    .setBorder(new SolidBorder(PDF_BORDER, 1))
                    .setPadding(9)
                    .add(new Paragraph(row[0])
                            .setBold()
                            .setFontSize(9)
                            .setFontColor(PDF_PRIMARY)));

            table.addCell(new Cell()
                    .setBackgroundColor(ColorConstants.WHITE)
                    .setBorder(new SolidBorder(PDF_BORDER, 1))
                    .setPadding(9)
                    .add(new Paragraph(safeText(row[1]))
                            .setFontSize(9.5f)
                            .setFontColor(PDF_TEXT)));
        }

        return table;
    }

    private Table buildNarrativeTable(String text) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1f}))
                .useAllAvailableWidth();

        table.addCell(new Cell()
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(PDF_BORDER, 1))
                .setPadding(12)
                .add(new Paragraph(safeText(text))
                        .setFontSize(9.5f)
                        .setFontColor(PDF_TEXT)
                        .setMultipliedLeading(1.35f)));

        return table;
    }

    private Cell signatureHeaderCell(String text) {
        return new Cell()
                .setBackgroundColor(PDF_PRIMARY)
                .setBorder(Border.NO_BORDER)
                .setPadding(9)
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph(text)
                        .setBold()
                        .setFontSize(9)
                        .setFontColor(ColorConstants.WHITE));
    }

    private void addSignatureRow(Table table, String signer, String role, boolean signed, java.time.temporal.TemporalAccessor signedAt, String signatureImage) {
        table.addCell(signatureBodyCell(signer));
        table.addCell(signatureBodyCell(role));
        table.addCell(signatureStatusCell(signed));
        table.addCell(signatureBodyCell(signed ? formatDateTimeOrDash(signedAt) : "-"));
        table.addCell(signatureImageCell(signed ? signatureImage : ""));
    }

    private Cell signatureBodyCell(String text) {
        return new Cell()
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(PDF_BORDER, 1))
                .setPadding(9)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(safeText(text))
                        .setFontSize(9)
                        .setFontColor(PDF_TEXT));
    }

    private Cell signatureStatusCell(boolean signed) {
        return new Cell()
                .setBackgroundColor(signed ? PDF_SIGNED_BG : PDF_PENDING_BG)
                .setBorder(new SolidBorder(PDF_BORDER, 1))
                .setPadding(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(signed ? "Signe" : "Non signe")
                        .setBold()
                        .setFontSize(9)
                        .setFontColor(signed ? PDF_SIGNED_TEXT : PDF_PENDING_TEXT));
    }

    private Cell signatureImageCell(String signatureImage) {
        Cell cell = new Cell()
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(PDF_BORDER, 1))
                .setPadding(6)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        Optional<Image> image = loadSignatureImage(signatureImage);
        if (image.isPresent()) {
            cell.add(image.get());
        } else if (signatureImagePdfHelper.isExploitableImageSource(signatureImage)) {
            cell.add(new Paragraph("Image indisponible")
                    .setFontSize(8)
                    .setFontColor(PDF_MUTED));
        } else if (!safeText(signatureImage).equals("-") && !signatureImage.isBlank()) {
            cell.add(new Paragraph("Signature enregistrée (aperçu indisponible)")
                    .setFontSize(8)
                    .setFontColor(PDF_MUTED));
        } else {
            cell.add(new Paragraph("-")
                    .setFontSize(8)
                    .setFontColor(PDF_MUTED));
        }

        return cell;
    }

    private byte[] createPdf(String documentTitle, Stage stage, List<String[]> rows, List<SignaturePdfRow> signatureRows) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument, PageSize.A4);
        document.setMargins(32, 32, 36, 32);

        document.add(new Paragraph("FSEGS - Gestion des stages")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(11)
                .setBold()
                .setFontColor(ColorConstants.DARK_GRAY));

        document.add(new Paragraph("Document administratif officiel genere automatiquement des que les signatures obligatoires sont completes.")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setMarginBottom(8));

        document.add(new LineSeparator(new SolidLine()).setMarginBottom(12));

        document.add(new Paragraph(documentTitle)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18)
                .setBold()
                .setFontColor(ColorConstants.BLACK)
                .setMarginBottom(4));

        document.add(new Paragraph("Date de generation : " + formatDateTime(LocalDateTime.now()))
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setMarginBottom(16));

        document.add(sectionTitle("Informations du stagiaire"));
        document.add(buildInfoTable(List.of(
                new String[]{"Nom complet", stage.getStagiaire() == null ? "-" : buildFullName(stage.getStagiaire().getPrenom(), stage.getStagiaire().getNom())},
                new String[]{"Email", stage.getStagiaire() == null ? "-" : safeText(stage.getStagiaire().getEmail())},
                new String[]{"Matricule", stage.getStagiaire() == null ? "-" : safeText(stage.getStagiaire().getMatricule())},
                new String[]{"Filiere / niveau", buildStudentTrack(stage)}
        )));

        document.add(sectionTitle("Informations de l'entreprise"));
        document.add(buildInfoTable(List.of(
                new String[]{"Entreprise", stage.getEntreprise() == null ? "-" : safeText(stage.getEntreprise().getNom())},
                new String[]{"Adresse", stage.getEntreprise() == null ? "-" : safeText(stage.getEntreprise().getAdresse())},
                new String[]{"Email", stage.getEntreprise() == null ? "-" : safeText(stage.getEntreprise().getEmail())},
                new String[]{"Representant", buildCompanyRepresentative(stage)}
        )));

        document.add(sectionTitle("Informations du stage"));
        document.add(buildInfoTable(List.of(
                new String[]{"Titre", safeText(stage.getTitre())},
                new String[]{"Sujet / mission", safeText(stage.getSujet())},
                new String[]{"Periode", formatDate(stage.getDateDebut()) + " - " + formatDate(stage.getDateFin())},
                new String[]{"Duree", stage.getDuree() == null ? "-" : stage.getDuree() + " mois"},
                new String[]{"Statut", stage.getStatut() == null ? "-" : stage.getStatut().name()}
        )));

        document.add(sectionTitle("Encadrement"));
        document.add(buildInfoTable(List.of(
                new String[]{"Encadrant academique", stage.getEncadrantAcademique() == null ? "-" : buildFullName(stage.getEncadrantAcademique().getPrenom(), stage.getEncadrantAcademique().getNom())},
                new String[]{"Encadrant professionnel", stage.getEncadrantProfessionnel() == null ? "-" : buildFullName(stage.getEncadrantProfessionnel().getPrenom(), stage.getEncadrantProfessionnel().getNom())}
        )));

        document.add(sectionTitle("Etat du document"));
        document.add(buildInfoTable(rows));

        if (!signatureRows.isEmpty()) {
            document.add(sectionTitle("Signatures"));
            document.add(buildSignatureTable(signatureRows));
        }

        document.close();
        return outputStream.toByteArray();
    }

    private byte[] createEvaluationPdf(Stage stage, FicheEvaluation fiche) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument, PageSize.A4);
        document.setMargins(26, 26, 30, 26);

        document.add(buildEvaluationHero(stage, fiche));
        document.add(buildEvaluationSummary(stage, fiche));

        document.add(buildSectionCard(
                "Informations generales",
                buildInfoContentTable(List.of(
                        new String[]{"Nom et prenom du stagiaire", stage.getStagiaire() == null ? "-" : buildFullName(stage.getStagiaire().getPrenom(), stage.getStagiaire().getNom())},
                        new String[]{"Section", buildStudentTrack(stage)},
                        new String[]{"Entreprise / lieu du stage", stage.getEntreprise() == null ? "-" : safeText(stage.getEntreprise().getNom()) + " - " + safeText(stage.getEntreprise().getAdresse())}
                )),
                PDF_PRIMARY_SOFT
        ));

        document.add(buildDualCard(
                buildSectionCard("Partie 1 : Encadrant professionnel", buildInfoContentTable(List.of(
                        new String[]{"Points forts", safeText(fiche.getPointFortEncadrantPro())},
                        new String[]{"Axes d'amelioration", safeText(fiche.getAxeAmeliorationEncadrantPro())},
                        new String[]{"Nom du signataire", resolveSignerName(fiche.getSignaturePour(RoleSignature.ENCADRANT_PROFESSIONNEL), "-")},
                        new String[]{"Role", RoleSignature.ENCADRANT_PROFESSIONNEL.name()},
                        new String[]{"Date", fiche.getSignaturePour(RoleSignature.ENCADRANT_PROFESSIONNEL).map(s -> formatDateTime(s.getDateSignature())).orElse("-")}
                )), PDF_PRIMARY_SOFT),
                buildSectionCard("Partie 2 : Representant de l'entreprise", buildInfoContentTable(List.of(
                        new String[]{"Points forts", safeText(fiche.getPointFortResponsableEntreprise())},
                        new String[]{"Axes d'amelioration", safeText(fiche.getAxeAmeliorationResponsableEntreprise())},
                        new String[]{"Nom du signataire", resolveSignerName(fiche.getSignaturePour(RoleSignature.RESPONSABLE_ENTREPRISE), "-")},
                        new String[]{"Role", RoleSignature.RESPONSABLE_ENTREPRISE.name()},
                        new String[]{"Date", fiche.getSignaturePour(RoleSignature.RESPONSABLE_ENTREPRISE).map(s -> formatDateTime(s.getDateSignature())).orElse("-")}
                )), PDF_SECONDARY_SOFT)
        ));

        document.add(buildSectionCard(
                "Partie 3 : Notes attribuees",
                buildEvaluationNotesContent(fiche),
                PDF_PRIMARY_SOFT
        ));

        document.add(buildSectionCard(
                "Partie 4 : Informations liees au stage",
                buildInfoContentTable(List.of(
                        new String[]{"Stage", safeText(stage.getTitre())},
                        new String[]{"Sujet", safeText(stage.getSujet())},
                        new String[]{"Periode", formatDate(stage.getDateDebut()) + " - " + formatDate(stage.getDateFin())},
                        new String[]{"Reunion finale", fiche.getReunionFinale() == null ? "-" : firstNonBlank(fiche.getReunionFinale().getNumReunion(), "Reunion finale")},
                        new String[]{"Date reunion finale", fiche.getReunionFinale() == null ? "-" : formatDate(fiche.getReunionFinale().getDate()) + " " + safeTime(fiche.getReunionFinale().getHeure())}
                )),
                PDF_SECONDARY_SOFT
        ));

        document.add(buildSectionCard(
                "Zone finale des signatures",
                buildEvaluationSignaturesContent(stage, fiche),
                PDF_PRIMARY_SOFT
        ));

        document.add(new Paragraph("La fiche est verrouillee lorsque les signatures sont completes.")
                .setFontSize(9)
                .setItalic()
                .setFontColor(PDF_MUTED)
                .setMarginTop(6)
                .setTextAlignment(TextAlignment.RIGHT));

        document.close();
        return outputStream.toByteArray();
    }

    private Table buildEvaluationHero(Stage stage, FicheEvaluation fiche) {
        Table hero = new Table(UnitValue.createPercentArray(new float[]{2.4f, 1f}))
                .useAllAvailableWidth()
                .setMarginBottom(14);

        Cell leftCell = new Cell()
                .setBackgroundColor(PDF_PRIMARY)
                .setBorder(Border.NO_BORDER)
                .setPadding(18);

        leftCell.add(new Paragraph("FSEGS - Gestion des stages")
                .setFontSize(10)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
                .setMarginBottom(8));
        leftCell.add(new Paragraph("FICHE D'EVALUATION DE STAGE")
                .setFontSize(20)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
                .setMarginBottom(6));
        leftCell.add(new Paragraph("Document d'evaluation renseigne conjointement par l'encadrant professionnel et le representant de l'entreprise.")
                .setFontSize(10)
                .setFontColor(new DeviceRgb(226, 232, 240))
                .setMultipliedLeading(1.3f));

        Cell rightCell = new Cell()
                .setBackgroundColor(PDF_SECONDARY)
                .setBorder(Border.NO_BORDER)
                .setPadding(18);
        rightCell.add(heroMeta("Stage", safeText(stage.getTitre())));
        rightCell.add(heroMeta("Note finale", InternshipPdfEvaluationFormat.formatFinalScoreValue(fiche.getNoteFinale())));
        rightCell.add(heroMeta("Etat", fiche.estVerrouillee() ? "Verrouillee" : "En cours"));
        rightCell.add(heroMeta("Generation", formatDateTime(LocalDateTime.now())));

        hero.addCell(leftCell);
        hero.addCell(rightCell);
        return hero;
    }

    private Table buildEvaluationSummary(Stage stage, FicheEvaluation fiche) {
        Table summary = new Table(UnitValue.createPercentArray(new float[]{1f, 1f, 1f, 1f}))
                .useAllAvailableWidth()
                .setMarginBottom(14);

        summary.addCell(summaryCell("Stagiaire",
                stage.getStagiaire() == null ? "-" : buildFullName(stage.getStagiaire().getPrenom(), stage.getStagiaire().getNom())));
        summary.addCell(summaryCell("Entreprise",
                stage.getEntreprise() == null ? "-" : safeText(stage.getEntreprise().getNom())));
        summary.addCell(summaryCell("Reunion finale",
                fiche.getReunionFinale() == null ? "-" : firstNonBlank(fiche.getReunionFinale().getNumReunion(), "Reunion finale")));
        summary.addCell(summaryCell("Verrouillage",
                fiche.estVerrouillee() ? "Actif" : "En attente"));

        return summary;
    }

    private Table buildEvaluationNotesContent(FicheEvaluation fiche) {
        Table container = new Table(UnitValue.createPercentArray(new float[]{1f}))
                .useAllAvailableWidth();

        Table table = new Table(UnitValue.createPercentArray(new float[]{2.5f, 1f, 2f}))
                .useAllAvailableWidth();

        table.addHeaderCell(signatureHeaderCell("Critere evalue"));
        table.addHeaderCell(signatureHeaderCell("Note / 5"));
        table.addHeaderCell(signatureHeaderCell("Commentaire"));

        List<String[]> rows = InternshipPdfEvaluationFormat.buildCriterionRows(fiche);
        if (rows.isEmpty()) {
            table.addCell(new Cell(1, 3)
                    .setBorder(new SolidBorder(PDF_BORDER, 1))
                    .setPadding(10)
                    .add(new Paragraph("Aucune note attribuee n'a encore ete renseignee.")
                            .setFontSize(9)
                            .setFontColor(PDF_MUTED)));
        } else {
            rows.forEach(row -> {
                table.addCell(signatureBodyCell(row[0]));
                table.addCell(signatureBodyCell(row[1]));
                table.addCell(signatureBodyCell(row[2]));
            });
        }

        container.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(table));
        container.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(8)
                .add(new Paragraph(InternshipPdfEvaluationFormat.formatFinalScoreLabel(fiche.getNoteFinale()))
                        .setBold()
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFontColor(PDF_PRIMARY)));
        return container;
    }

    private Table buildEvaluationSignaturesContent(Stage stage, FicheEvaluation fiche) {
        Table container = new Table(UnitValue.createPercentArray(new float[]{1f}))
                .useAllAvailableWidth();
        container.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .add(new Paragraph("Les signatures ci-dessous valident definitivement la fiche d'evaluation. Toute modification est interdite des que les deux signatures sont presentes.")
                        .setFontSize(9)
                        .setFontColor(PDF_MUTED)
                        .setMarginBottom(10)));
        container.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(buildSignatureTable(buildEvaluationSignatureRows(stage, fiche))));
        return container;
    }

    private Paragraph sectionTitle(String title) {
        return new Paragraph(title)
                .setFontSize(12)
                .setBold()
                .setFontColor(ColorConstants.DARK_GRAY)
                .setMarginTop(8)
                .setMarginBottom(6);
    }

    private Table buildInfoTable(List<String[]> rows) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1.25f, 2.75f}))
                .useAllAvailableWidth()
                .setMarginBottom(10);

        for (String[] row : rows) {
            addInfoRow(table, row[0], row[1]);
        }

        return table;
    }

    private Table buildSignatureTable(List<SignaturePdfRow> rows) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{2.0f, 1.4f, 1.0f, 1.2f, 1.5f}))
                .useAllAvailableWidth()
                .setMarginBottom(12);

        table.addHeaderCell(headerCell("Signataire"));
        table.addHeaderCell(headerCell("Qualite"));
        table.addHeaderCell(headerCell("Etat"));
        table.addHeaderCell(headerCell("Date"));
        table.addHeaderCell(headerCell("Signature"));

        for (SignaturePdfRow row : rows) {
            table.addCell(bodyCell(row.signer()));
            table.addCell(bodyCell(row.role()));
            table.addCell(bodyCell(row.signed() ? "Signe" : "En attente"));
            table.addCell(bodyCell(row.signed() ? formatDateTimeOrDash(row.signedAt()) : "-"));
            table.addCell(signatureImageCell(row.signed() ? row.signatureImage() : ""));
        }

        return table;
    }

    private void addInfoRow(Table table, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBorder(Border.NO_BORDER)
                .setPadding(8));
        table.addCell(new Cell()
                .add(new Paragraph(safeText(value)))
                .setBorder(Border.NO_BORDER)
                .setPadding(8));
    }

    private Cell headerCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setBorder(Border.NO_BORDER)
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell bodyCell(String text) {
        return new Cell()
                .add(new Paragraph(safeText(text)))
                .setBorder(Border.NO_BORDER)
                .setPadding(8)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private List<SignaturePdfRow> buildEvaluationSignatureRows(Stage stage, FicheEvaluation fiche) {
        Optional<Signature> epSig = fiche.getSignaturePour(RoleSignature.ENCADRANT_PROFESSIONNEL);
        Optional<Signature> reSig = fiche.getSignaturePour(RoleSignature.RESPONSABLE_ENTREPRISE);
        String fallbackEp = stage.getEncadrantProfessionnel() == null ? "-" : buildFullName(stage.getEncadrantProfessionnel().getPrenom(), stage.getEncadrantProfessionnel().getNom());
        String urlEp = stage.getEncadrantProfessionnel() == null ? "" : firstNonBlank(stage.getEncadrantProfessionnel().getUrlSignature(), "");
        String urlRe = stage.getTuteurEntreprise() == null ? "" : firstNonBlank(stage.getTuteurEntreprise().getUrlSignature(), "");
        return List.of(
                new SignaturePdfRow(
                        resolveSignerName(epSig, fallbackEp),
                        "Encadrant professionnel",
                        epSig.isPresent(),
                        epSig.map(Signature::getDateSignature).orElse(null),
                        resolveSignerUrl(epSig, urlEp)
                ),
                new SignaturePdfRow(
                        resolveSignerName(reSig, buildCompanyRepresentative(stage)),
                        "Representant entreprise",
                        reSig.isPresent(),
                        reSig.map(Signature::getDateSignature).orElse(null),
                        resolveSignerUrl(reSig, urlRe)
                )
        );
    }

    private List<SignaturePdfRow> buildLogbookSignatureRows(Stage stage, CahierStage cahierStage) {
        Optional<Signature> stSig = cahierStage.getSignaturePour(RoleSignature.STAGIAIRE);
        Optional<Signature> eaSig = cahierStage.getSignaturePour(RoleSignature.ENCADRANT_ACADEMIQUE);
        Optional<Signature> epSig = cahierStage.getSignaturePour(RoleSignature.ENCADRANT_PROFESSIONNEL);
        Optional<Signature> reSig = cahierStage.getSignaturePour(RoleSignature.RESPONSABLE_ENTREPRISE);
        return List.of(
                new SignaturePdfRow(
                        resolveSignerName(stSig, stage.getStagiaire() == null ? "-" : buildFullName(stage.getStagiaire().getPrenom(), stage.getStagiaire().getNom())),
                        "Stagiaire",
                        stSig.isPresent(),
                        stSig.map(Signature::getDateSignature).orElse(null),
                        resolveSignerUrl(stSig, stage.getStagiaire() == null ? "" : firstNonBlank(stage.getStagiaire().getUrlSignature(), ""))
                ),
                new SignaturePdfRow(
                        resolveSignerName(eaSig, stage.getEncadrantAcademique() == null ? "-" : buildFullName(stage.getEncadrantAcademique().getPrenom(), stage.getEncadrantAcademique().getNom())),
                        "Encadrant academique",
                        eaSig.isPresent(),
                        eaSig.map(Signature::getDateSignature).orElse(null),
                        resolveSignerUrl(eaSig, stage.getEncadrantAcademique() == null ? "" : firstNonBlank(stage.getEncadrantAcademique().getUrlSignature(), ""))
                ),
                new SignaturePdfRow(
                        resolveSignerName(epSig, stage.getEncadrantProfessionnel() == null ? "-" : buildFullName(stage.getEncadrantProfessionnel().getPrenom(), stage.getEncadrantProfessionnel().getNom())),
                        "Encadrant professionnel",
                        epSig.isPresent(),
                        epSig.map(Signature::getDateSignature).orElse(null),
                        resolveSignerUrl(epSig, stage.getEncadrantProfessionnel() == null ? "" : firstNonBlank(stage.getEncadrantProfessionnel().getUrlSignature(), ""))
                ),
                new SignaturePdfRow(
                        resolveSignerName(reSig, buildCompanyRepresentative(stage)),
                        "Representant entreprise",
                        reSig.isPresent(),
                        reSig.map(Signature::getDateSignature).orElse(null),
                        resolveSignerUrl(reSig, stage.getTuteurEntreprise() == null ? "" : firstNonBlank(stage.getTuteurEntreprise().getUrlSignature(), ""))
                )
        );
    }

    private String resolveSignerName(Optional<Signature> sigOpt, String fallback) {
        return sigOpt
                .flatMap(sig -> sig.getSignataireId() != null
                        ? utilisateurRepository.findById(sig.getSignataireId())
                        : Optional.empty())
                .map(u -> buildFullName(u.getPrenom(), u.getNom()))
                .orElse(fallback);
    }

    private String resolveSignerUrl(Optional<Signature> sigOpt, String fallback) {
        return signatureImagePdfHelper.resolveImageSource(sigOpt, fallback == null ? "" : fallback);
    }

    private Optional<Image> loadSignatureImage(String signatureSource) {
        return signatureImagePdfHelper.loadSignatureImage(signatureSource, 105f, 45f);
    }

    private Table buildTrelloTasksTable(TrelloSnapshot snapshot) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{2.0f, 1.4f, 2.6f}))
                .useAllAvailableWidth()
                .setMarginBottom(10);
        table.addHeaderCell(headerCell("Tache"));
        table.addHeaderCell(headerCell("Liste"));
        table.addHeaderCell(headerCell("Description"));

        for (Map<String, Object> task : snapshot.tasks()) {
            String description = String.valueOf(task.getOrDefault("desc", ""));
            table.addCell(bodyCell(String.valueOf(task.getOrDefault("name", "-"))));
            table.addCell(bodyCell(snapshot.listNames().getOrDefault(String.valueOf(task.getOrDefault("idList", "")), "-")));
            table.addCell(bodyCell(description.isBlank() ? "-" : description));
        }

        return table;
    }

    private Table buildMeetingsTable(List<Reunion> meetings) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1.1f, 1.2f, 2.2f, 2.5f}))
                .useAllAvailableWidth()
                .setMarginBottom(10);
        table.addHeaderCell(headerCell("Reunion"));
        table.addHeaderCell(headerCell("Date"));
        table.addHeaderCell(headerCell("Observation"));
        table.addHeaderCell(headerCell("Compte rendu"));

        for (Reunion meeting : meetings) {
            table.addCell(bodyCell(safeText(meeting.getNumReunion())));
            table.addCell(bodyCell(formatDate(meeting.getDate()) + " " + safeTime(meeting.getHeure())));
            table.addCell(bodyCell(safeText(meeting.getObservation())));
            table.addCell(bodyCell(safeText(meeting.getCompteRendu())));
        }

        return table;
    }

    private Table buildAbsenceTable(List<Absence> absences) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1.2f, 0.8f, 2.4f, 1.3f, 2.0f}))
                .useAllAvailableWidth()
                .setMarginBottom(10);
        table.addHeaderCell(headerCell("Date"));
        table.addHeaderCell(headerCell("Nombre"));
        table.addHeaderCell(headerCell("Justification"));
        table.addHeaderCell(headerCell("Statut"));
        table.addHeaderCell(headerCell("Commentaire"));

        if (absences.isEmpty()) {
            table.addCell(new Cell(1, 5)
                    .setPadding(8)
                    .setBorder(Border.NO_BORDER)
                    .add(new Paragraph("Aucune absence enregistree. Le stagiaire est considere present sur toute la periode.")
                            .setFontSize(9)
                            .setFontColor(PDF_MUTED)));
            return table;
        }

        for (Absence absence : absences) {
            table.addCell(bodyCell(formatDate(absence.getDateAbsence())));
            table.addCell(bodyCell(safeNumber(absence.getNbAbsence())));
            table.addCell(bodyCell(safeText(absence.getJustification())));
            table.addCell(bodyCell(safeText(absence.getStatut())));
            table.addCell(bodyCell(safeText(absence.getCommentaire())));
        }

        return table;
    }

    private TrelloSnapshot getTrelloSnapshot(Stage stage) {
        if (stage == null || !hasText(stage.getTrelloBoardId())) {
            return new TrelloSnapshot(false, List.of(), Map.of());
        }
        try {
            List<Map<String, Object>> lists = trelloService.getListsByBoard(stage.getTrelloBoardId());
            List<Map<String, Object>> cards = trelloService.getCardsByBoard(stage.getTrelloBoardId());
            Map<String, String> listNames = new LinkedHashMap<>();
            for (Map<String, Object> item : lists) {
                listNames.put(String.valueOf(item.getOrDefault("id", "")), String.valueOf(item.getOrDefault("name", "-")));
            }
            return new TrelloSnapshot(true, cards == null ? List.of() : cards, listNames);
        } catch (RuntimeException exception) {
            return new TrelloSnapshot(false, List.of(), Map.of());
        }
    }

    private List<Reunion> getWeeklyMeetingsForLogbook(Long stageId) {
        return LogbookMeetingSupport.weeklyMeetingsSorted(reunionRepository.findByStageId(stageId))
                .toList();
    }

    private Stage getAuthorizedStage(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage introuvable"));
        authorizeLinkedStageAccess(stage);
        ensureStageEligibleForDocuments(stage);
        return stage;
    }

    /** Les stages refuses n'ont pas de documents accessibles dans l'application. */
    private boolean isEligibleForStageDocuments(Stage stage) {
        return stage != null && stage.getStatut() != StatutStage.REFUSE;
    }

    private void ensureStageEligibleForDocuments(Stage stage) {
        if (!isEligibleForStageDocuments(stage)) {
            throw new BusinessException("Les documents ne sont pas disponibles pour un stage refuse.");
        }
    }

    private void authorizeLinkedStageAccess(Stage stage) {
        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifie introuvable."));
        Long userId = utilisateur.getId();
        boolean allowed = switch (utilisateur.getRole()) {
            case ADMINISTRATEUR, RESPONSABLE_STAGE -> true;
            case STAGIAIRE -> stage.getStagiaire() != null && userId.equals(stage.getStagiaire().getId());
            case ENCADRANT_ACADEMIQUE -> stage.getEncadrantAcademique() != null && userId.equals(stage.getEncadrantAcademique().getId());
            case ENCADRANT_PROFESSIONNEL -> stage.getEncadrantProfessionnel() != null && userId.equals(stage.getEncadrantProfessionnel().getId());
            case RESPONSABLE_ENTREPRISE -> {
                if (!(utilisateur instanceof ResponsableEntreprise re)) yield false;
                yield re.getEntreprise() != null
                        && stage.getEntreprise() != null
                        && re.getEntreprise().getId().equals(stage.getEntreprise().getId());
            }
            default -> false;
        };

        if (!allowed) {
            throw new AccessDeniedException("Acces refuse a ce document de stage.");
        }
    }

    private String buildFullName(String prenom, String nom) {
        String fullName = ((prenom == null ? "" : prenom.trim()) + " " + (nom == null ? "" : nom.trim())).trim();
        return fullName.isBlank() ? "-" : fullName;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? safeText(fallback) : first.trim();
    }

    private String safeNumber(Number value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String formatDate(java.time.temporal.TemporalAccessor date) {
        return date == null ? "-" : DATE_FORMAT.format(date);
    }

    private String formatDateTime(java.time.temporal.TemporalAccessor dateTime) {
        if (dateTime == null) {
            return "";
        }

        return DATE_TIME_FORMAT.format(dateTime);
    }

    private String formatDateTimeOrDash(java.time.temporal.TemporalAccessor dateTime) {
        String formatted = formatDateTime(dateTime);
        return formatted.isBlank() ? "-" : formatted;
    }

    private String safeTime(java.time.temporal.TemporalAccessor time) {
        if (time == null) {
            return "-";
        }
        return String.valueOf(time);
    }

    private String buildStudentTrack(Stage stage) {
        if (stage == null || stage.getStagiaire() == null) {
            return "-";
        }

        String filiere = stage.getStagiaire().getFiliere() != null ? safeText(stage.getStagiaire().getFiliere().getNom()) : "-";
        String niveau = stage.getStagiaire().getNiveau() != null ? "Niveau " + stage.getStagiaire().getNiveau() : "-";
        return (filiere + " / " + niveau).replace("- / -", "-");
    }

    private String buildCompanyRepresentative(Stage stage) {
        if (stage == null) {
            return "-";
        }

        if (stage.getTuteurEntreprise() != null) {
            return buildFullName(stage.getTuteurEntreprise().getPrenom(), stage.getTuteurEntreprise().getNom());
        }

        return stage.getEntreprise() != null ? safeText(stage.getEntreprise().getNom()) : "-";
    }

    private String toStatus(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Signe" : "En attente";
    }
}

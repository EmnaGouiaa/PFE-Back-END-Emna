package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CompanyValidationItemDto;
import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;
import fsegs.pfebackendemnagouuiaa.entities.Entreprise;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.entities.StatutValidation;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.CahierStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ConventionStageRepository;
import fsegs.pfebackendemnagouuiaa.repository.ResponsableEntrepriseRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CompanyValidationServiceImpl implements CompanyValidationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StageRepository stageRepository;
    private final ConventionStageRepository conventionStageRepository;
    private final CahierStageRepository cahierStageRepository;
    private final ResponsableEntrepriseRepository responsableEntrepriseRepository;
    private final NotificationService notificationService;
    private final ConventionStageService conventionStageService;
    private final CahierStageService cahierStageService;
    private final StageService stageService;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public List<CompanyValidationItemDto> getValidationItemsForAuthenticatedCompany() {
        ResponsableEntreprise responsable = getAuthenticatedResponsableEntreprise();
        Long entrepriseId = getEntrepriseId(responsable);

        List<CompanyValidationItemDto> items = new ArrayList<>();
        for (Stage stage : stageRepository.findByEntrepriseId(entrepriseId)) {
            if (stage == null || stage.getId() == null) {
                continue;
            }

            conventionStageRepository.findByStageId(stage.getId())
                    .filter(convention -> !Boolean.TRUE.equals(convention.getSigneeEntreprise()))
                    .map(convention -> buildConventionValidationItem(stage, convention))
                    .ifPresent(items::add);

            cahierStageRepository.findByStageId(stage.getId())
                    .filter(cahier -> !Boolean.TRUE.equals(cahier.getSigneeRespEntreprise()))
                    .map(cahier -> buildCahierValidationItem(stage, cahier))
                    .ifPresent(items::add);
        }

        return items.stream()
                .sorted(Comparator
                        .comparing(CompanyValidationItemDto::isPending).reversed()
                        .thenComparing(CompanyValidationItemDto::getStageId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(CompanyValidationItemDto::getType, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @Override
    @Transactional
    public CompanyValidationItemDto approve(String itemType, Long itemId) {
        return switch (normalizeType(itemType)) {
            case "DEMANDE_STAGE", "STAGE" -> buildStageValidationItem(stageService.validerStageParEntreprise(itemId));
            case "SUJET" -> approveSubject(itemId);
            case "CONVENTION" -> approveConvention(itemId);
            case "CAHIER_STAGE" -> approveCahier(itemId);
            default -> throw new IllegalArgumentException("Type de validation non pris en charge : " + itemType);
        };
    }

    @Override
    @Transactional
    public CompanyValidationItemDto reject(String itemType, Long itemId, String commentaire) {
        String normalizedType = normalizeType(itemType);
        String requiredComment = normalizeComment(commentaire);

        return switch (normalizedType) {
            case "DEMANDE_STAGE", "STAGE" -> rejectStage(itemId, requiredComment);
            case "SUJET" -> rejectSubject(itemId, requiredComment);
            case "CONVENTION" -> rejectDocument(itemId, requiredComment, "CONVENTION");
            case "CAHIER_STAGE" -> rejectDocument(itemId, requiredComment, "CAHIER_STAGE");
            default -> throw new IllegalArgumentException("Type de validation non pris en charge : " + itemType);
        };
    }

    private CompanyValidationItemDto approveSubject(Long stageId) {
        Stage stage = getManagedStage(stageId);
        stage.setStatutSujet(StatutValidation.VALIDEE);
        Stage saved = stageRepository.save(stage);

        notifyStudent(saved, "Sujet de stage validé",
                "Le sujet du stage " + safeStageTitle(saved) + " a été validé par votre entreprise.");
        notifyAcademicSupervisor(saved, "Sujet de stage validé côté entreprise",
                "Le sujet du stage " + safeStageTitle(saved) + " a été validé par le représentant entreprise.");

        return buildSubjectValidationItem(saved);
    }

    private CompanyValidationItemDto approveConvention(Long conventionId) {
        ConventionStage convention = conventionStageRepository.findById(conventionId)
                .orElseThrow(() -> new IllegalArgumentException("Convention introuvable."));
        Stage stage = getManagedStage(convention.getStage() != null ? convention.getStage().getId() : null);

        conventionStageService.signerParEntreprise(conventionId);
        notifyStudent(stage, "Convention validée",
                "La convention du stage " + safeStageTitle(stage) + " a été validée par votre entreprise.");
        return buildConventionValidationItem(stage, conventionStageRepository.findById(conventionId).orElse(convention));
    }

    private CompanyValidationItemDto approveCahier(Long cahierId) {
        CahierStage cahier = cahierStageRepository.findById(cahierId)
                .orElseThrow(() -> new IllegalArgumentException("Cahier de stage introuvable."));
        Stage stage = getManagedStage(cahier.getStage() != null ? cahier.getStage().getId() : null);

        cahierStageService.signerParResponsableEntreprise(cahierId);
        notifyStudent(stage, "Document validé",
                "Le cahier de stage " + safeStageTitle(stage) + " a été validé par votre entreprise.");
        return buildCahierValidationItem(stage, cahierStageRepository.findById(cahierId).orElse(cahier));
    }

    private CompanyValidationItemDto rejectStage(Long stageId, String commentaire) {
        Stage stage = getManagedStage(stageId);
        stage.setStatut(StatutStage.REFUSE);
        Stage saved = stageRepository.save(stage);

        notifyStudent(saved, "Stage refusé",
                "Le stage " + safeStageTitle(saved) + " a été refusé par votre entreprise. Motif : " + commentaire);
        notifyAcademicSupervisor(saved, "Stage refusé côté entreprise",
                "Le stage " + safeStageTitle(saved) + " a été refusé par le représentant entreprise. Motif : " + commentaire);

        return buildStageValidationItem(saved);
    }

    private CompanyValidationItemDto rejectSubject(Long stageId, String commentaire) {
        Stage stage = getManagedStage(stageId);
        stage.setStatutSujet(StatutValidation.REFUSEE);
        stage.setStatut(StatutStage.REFUSE);
        Stage saved = stageRepository.save(stage);

        notifyStudent(saved, "Sujet de stage refusé",
                "Le sujet du stage " + safeStageTitle(saved) + " a été refusé par votre entreprise. Motif : " + commentaire);
        notifyAcademicSupervisor(saved, "Sujet de stage refusé côté entreprise",
                "Le sujet du stage " + safeStageTitle(saved) + " a été refusé par le représentant entreprise. Motif : " + commentaire);

        return buildSubjectValidationItem(saved);
    }

    private CompanyValidationItemDto rejectDocument(Long itemId, String commentaire, String documentType) {
        Stage stage = switch (documentType) {
            case "CONVENTION" -> {
                ConventionStage convention = conventionStageRepository.findById(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("Convention introuvable."));
                yield getManagedStage(convention.getStage() != null ? convention.getStage().getId() : null);
            }
            case "CAHIER_STAGE" -> {
                CahierStage cahier = cahierStageRepository.findById(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("Cahier de stage introuvable."));
                yield getManagedStage(cahier.getStage() != null ? cahier.getStage().getId() : null);
            }
            default -> throw new IllegalArgumentException("Document non pris en charge.");
        };

        if (stage.getStatut() == StatutStage.EN_COURS || stage.getStatut() == StatutStage.TERMINE) {
            throw new IllegalStateException("Impossible de refuser ce document pour un stage déjà démarré ou terminé.");
        }

        stage.setStatut(StatutStage.REFUSE);
        Stage saved = stageRepository.save(stage);

        String documentLabel = "CONVENTION".equals(documentType) ? "la convention" : "le cahier de stage";
        notifyStudent(saved, "Document refusé",
                "Votre entreprise a refusé " + documentLabel + " du stage " + safeStageTitle(saved) + ". Motif : " + commentaire);
        notifyAcademicSupervisor(saved, "Document refusé côté entreprise",
                "Le représentant entreprise a refusé " + documentLabel + " du stage " + safeStageTitle(saved) + ". Motif : " + commentaire);

        return "CONVENTION".equals(documentType)
                ? buildConventionValidationItem(saved, conventionStageRepository.findById(itemId).orElseThrow())
                : buildCahierValidationItem(saved, cahierStageRepository.findById(itemId).orElseThrow());
    }

    private Stage getManagedStage(Long stageId) {
        if (stageId == null) {
            throw new IllegalArgumentException("Stage introuvable.");
        }

        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage introuvable."));

        ResponsableEntreprise responsable = getAuthenticatedResponsableEntreprise();
        ensureStageBelongsToCompany(stage, responsable);
        return stage;
    }

    private ResponsableEntreprise getAuthenticatedResponsableEntreprise() {
        Utilisateur authenticatedUser = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifié introuvable."));

        if (authenticatedUser.getRole() != Role.RESPONSABLE_ENTREPRISE) {
            throw new AccessDeniedException("Accès refusé : rôle RESPONSABLE_ENTREPRISE requis.");
        }

        if (authenticatedUser instanceof ResponsableEntreprise responsableEntreprise) {
            return responsableEntreprise;
        }

        return responsableEntrepriseRepository.findByEmailIgnoreCase(authenticatedUser.getEmail())
                .orElseThrow(() -> new AccessDeniedException("Compte représentant entreprise introuvable."));
    }

    private void ensureStageBelongsToCompany(Stage stage, ResponsableEntreprise responsable) {
        Long expectedEntrepriseId = getEntrepriseId(responsable);
        Long stageEntrepriseId = stage.getEntreprise() != null ? stage.getEntreprise().getId() : null;

        if (stageEntrepriseId == null || !stageEntrepriseId.equals(expectedEntrepriseId)) {
            throw new AccessDeniedException("Accès refusé à un élément qui n'appartient pas à votre entreprise.");
        }
    }

    private Long getEntrepriseId(ResponsableEntreprise responsable) {
        Entreprise entreprise = responsable.getEntreprise();
        if (entreprise == null || entreprise.getId() == null) {
            throw new AccessDeniedException("Aucune entreprise n'est rattachée à ce compte.");
        }
        return entreprise.getId();
    }

    private String normalizeType(String itemType) {
        if (itemType == null || itemType.isBlank()) {
            throw new IllegalArgumentException("Le type de validation est obligatoire.");
        }
        return itemType.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private String normalizeComment(String commentaire) {
        if (commentaire == null || commentaire.isBlank()) {
            throw new IllegalArgumentException("Le motif du refus est obligatoire.");
        }
        return commentaire.trim();
    }

    private void notifyStudent(Stage stage, String title, String message) {
        if (stage.getStagiaire() != null && stage.getStagiaire().getId() != null) {
            notificationService.creerNotification(stage.getStagiaire().getId(), title, message, "VALIDATION_ENTREPRISE", stage.getId(), "STAGE");
        }
    }

    private void notifyAcademicSupervisor(Stage stage, String title, String message) {
        if (stage.getEncadrantAcademique() != null && stage.getEncadrantAcademique().getId() != null) {
            notificationService.creerNotification(stage.getEncadrantAcademique().getId(), title, message, "VALIDATION_ENTREPRISE", stage.getId(), "STAGE");
        }
    }

    private CompanyValidationItemDto buildStageValidationItem(Stage stage) {
        String type = stage.getDemandeStage() != null ? "DEMANDE_STAGE" : "STAGE";
        String title = stage.getDemandeStage() != null ? "Demande de stage" : "Stage affecté";
        String description = stage.getDemandeStage() != null
                ? "Validation de la demande liée au stage " + safeStageTitle(stage) + "."
                : "Validation du stage " + safeStageTitle(stage) + ".";

        return baseBuilder(stage, type, title, description, mapStageStatus(stage.getStatut()), stage.getId(), stage.getDemandeStage() != null ? stage.getDemandeStage().getId() : stage.getId())
                .build();
    }

    private CompanyValidationItemDto buildSubjectValidationItem(Stage stage) {
        return baseBuilder(
                stage,
                "SUJET",
                "Sujet de stage",
                "Validation du sujet pour le stage " + safeStageTitle(stage) + ".",
                mapValidationStatus(stage.getStatutSujet()),
                stage.getId(),
                stage.getId()
        ).build();
    }

    private CompanyValidationItemDto buildConventionValidationItem(Stage stage, ConventionStage convention) {
        String status = Boolean.TRUE.equals(convention.getSigneeEntreprise())
                ? "VALIDEE"
                : stage.getStatut() == StatutStage.REFUSE ? "REFUSEE" : "EN_ATTENTE";

        return baseBuilder(
                stage,
                "CONVENTION",
                "Convention de stage",
                "Validation ou signature de la convention de stage.",
                status,
                convention.getId(),
                convention.getId()
        ).build();
    }

    private CompanyValidationItemDto buildCahierValidationItem(Stage stage, CahierStage cahier) {
        String status = Boolean.TRUE.equals(cahier.getSigneeRespEntreprise())
                ? "VALIDEE"
                : stage.getStatut() == StatutStage.REFUSE ? "REFUSEE" : "EN_ATTENTE";

        return baseBuilder(
                stage,
                "CAHIER_STAGE",
                "Cahier de stage",
                "Validation ou signature du cahier de stage.",
                status,
                cahier.getId(),
                cahier.getId()
        ).build();
    }

    private CompanyValidationItemDto.CompanyValidationItemDtoBuilder baseBuilder(
            Stage stage,
            String type,
            String title,
            String description,
            String status,
            Long itemId,
            Long relatedEntityId
    ) {
        return CompanyValidationItemDto.builder()
                .key(type + "-" + itemId)
                .type(type)
                .title(title)
                .description(description)
                .status(status)
                .pending("EN_ATTENTE".equals(status))
                .itemId(itemId)
                .stageId(stage.getId())
                .relatedEntityId(relatedEntityId)
                .stageTitle(safeStageTitle(stage))
                .studentName(buildStudentName(stage))
                .companyName(stage.getEntreprise() != null ? safeText(stage.getEntreprise().getNom()) : "Entreprise")
                .dateDebut(formatDate(stage.getDateDebut()))
                .dateFin(formatDate(stage.getDateFin()));
    }

    private String mapStageStatus(StatutStage statut) {
        if (statut == null || statut == StatutStage.EN_ATTENTE) {
            return "EN_ATTENTE";
        }
        if (statut == StatutStage.REFUSE) {
            return "REFUSEE";
        }
        return "VALIDEE";
    }

    private String mapValidationStatus(StatutValidation statut) {
        if (statut == null || statut == StatutValidation.EN_ATTENTE) {
            return "EN_ATTENTE";
        }
        if (statut == StatutValidation.REFUSEE) {
            return "REFUSEE";
        }
        return "VALIDEE";
    }

    private String formatDate(java.time.LocalDate date) {
        return date != null ? DATE_FORMATTER.format(date) : "";
    }

    private String buildStudentName(Stage stage) {
        if (stage.getStagiaire() == null) {
            return "Stagiaire";
        }
        return safeText((safeText(stage.getStagiaire().getPrenom()) + " " + safeText(stage.getStagiaire().getNom())).trim());
    }

    private String safeStageTitle(Stage stage) {
        return safeText(stage.getTitre() != null && !stage.getTitre().isBlank() ? stage.getTitre() : stage.getSujet());
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "Non renseigné" : value.trim();
    }
}

package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.EnqueteSatisfactionDto;
import fsegs.pfebackendemnagouuiaa.entities.EnqueteSatisfaction;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import fsegs.pfebackendemnagouuiaa.repository.EnqueteSatisfactionRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EnqueteSatisfactionServiceImpl implements EnqueteSatisfactionService {

    private static final String STATUT_DISPONIBLE = "Disponible";
    private static final String STATUT_NON_DISPONIBLE = "Non disponible";
    private static final String MESSAGE_INDISPONIBLE = "Aucune enquête de satisfaction disponible pour le moment.";
    private static final String MESSAGE_URL_INVALIDE = "Le lien de l’enquête est indisponible.";

    private final EnqueteSatisfactionRepository enqueteSatisfactionRepository;
    private final ReunionFinaleRepository reunionFinaleRepository;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public EnqueteSatisfactionDto getConfiguration() {
        return enqueteSatisfactionRepository.findTopByOrderByIdAsc()
                .map(this::toConfigurationDto)
                .orElseGet(this::emptyConfigurationDto);
    }

    @Override
    @Transactional
    public EnqueteSatisfactionDto saveConfiguration(EnqueteSatisfactionDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Les données de l’enquête sont obligatoires.");
        }

        String titre = requireText(dto.getTitre(), "Le titre de l’enquête est obligatoire.");
        String description = requireText(dto.getDescription(), "La description de l’enquête est obligatoire.");
        String url = normalizeUrl(dto.getUrlFormulaire(), "Veuillez saisir une URL http(s) valide.");

        EnqueteSatisfaction enquete = enqueteSatisfactionRepository.findTopByOrderByIdAsc()
                .orElseGet(EnqueteSatisfaction::new);
        enquete.setTitre(titre);
        enquete.setDescription(description);
        enquete.setUrlFormulaire(url);

        return toConfigurationDto(enqueteSatisfactionRepository.save(enquete));
    }

    @Override
    @Transactional(readOnly = true)
    public EnqueteSatisfactionDto getDisponiblePourUtilisateurConnecte() {
        Utilisateur utilisateur = jwtService.getAuthenticatedUtilisateur()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur authentifié introuvable."));

        if (!isSatisfactionActorRole(utilisateur.getRole()) || utilisateur.getId() == null) {
            throw new AccessDeniedException("Accès refusé à l’enquête de satisfaction.");
        }

        Optional<EnqueteSatisfaction> configuredSurvey = enqueteSatisfactionRepository.findTopByOrderByIdAsc();
        if (configuredSurvey.isEmpty()) {
            return unavailable(MESSAGE_INDISPONIBLE);
        }

        EnqueteSatisfaction enquete = configuredSurvey.get();
        if (!isValidHttpUrl(enquete.getUrlFormulaire())) {
            return unavailable(MESSAGE_URL_INVALIDE);
        }

        LocalDate today = LocalDate.now();
        Optional<ReunionFinale> reunionDuJour = reunionFinaleRepository
                .findConcernedByUtilisateurIdAndDate(utilisateur.getId(), today)
                .stream()
                .min(Comparator.comparing(ReunionFinale::getHeure, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ReunionFinale::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        if (reunionDuJour.isEmpty()) {
            EnqueteSatisfactionDto dto = unavailable(MESSAGE_INDISPONIBLE);
            dto.setDateAtteinte(false);
            return dto;
        }

        return toAvailableDto(enquete, reunionDuJour.get());
    }

    private EnqueteSatisfactionDto toConfigurationDto(EnqueteSatisfaction enquete) {
        EnqueteSatisfactionDto dto = new EnqueteSatisfactionDto();
        dto.setEnqueteId(enquete.getId());
        dto.setTitre(enquete.getTitre());
        dto.setDescription(enquete.getDescription());
        dto.setUrlFormulaire(enquete.getUrlFormulaire());
        dto.setDisponible(isValidHttpUrl(enquete.getUrlFormulaire()));
        dto.setStatut(dto.isDisponible() ? STATUT_DISPONIBLE : STATUT_NON_DISPONIBLE);
        return dto;
    }

    private EnqueteSatisfactionDto toAvailableDto(EnqueteSatisfaction enquete, ReunionFinale reunionFinale) {
        EnqueteSatisfactionDto dto = toConfigurationDto(enquete);
        dto.setReunionFinaleId(reunionFinale.getId());
        dto.setDateAtteinte(true);
        dto.setDisponible(true);
        dto.setStatut(STATUT_DISPONIBLE);

        Stage stage = reunionFinale.getStage();
        if (stage != null) {
            dto.setStageId(stage.getId());
            dto.setStageTitre(stage.getTitre());
        }

        return dto;
    }

    private EnqueteSatisfactionDto emptyConfigurationDto() {
        EnqueteSatisfactionDto dto = new EnqueteSatisfactionDto();
        dto.setDisponible(false);
        dto.setStatut(STATUT_NON_DISPONIBLE);
        dto.setMessage(MESSAGE_INDISPONIBLE);
        return dto;
    }

    private EnqueteSatisfactionDto unavailable(String message) {
        EnqueteSatisfactionDto dto = emptyConfigurationDto();
        dto.setMessage(message);
        return dto;
    }

    private String requireText(String value, String message) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeUrl(String rawUrl, String invalidMessage) {
        String normalized = requireText(rawUrl, "L’URL externe du formulaire est obligatoire.");
        if (!isValidHttpUrl(normalized)) {
            throw new IllegalArgumentException(invalidMessage);
        }
        return normalized;
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

    private boolean isSatisfactionActorRole(Role role) {
        return role == Role.STAGIAIRE
                || role == Role.ENCADRANT_ACADEMIQUE
                || role == Role.ENCADRANT_PROFESSIONNEL
                || role == Role.RESPONSABLE_ENTREPRISE;
    }
}

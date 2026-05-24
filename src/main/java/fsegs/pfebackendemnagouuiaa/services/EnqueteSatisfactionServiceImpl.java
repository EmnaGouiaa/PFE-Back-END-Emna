package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.ConfigurerEnqueteRequest;
import fsegs.pfebackendemnagouuiaa.dto.EnqueteSatisfactionDto;
import fsegs.pfebackendemnagouuiaa.entities.ConfigurationGlobaleEnquete;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.repository.ConfigurationGlobaleEnqueteRepository;
import fsegs.pfebackendemnagouuiaa.repository.ReunionFinaleRepository;
import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Règle d'ouverture de l'enquête de satisfaction :
 *
 *   sectionOuverte = (aujourd'hui >= dateFin_stage
 *                  OU aujourd'hui >= date_réunionFinale)
 *                  ET enquête.active = true
 *                  ET urlFormulaire configurée
 *
 * L'URL du formulaire n'est transmise au client que si la section est ouverte.
 */
@Service
@RequiredArgsConstructor
public class EnqueteSatisfactionServiceImpl implements EnqueteSatisfactionService {

    private static final String DEFAULT_TITLE       = "Enquête de satisfaction";
    private static final String DEFAULT_DESCRIPTION = "Merci de répondre à cette enquête de satisfaction.";
    private static final DateTimeFormatter DATE_FR  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ConfigurationGlobaleEnqueteRepository configurationGlobaleEnqueteRepository;
    private final ReunionFinaleRepository               reunionFinaleRepository;
    private final StageRepository                       stageRepository;

    // ─── Lecture configuration globale ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public EnqueteSatisfactionDto getConfiguration() {
        return toConfigDto(getOrCreateGlobalConfig());
    }

    // ─── Enregistrement configuration (RESPONSABLE_STAGE) ───────────────────

    @Override
    @Transactional
    public EnqueteSatisfactionDto saveConfiguration(ConfigurerEnqueteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête ne peut pas être vide.");
        }

        String titre       = normalizeRequired(request.getTitre(),       "Le titre de l'enquête est obligatoire.");
        String description = normalizeRequired(request.getDescription(), "La description de l'enquête est obligatoire.");
        String url         = sanitizeUrl(request.getUrlFormulaire());

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("L'URL externe du formulaire est obligatoire.");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("L'URL doit commencer par http:// ou https://.");
        }

        ConfigurationGlobaleEnquete config = getOrCreateGlobalConfig();
        config.setTitre(titre);
        config.setDescription(description);
        config.setUrlFormulaire(url);
        config = configurationGlobaleEnqueteRepository.save(config);

        return toConfigDto(config);
    }

    // ─── Activer / désactiver (RESPONSABLE_STAGE) ────────────────────────────

    @Override
    @Transactional
    public EnqueteSatisfactionDto toggleActive() {
        ConfigurationGlobaleEnquete config = getOrCreateGlobalConfig();
        config.setActive(!config.isActive());
        config = configurationGlobaleEnqueteRepository.save(config);
        return toConfigDto(config);
    }

    // ─── Etat enquête pour un stage précis (acteurs) ─────────────────────────

    /**
     * Calcule la visibilité de l'enquête pour un stage donné.
     *
     * Règle :  ouvert si (today >= dateFin_stage OU today >= date_réunionFinale)
     *                     ET active ET URL configurée.
     */
    @Override
    @Transactional(readOnly = true)
    public EnqueteSatisfactionDto getForStage(Long stageId) {

        ConfigurationGlobaleEnquete config = getOrCreateGlobalConfig();
        boolean hasUrl = config.getUrlFormulaire() != null && !config.getUrlFormulaire().isBlank();

        // Récupération des données du stage et de sa réunion finale
        // (la réunion finale n'est plus une condition suffisante d'ouverture — la règle
        // métier exige que la date de fin du stage soit atteinte).
        Stage         stage         = stageRepository.findById(stageId).orElse(null);
        ReunionFinale reunionFinale = reunionFinaleRepository.findFirstByStageIdOrderByIdAsc(stageId).orElse(null);

        LocalDate today = LocalDate.now();

        // ── Conditions d'ouverture ──────────────────────────────────────────
        // REGLE METIER : l'enquête ne peut être complétée qu'APRES la fin du stage.
        // Le statut du stage (EN_COURS / TERMINE) et celui de l'enquête restent
        // INDEPENDANTS : compléter l'enquête ne déclenche aucune transition de stage,
        // et inversement la fin du stage n'auto-complète pas l'enquête.
        boolean dateFinAtteinte =
                stage != null
                && stage.getDateFin() != null
                && !today.isBefore(stage.getDateFin());           // today >= dateFin

        boolean sectionOuverte = dateFinAtteinte && config.isActive() && hasUrl;

        // ── Statut et message affichés à l'utilisateur ──────────────────────
        String statut;
        String message;

        if (!hasUrl) {
            statut  = "Non configurée";
            message = "L'enquête de satisfaction n'est pas encore configurée par le responsable des stages.";

        } else if (!config.isActive()) {
            statut  = "Désactivée";
            message = "L'enquête de satisfaction est temporairement désactivée.";

        } else if (stage == null) {
            statut  = "En attente";
            message = "Stage introuvable.";

        } else if (!dateFinAtteinte) {
            // Stage pas encore termine → enquête bloquée avec message exact du spec.
            statut  = "En attente";
            LocalDate dateFin = stage.getDateFin();
            String suffixe = dateFin != null
                    ? " (date de fin prévue : " + dateFin.format(DATE_FR) + ")."
                    : ".";
            message = "L'enquête ne peut être complétée qu'après la fin du stage" + suffixe;

        } else {
            statut  = "Ouverte";
            message = "L'enquête est disponible suite à la fin de votre stage. Merci de compléter le formulaire.";
        }

        // L'URL n'est exposée QUE si la section est ouverte
        String urlVisible   = sectionOuverte ? config.getUrlFormulaire() : "";
        String stageTitre   = stage != null && stage.getTitre() != null ? stage.getTitre() : "";
        Long   rfId         = reunionFinale != null ? reunionFinale.getId() : null;

        return new EnqueteSatisfactionDto(
                rfId,
                stageId,
                stageTitre,
                null,                    // enqueteId — non utilisé (URL externe)
                config.getTitre(),
                config.getDescription(),
                urlVisible,
                statut,
                sectionOuverte,          // disponible
                dateFinAtteinte,         // dateAtteinte (basé uniquement sur la fin de stage)
                message,
                sectionOuverte,          // sectionEnqueteOuverte
                config.isActive(),
                config.getDateModification() != null ? config.getDateModification().toString() : null
        );
    }

    // ─── Helpers privés ─────────────────────────────────────────────────────

    /** Retourne la date la plus proche parmi dateFin et dateReunion (null ignorées). */
    private LocalDate dateLaPlusProche(LocalDate dateFin, LocalDate dateReunion) {
        if (dateFin == null)      return dateReunion;
        if (dateReunion == null)  return dateFin;
        return dateFin.isBefore(dateReunion) ? dateFin : dateReunion;
    }

    /** Libellé correspondant à la date d'ouverture la plus proche. */
    private String labelDateOuverture(LocalDate dateFin, LocalDate dateReunion) {
        if (dateFin == null && dateReunion == null) return "";
        if (dateFin == null)   return "réunion finale";
        if (dateReunion == null) return "fin de stage";
        return dateFin.isBefore(dateReunion) ? "fin de stage" : "réunion finale";
    }

    private ConfigurationGlobaleEnquete getOrCreateGlobalConfig() {
        return configurationGlobaleEnqueteRepository.findById(1L).orElseGet(() -> {
            ConfigurationGlobaleEnquete c = new ConfigurationGlobaleEnquete();
            c.setId(1L);
            c.setTitre(DEFAULT_TITLE);
            c.setDescription(DEFAULT_DESCRIPTION);
            c.setUrlFormulaire(null);
            return configurationGlobaleEnqueteRepository.save(c);
        });
    }

    private EnqueteSatisfactionDto toConfigDto(ConfigurationGlobaleEnquete config) {
        boolean hasUrl = config.getUrlFormulaire() != null && !config.getUrlFormulaire().isBlank();
        return new EnqueteSatisfactionDto(
                null, null, "", null,
                config.getTitre(),
                config.getDescription(),
                hasUrl ? config.getUrlFormulaire() : "",
                hasUrl ? "Configurée" : "Non configurée",
                hasUrl, false, "", false,
                config.isActive(),
                config.getDateModification() != null ? config.getDateModification().toString() : null
        );
    }

    private String sanitizeUrl(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() || "string".equalsIgnoreCase(t) ? null : t;
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }
}

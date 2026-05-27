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
 * Règle d'ouverture / fermeture de l'enquête de satisfaction :
 *
 *   Fenêtre d'ouverture :  dateFin_stage  ≤  today  ≤  dateFin_stage + 6 jours
 *                          ET  enquête.active = true
 *                          ET  urlFormulaire configurée
 *
 *   → Avant dateFin  : statut "En attente"  — enquête non encore accessible.
 *   → Dans la fenêtre : statut "Ouverte"    — URL exposée au client.
 *   → Après + 6 jours : statut "Fermée"     — enquête définitivement close,
 *                                             URL masquée, jamais réouverte.
 *
 * Chaque stage est traité indépendamment : la fin d'un autre stage ne rouvre
 * jamais une enquête déjà fermée.
 *
 * L'URL du formulaire n'est transmise au client que si la section est ouverte.
 */
@Service
@RequiredArgsConstructor
public class EnqueteSatisfactionServiceImpl implements EnqueteSatisfactionService {

    private static final String DEFAULT_TITLE       = "Enquête de satisfaction";
    private static final String DEFAULT_DESCRIPTION = "Merci de répondre à cette enquête de satisfaction.";
    private static final DateTimeFormatter DATE_FR  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Durée de la fenêtre d'enquête en jours APRÈS la date de fin de stage.
     * Valeur 6 → fenêtre = dateFin + 6 jours inclus = 7 jours au total
     * (le jour de fin de stage compte comme jour 1).
     */
    private static final long FENETRE_ENQUETE_JOURS = 6L;

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
     * <p>Fenêtre d'ouverture (par stage, indépendante) :
     * <pre>
     *   dateFin  ≤  today  ≤  dateFin + 6 jours
     * </pre>
     * Avant dateFin       → "En attente"  (enquête pas encore accessible)
     * Dans la fenêtre     → "Ouverte"     (URL exposée si active ET configurée)
     * Après dateFin + 6   → "Fermée"      (URL masquée, jamais réouverte)
     *
     * <p>Chaque stage est traité indépendamment.  La fin d'un autre stage ne
     * rouvre jamais une enquête déjà fermée par expiration de sa fenêtre.
     */
    @Override
    @Transactional(readOnly = true)
    public EnqueteSatisfactionDto getForStage(Long stageId) {

        ConfigurationGlobaleEnquete config = getOrCreateGlobalConfig();
        boolean hasUrl = config.getUrlFormulaire() != null && !config.getUrlFormulaire().isBlank();

        Stage         stage         = stageRepository.findById(stageId).orElse(null);
        ReunionFinale reunionFinale = reunionFinaleRepository.findFirstByStageIdOrderByIdAsc(stageId).orElse(null);

        LocalDate today = LocalDate.now();

        // ── Calcul de la fenêtre d'ouverture propre à ce stage ──────────────
        // REGLE METIER :
        //   • L'enquête s'ouvre le jour de la fin du stage (today >= dateFin).
        //   • Elle reste accessible pendant 7 jours au total (dateFin inclus).
        //   • Après dateFin + 6 jours, elle est définitivement fermée.
        //   • Le statut du stage et celui de l'enquête sont INDEPENDANTS.

        LocalDate dateFin = stage != null ? stage.getDateFin() : null;

        // today >= dateFin  →  la fenêtre a démarré
        boolean dateFinAtteinte =
                dateFin != null && !today.isBefore(dateFin);

        // today <= dateFin + 6  →  on est encore dans la fenêtre des 7 jours
        boolean dansLaFenetre =
                dateFinAtteinte && !today.isAfter(dateFin.plusDays(FENETRE_ENQUETE_JOURS));

        // today > dateFin + 6  →  fenêtre expirée, enquête définitivement fermée
        boolean fenetreExpiree =
                dateFinAtteinte && today.isAfter(dateFin.plusDays(FENETRE_ENQUETE_JOURS));

        // L'enquête est accessible uniquement dans la fenêtre ET si configurée/active
        boolean sectionOuverte = dansLaFenetre && config.isActive() && hasUrl;

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

        } else if (fenetreExpiree) {
            // Fenêtre de 7 jours expirée → clôture définitive, indépendante des autres stages
            statut  = "Fermée";
            message = "La période de l'enquête de satisfaction est clôturée."
                    + " L'enquête était disponible du " + dateFin.format(DATE_FR)
                    + " au " + dateFin.plusDays(FENETRE_ENQUETE_JOURS).format(DATE_FR) + ".";

        } else if (!dateFinAtteinte) {
            // Stage pas encore terminé → enquête bloquée
            String suffixe = dateFin != null
                    ? " (date de fin prévue : " + dateFin.format(DATE_FR) + ")."
                    : ".";
            statut  = "En attente";
            message = "L'enquête ne peut être complétée qu'après la fin du stage" + suffixe;

        } else {
            // Dans la fenêtre → enquête ouverte
            LocalDate dateFermeture = dateFin.plusDays(FENETRE_ENQUETE_JOURS);
            statut  = "Ouverte";
            message = "L'enquête est disponible suite à la fin de votre stage."
                    + " Elle sera accessible jusqu'au " + dateFermeture.format(DATE_FR)
                    + ". Merci de compléter le formulaire.";
        }

        // L'URL n'est exposée QUE si la section est ouverte (dans la fenêtre)
        String urlVisible = sectionOuverte ? config.getUrlFormulaire() : "";
        String stageTitre = stage != null && stage.getTitre() != null ? stage.getTitre() : "";
        Long   rfId       = reunionFinale != null ? reunionFinale.getId() : null;

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
                dateFinAtteinte,         // dateAtteinte (vrai dès que dateFin est atteinte)
                message,
                sectionOuverte,          // sectionEnqueteOuverte
                config.isActive(),
                config.getDateModification() != null ? config.getDateModification().toString() : null
        );
    }

    // ─── Helpers privés ─────────────────────────────────────────────────────

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

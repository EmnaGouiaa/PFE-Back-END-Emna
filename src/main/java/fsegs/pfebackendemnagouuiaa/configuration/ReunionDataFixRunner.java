package fsegs.pfebackendemnagouuiaa.configuration;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Correctifs de données au démarrage pour la table {@code reunion}.
 *
 * <p><b>Rôle :</b> normaliser les valeurs héritées de versions antérieures du modèle
 * (types de réunion, URLs de formulaires placeholder, lien vers le cahier de stage).</p>
 *
 * <p><b>Étapes :</b></p>
 * <ul>
 *   <li>{@link #normalizeMeetingTypes()} — uniformiser {@code type_reunion}.</li>
 *   <li>{@link #cleanupLegacyFormUrls()} — purger les URL vides ou {@code "string"}.</li>
 *   <li>{@link #backfillCahierStageLinks()} — renseigner {@code cahier_stage_id} manquants.</li>
 * </ul>
 *
 * <p><b>Relations :</b> prépare des données cohérentes pour les services de réunion et
 * d'évaluation ; idempotent via UPDATE conditionnels.</p>
 */
@Component
@RequiredArgsConstructor
public class ReunionDataFixRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReunionDataFixRunner.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * Orchestre les trois passes de correction sur les réunions existantes.
     *
     * @param args arguments Spring Boot (non utilisés)
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            normalizeMeetingTypes();
            cleanupLegacyFormUrls();
            backfillCahierStageLinks();
        } catch (Exception exception) {
            log.error("Impossible de corriger automatiquement les donnees reunion", exception);
        }
    }

    /**
     * Harmonise les libellés de type de réunion vers {@code FINALE} ou {@code HEBDOMADAIRE}.
     */
    private void normalizeMeetingTypes() {
        int finaleUpdated = jdbcTemplate.update(
                "UPDATE reunion SET type_reunion = 'FINALE' WHERE UPPER(TRIM(type_reunion)) = 'FINALE' OR TRIM(type_reunion) = 'Finale'"
        );
        int hebdomadaireUpdated = jdbcTemplate.update(
                "UPDATE reunion SET type_reunion = 'HEBDOMADAIRE' WHERE UPPER(TRIM(type_reunion)) IN ('REUNION', 'HEBDOMADAIRE')"
        );
        log.info("Correction type_reunion terminee: finale={}, hebdomadaire={}", finaleUpdated, hebdomadaireUpdated);
    }

    /**
     * Met à NULL les URL de formulaires laissées vides ou avec la valeur placeholder Swagger.
     */
    private void cleanupLegacyFormUrls() {
        int evaluationUpdated = jdbcTemplate.update(
                "UPDATE reunion SET url_form_evaluation = NULL WHERE url_form_evaluation IS NOT NULL AND TRIM(LOWER(url_form_evaluation)) IN ('', 'string')"
        );
        int satisfactionUpdated = jdbcTemplate.update(
                "UPDATE reunion SET url_form_satisfaction = NULL WHERE url_form_satisfaction IS NOT NULL AND TRIM(LOWER(url_form_satisfaction)) IN ('', 'string')"
        );
        log.info("Nettoyage des URLs reunion termine: evaluation={}, satisfaction={}", evaluationUpdated, satisfactionUpdated);
    }

    /**
     * Rattache chaque réunion à son {@code cahier_stage} via {@code stage_id} lorsque le lien est absent.
     */
    private void backfillCahierStageLinks() {
        int linksUpdated = jdbcTemplate.update(
                """
                UPDATE reunion r
                JOIN cahier_stage cs ON cs.stage_id = r.stage_id
                SET r.cahier_stage_id = cs.id
                WHERE r.stage_id IS NOT NULL
                  AND r.cahier_stage_id IS NULL
                """
        );
        log.info("Remplissage cahier_stage_id des reunions termine: {}", linksUpdated);
    }
}

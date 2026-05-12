package fsegs.pfebackendemnagouuiaa.configuration;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReunionDataFixRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReunionDataFixRunner.class);

    private final JdbcTemplate jdbcTemplate;

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

    private void normalizeMeetingTypes() {
        int finaleUpdated = jdbcTemplate.update(
                "UPDATE reunion SET type_reunion = 'FINALE' WHERE UPPER(TRIM(type_reunion)) = 'FINALE' OR TRIM(type_reunion) = 'Finale'"
        );
        int hebdomadaireUpdated = jdbcTemplate.update(
                "UPDATE reunion SET type_reunion = 'HEBDOMADAIRE' WHERE UPPER(TRIM(type_reunion)) IN ('REUNION', 'HEBDOMADAIRE')"
        );
        log.info("Correction type_reunion terminee: finale={}, hebdomadaire={}", finaleUpdated, hebdomadaireUpdated);
    }

    private void cleanupLegacyFormUrls() {
        int evaluationUpdated = jdbcTemplate.update(
                "UPDATE reunion SET url_form_evaluation = NULL WHERE url_form_evaluation IS NOT NULL AND TRIM(LOWER(url_form_evaluation)) IN ('', 'string')"
        );
        int satisfactionUpdated = jdbcTemplate.update(
                "UPDATE reunion SET url_form_satisfaction = NULL WHERE url_form_satisfaction IS NOT NULL AND TRIM(LOWER(url_form_satisfaction)) IN ('', 'string')"
        );
        log.info("Nettoyage des URLs reunion termine: evaluation={}, satisfaction={}", evaluationUpdated, satisfactionUpdated);
    }

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

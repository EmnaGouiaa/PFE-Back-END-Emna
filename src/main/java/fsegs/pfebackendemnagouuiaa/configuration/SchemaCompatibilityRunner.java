package fsegs.pfebackendemnagouuiaa.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class SchemaCompatibilityRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        backfillSurveyColumns();
        backfillSurveyRoles();
        alignNotificationTypeEnum();
        relaxLegacyNotificationUserConstraint();
    }

    private void backfillSurveyColumns() {
        jdbcTemplate.update("""
                update enquete_satisfaction
                set titre = coalesce(nullif(trim(titre), ''), 'Enquete de satisfaction'),
                    description = coalesce(nullif(trim(description), ''), 'Veuillez repondre a l''enquete de satisfaction liee a votre reunion finale.'),
                    url_formulaire = coalesce(url_formulaire, '')
                """);
    }

    private void backfillSurveyRoles() {
        int updated = jdbcTemplate.update("""
                update enquete_satisfaction es
                join utilisateur u on u.id = es.utilisateur_id
                set es.role_repondant = u.role
                where es.role_repondant is null
                """);
        if (updated > 0) {
            log.info("Correction role_repondant terminee pour {} enquete(s) de satisfaction.", updated);
        }
    }

    private void alignNotificationTypeEnum() {
        jdbcTemplate.execute("""
                alter table notification
                modify column type_notification enum(
                    'REUNION_FIXEE',
                    'REUNION_SUIVI',
                    'VALIDATION_ENTREPRISE',
                    'AFFECTATION_ENCADRANT_STAGE',
                    'SIGNATURE_DOSSIER_STAGE',
                    'DEMANDE_ENTREPRISE_VALIDEE',
                    'DEMANDE_ENTREPRISE_REFUSEE',
                    'STAGE_AFFECTE',
                    'ENQUETE_SATISFACTION'
                ) not null
                """);
    }

    private void relaxLegacyNotificationUserConstraint() {
        jdbcTemplate.execute("""
                alter table notification
                modify column utilisateur_id bigint null
                """);
    }
}

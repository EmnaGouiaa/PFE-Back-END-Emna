package fsegs.pfebackendemnagouuiaa.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Exécute au démarrage les migrations de schéma et de données que Hibernate seul ne peut pas appliquer.
 *
 * <p><b>Rôle :</b> éviter les erreurs MySQL « Data truncated » lorsque des colonnes ENUM
 * contiennent encore d'anciennes valeurs (rôles utilisateur renommés, types de notification, etc.).</p>
 *
 * <h3>Pourquoi un {@link CommandLineRunner} plutôt que Flyway/Liquibase ?</h3>
 * <p>Le projet utilise {@code spring.jpa.hibernate.ddl-auto=update}. Le DDL Hibernate s'exécute
 * <em>avant</em> tout {@code CommandLineRunner}. Si des lignes portent encore
 * {@code RESPONSABLE_SERVICE_STAGES}, un {@code ALTER TABLE … MODIFY COLUMN} échoue car MySQL
 * refuse de rétrécir un ENUM tant que des lignes utilisent les anciennes valeurs.</p>
 *
 * <h3>Stratégie en trois temps pour les colonnes ENUM</h3>
 * <ol>
 *   <li><b>Élargir</b> — ajouter la nouvelle valeur ENUM sans retirer les anciennes.</li>
 *   <li><b>Migrer les données</b> — {@code UPDATE} vers la nouvelle valeur métier.</li>
 *   <li><b>Rétrécir</b> — retirer les valeurs obsolètes une fois plus aucune ligne ne les référence.</li>
 * </ol>
 *
 * <p><b>Ordre d'exécution :</b> {@code @Order(0)} — avant {@link DemoDataInitializer} et les seeds.</p>
 * <p>Toutes les étapes sont idempotentes (sans effet si déjà appliquées).</p>
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class SchemaCompatibilityRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    // ── Final enum definitions (must match the Java Role enum exactly) ──────────

    private static final String UTILISATEUR_ROLE_FINAL =
            "'ADMINISTRATEUR','AGENT_STAGE','ENCADRANT_ACADEMIQUE'," +
            "'ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE'," +
            "'RESPONSABLE_STAGE','STAGIAIRE'";

    /** Transitional definition: final values + legacy values that may still be in the DB. */
    private static final String UTILISATEUR_ROLE_EXPANDED =
            "'ADMINISTRATEUR','AGENT_STAGE','ENCADRANT_ACADEMIQUE'," +
            "'ENCADRANT_PROFESSIONNEL','RESPONSABLE_ENTREPRISE'," +
            "'RESPONSABLE_SERVICE_STAGES','RESPONSABLE_UNIVERSITAIRE_STAGES'," +
            "'RESPONSABLE_STAGE','STAGIAIRE'";

    /**
     * Lance les trois passes de compatibilité schéma/données.
     *
     * @param args arguments de ligne de commande Spring Boot (non utilisés)
     */
    @Override
    public void run(String... args) {
        backfillLegacyManagerRole();
        alignNotificationTypeEnum();
        relaxLegacyNotificationUserConstraint();
        addWeeklyMeetingObservationColumns();
    }

    /**
     * Migre les utilisateurs dont le rôle a été renommé vers {@code RESPONSABLE_STAGE}.
     *
     * <p>Applique la séquence élargir → UPDATE → rétrécir sur {@code utilisateur.role}.</p>
     */
    private void backfillLegacyManagerRole() {
        // ── utilisateur.role ─────────────────────────────────────────────────────

        // Step 1 – Expand: add RESPONSABLE_STAGE while keeping the old values.
        //           Succeeds even when old rows are present (only adding, not removing).
        silentAlter(
                "ALTER TABLE utilisateur MODIFY COLUMN role ENUM(" + UTILISATEUR_ROLE_EXPANDED + ") NOT NULL",
                "utilisateur.role expand (add RESPONSABLE_STAGE)"
        );

        // Step 2 – Migrate: RESPONSABLE_STAGE is now a valid enum member → no truncation.
        int utilisateursUpdated = jdbcTemplate.update("""
                UPDATE utilisateur
                SET role = 'RESPONSABLE_STAGE'
                WHERE role IN ('RESPONSABLE_SERVICE_STAGES', 'RESPONSABLE_UNIVERSITAIRE_STAGES')
                """);
        if (utilisateursUpdated > 0) {
            log.info("[SchemaRunner] Migration des roles legacy terminee pour {} utilisateur(s).", utilisateursUpdated);
        }

        // Step 3 – Shrink: remove old values now that no row references them any more.
        //           Also prevents the Hibernate startup WARN about mismatched enum definition.
        silentAlter(
                "ALTER TABLE utilisateur MODIFY COLUMN role ENUM(" + UTILISATEUR_ROLE_FINAL + ") NOT NULL",
                "utilisateur.role shrink (remove legacy values)"
        );

    }

    /**
     * Convertit {@code notifications.type_notification} de ENUM vers VARCHAR(100)
     * ou élargit un VARCHAR trop court.
     */
    private void alignNotificationTypeEnum() {
        try {
            String dataType = jdbcTemplate.queryForObject(
                    """
                    SELECT DATA_TYPE
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name   = 'notifications'
                      AND column_name  = 'type_notification'
                    """,
                    String.class
            );
            if ("enum".equalsIgnoreCase(dataType)) {
                jdbcTemplate.execute(
                        "ALTER TABLE notifications MODIFY COLUMN type_notification VARCHAR(100) NOT NULL"
                );
                log.info("[SchemaRunner] Colonne notifications.type_notification convertie de ENUM vers VARCHAR(100).");
            } else if ("varchar".equalsIgnoreCase(dataType)) {
                Integer charLen = jdbcTemplate.queryForObject(
                        """
                        SELECT CHARACTER_MAXIMUM_LENGTH
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name   = 'notifications'
                          AND column_name  = 'type_notification'
                        """,
                        Integer.class
                );
                if (charLen != null && charLen < 100) {
                    jdbcTemplate.execute(
                            "ALTER TABLE notifications MODIFY COLUMN type_notification VARCHAR(100) NOT NULL"
                    );
                    log.info("[SchemaRunner] Colonne notifications.type_notification elargie a VARCHAR(100) (etait VARCHAR({})).", charLen);
                }
            }
        } catch (Exception ex) {
            log.warn("[SchemaRunner] Impossible d'aligner notifications.type_notification automatiquement: {}", ex.getMessage());
        }
    }

    /**
     * Autorise {@code notifications.utilisateur_id} NULL pour les notifications système globales.
     */
    private void relaxLegacyNotificationUserConstraint() {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE notifications
                    MODIFY COLUMN utilisateur_id BIGINT NULL
                    """);
        } catch (Exception ex) {
            log.debug("[SchemaRunner] relaxLegacyNotificationUserConstraint ignoree: {}", ex.getMessage());
        }
    }

    /**
     * Exécute un DDL en ignorant silencieusement les erreurs (migration déjà appliquée).
     *
     * @param sql   instruction ALTER TABLE
     * @param label libellé pour les logs DEBUG
     */
    /**
     * Colonnes d'observations séparées par encadrant sur les réunions hebdomadaires.
     */
    private void addWeeklyMeetingObservationColumns() {
        silentAlter(
                "ALTER TABLE reunion ADD COLUMN observation_encadrant_academique VARCHAR(5000) NULL",
                "reunion.observation_encadrant_academique add"
        );
        silentAlter(
                "ALTER TABLE reunion ADD COLUMN observation_encadrant_professionnel VARCHAR(5000) NULL",
                "reunion.observation_encadrant_professionnel add"
        );

        try {
            int migratedAcademic = jdbcTemplate.update("""
                    UPDATE reunion
                    SET observation_encadrant_academique = observation
                    WHERE observation IS NOT NULL
                      AND TRIM(observation) <> ''
                      AND UPPER(TRIM(type_reunion)) = 'HEBDOMADAIRE'
                      AND UPPER(TRIM(type_encadrant_createur)) = 'ACADEMIQUE'
                      AND (observation_encadrant_academique IS NULL OR TRIM(observation_encadrant_academique) = '')
                    """);
            int migratedProfessional = jdbcTemplate.update("""
                    UPDATE reunion
                    SET observation_encadrant_professionnel = observation
                    WHERE observation IS NOT NULL
                      AND TRIM(observation) <> ''
                      AND UPPER(TRIM(type_reunion)) = 'HEBDOMADAIRE'
                      AND (observation_encadrant_professionnel IS NULL OR TRIM(observation_encadrant_professionnel) = '')
                      AND (
                          UPPER(TRIM(type_encadrant_createur)) = 'PROFESSIONNEL'
                          OR type_encadrant_createur IS NULL
                          OR TRIM(type_encadrant_createur) = ''
                      )
                    """);
            if (migratedAcademic > 0 || migratedProfessional > 0) {
                log.info(
                        "[SchemaRunner] Observations hebdomadaires migrees: academique={}, professionnel={}.",
                        migratedAcademic,
                        migratedProfessional
                );
            }
        } catch (Exception ex) {
            log.warn("[SchemaRunner] Migration observations hebdomadaires ignoree: {}", ex.getMessage());
        }
    }

    private void silentAlter(String sql, String label) {
        try {
            jdbcTemplate.execute(sql);
            log.debug("[SchemaRunner] OK: {}", label);
        } catch (Exception ex) {
            log.debug("[SchemaRunner] Ignoree (deja appliquee ou non applicable) — {}: {}", label, ex.getMessage());
        }
    }
}

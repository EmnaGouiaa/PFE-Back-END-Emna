package fsegs.pfebackendemnagouuiaa.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Handles schema and data migrations that cannot be expressed as pure Hibernate DDL.
 *
 * <h3>Why a CommandLineRunner and not a Flyway/Liquibase migration?</h3>
 * The project uses {@code spring.jpa.hibernate.ddl-auto=update}.  Hibernate's DDL updater
 * runs <em>before</em> any {@code CommandLineRunner} / {@code ApplicationRunner} bean.
 * When the database still contains rows whose ENUM column values belong to a renamed role
 * (e.g. {@code RESPONSABLE_SERVICE_STAGES}), Hibernate's {@code ALTER TABLE … MODIFY COLUMN}
 * fails with <em>"Data truncated"</em> — MySQL refuses to narrow an ENUM that still has rows
 * using the old values.
 *
 * <h3>Correct 3-step migration for ENUM columns</h3>
 * <ol>
 *   <li><b>Expand</b> – {@code ALTER TABLE} to add the new value alongside the old ones.
 *       MySQL accepts this because we are only <em>adding</em>, never removing.</li>
 *   <li><b>Migrate data</b> – {@code UPDATE} rows from old value to new value.
 *       Now the new value is a valid enum member, so no truncation.</li>
 *   <li><b>Shrink</b> – {@code ALTER TABLE} to remove the old values.
 *       MySQL accepts this because no row uses those values any more.
 *       Hibernate will also attempt this on startup via {@code ddl-auto=update},
 *       but doing it here prevents the startup WARN.</li>
 * </ol>
 *
 * <p>All three steps are idempotent — safe to run on every startup.</p>
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

    @Override
    public void run(String... args) {
        backfillLegacyManagerRole();
        alignNotificationTypeEnum();
        relaxLegacyNotificationUserConstraint();
    }

    /**
     * Migrates users whose {@code role} was renamed from the old
     * {@code RESPONSABLE_SERVICE_STAGES} / {@code RESPONSABLE_UNIVERSITAIRE_STAGES}
     * to the current {@code RESPONSABLE_STAGE}.
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
     * Executes a DDL statement and swallows any exception, logging at DEBUG level.
     * Used for idempotent ALTERs that are safe to skip if the schema is already in the
     * target state (e.g. when running for the second time after a successful migration).
     */
    private void silentAlter(String sql, String label) {
        try {
            jdbcTemplate.execute(sql);
            log.debug("[SchemaRunner] OK: {}", label);
        } catch (Exception ex) {
            log.debug("[SchemaRunner] Ignoree (deja appliquee ou non applicable) — {}: {}", label, ex.getMessage());
        }
    }
}

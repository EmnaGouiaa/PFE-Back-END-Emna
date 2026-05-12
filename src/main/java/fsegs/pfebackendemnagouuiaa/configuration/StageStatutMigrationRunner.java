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
public class StageStatutMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StageStatutMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            if (databaseName == null || databaseName.isBlank()) {
                log.warn("Migration stage.statut ignoree: base courante introuvable.");
                return;
            }

            ColumnMetadata metadata = jdbcTemplate.query(
                    """
                    SELECT DATA_TYPE, IS_NULLABLE
                    FROM information_schema.columns
                    WHERE table_schema = ?
                      AND table_name = 'stage'
                      AND column_name = 'statut'
                    """,
                    rs -> {
                        if (!rs.next()) {
                            return null;
                        }
                        return new ColumnMetadata(
                                rs.getString("DATA_TYPE"),
                                rs.getString("IS_NULLABLE")
                        );
                    },
                    databaseName
            );

            if (metadata == null) {
                log.warn("Migration stage.statut ignoree: colonne introuvable.");
                return;
            }

            if ("enum".equalsIgnoreCase(metadata.dataType())) {
                String nullabilityClause = "NO".equalsIgnoreCase(metadata.isNullable()) ? "NOT NULL" : "NULL";
                jdbcTemplate.execute("ALTER TABLE stage MODIFY COLUMN statut VARCHAR(32) " + nullabilityClause);
                log.info("Colonne stage.statut convertie de ENUM vers VARCHAR(32).");
            }

            jdbcTemplate.update("UPDATE stage SET statut = 'PAS_COMMENCE' WHERE statut IN ('EN_ATTENTE', 'VALIDE_PAR_ENTREPRISE', 'VALIDE_PAR_RESPONSABLE')");
            jdbcTemplate.update("UPDATE stage SET statut = 'EN_COURS' WHERE statut = 'ACTIF'");
            jdbcTemplate.update("UPDATE stage SET statut = 'ANNULE' WHERE statut = 'ANNULEE'");
            log.info("Migration des anciennes valeurs de stage.statut terminee.");
        } catch (Exception exception) {
            log.error("Impossible d'aligner automatiquement les valeurs de stage.statut", exception);
        }
    }

    private record ColumnMetadata(String dataType, String isNullable) {
    }
}

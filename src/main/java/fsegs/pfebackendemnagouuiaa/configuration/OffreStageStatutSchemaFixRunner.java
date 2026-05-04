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
public class OffreStageStatutSchemaFixRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OffreStageStatutSchemaFixRunner.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            if (databaseName == null || databaseName.isBlank()) {
                log.warn("Correction schema offre_stage.statut ignoree: base courante introuvable.");
                return;
            }

            ColumnMetadata metadata = jdbcTemplate.query(
                    """
                    SELECT DATA_TYPE, IS_NULLABLE
                    FROM information_schema.columns
                    WHERE table_schema = ?
                      AND table_name = 'offre_stage'
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
                log.warn("Correction schema offre_stage.statut ignoree: colonne introuvable.");
                return;
            }

            log.info("Colonne offre_stage.statut detectee: type={}, nullable={}", metadata.dataType(), metadata.isNullable());

            if (!"enum".equalsIgnoreCase(metadata.dataType())) {
                return;
            }

            String nullabilityClause = "NO".equalsIgnoreCase(metadata.isNullable()) ? "NOT NULL" : "NULL";
            jdbcTemplate.execute("ALTER TABLE offre_stage MODIFY COLUMN statut VARCHAR(32) " + nullabilityClause);
            log.info("Colonne offre_stage.statut convertie de ENUM vers VARCHAR(32).");
        } catch (Exception exception) {
            log.error("Impossible d'aligner automatiquement le schema de offre_stage.statut", exception);
        }
    }

    private record ColumnMetadata(String dataType, String isNullable) {
    }
}

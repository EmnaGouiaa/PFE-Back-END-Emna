package fsegs.pfebackendemnagouuiaa.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegacySchemaFixRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE `user` MODIFY COLUMN `entreprise` BIGINT NULL");
            log.info("Schema fix applied: user.entreprise is now nullable.");
        } catch (Exception ex) {
            // Safe fallback for environments where schema is already correct or differs.
            log.info("Schema fix skipped (already applied or not needed): {}", ex.getMessage());
        }
    }
}

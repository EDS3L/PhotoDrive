package pl.photodrive.core.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaFixer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        dropLegacyColumnIfPresent("albums", "isPublic");
        widenVarcharColumnIfNeeded("passwordTokens", "token", 64);
    }

    private void dropLegacyColumnIfPresent(String table, String column) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, table, column);
            if (count != null && count > 0) {
                log.warn("Dropping legacy column {}.{}", table, column);
                jdbcTemplate.execute("ALTER TABLE `" + table + "` DROP COLUMN `" + column + "`");
            }
        } catch (Exception e) {
            log.error("Failed to drop legacy column {}.{}: {}", table, column, e.getMessage());
        }
    }

    private void widenVarcharColumnIfNeeded(String table, String column, int minLength) {
        try {
            Integer currentLength = jdbcTemplate.queryForObject(
                    "SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, table, column);
            if (currentLength != null && currentLength < minLength) {
                log.warn("Widening column {}.{} to VARCHAR({})", table, column, minLength);
                jdbcTemplate.execute("ALTER TABLE `" + table + "` MODIFY COLUMN `" + column + "` VARCHAR(" + minLength + ") NOT NULL");
            }
        } catch (Exception e) {
            log.error("Failed to widen column {}.{}: {}", table, column, e.getMessage());
        }
    }
}

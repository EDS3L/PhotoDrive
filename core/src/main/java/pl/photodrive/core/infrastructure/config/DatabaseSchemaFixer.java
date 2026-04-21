package pl.photodrive.core.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-shot, idempotent database clean-up executed on application start-up.
 *
 * <p>Earlier versions of the {@code albums} table used the camelCase column
 * name {@code isPublic} (implicitly created by Hibernate before an explicit
 * {@code @Column(name = "is_public")} mapping was introduced). With
 * {@code spring.jpa.hibernate.ddl-auto=update}, Hibernate happily adds the new
 * {@code is_public} column but never removes the old one, and because the
 * legacy column is {@code NOT NULL} without a default, every INSERT fails with
 * {@code Field 'isPublic' doesn't have a default value}.
 *
 * <p>This runner removes that legacy column if (and only if) it is still
 * present, so the schema heals itself the first time the new code is
 * deployed — and it is a no-op on every subsequent start-up and on a brand new
 * database.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaFixer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        dropLegacyColumnIfPresent("albums", "isPublic");
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
}

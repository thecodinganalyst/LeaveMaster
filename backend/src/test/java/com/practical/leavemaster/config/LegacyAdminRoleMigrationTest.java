package com.practical.leavemaster.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyAdminRoleMigrationTest {

    private static final String MIGRATION_LOCATION = "classpath:db/migration/h2";

    @Test
    void freshDatabaseDoesNotContainLegacyAdminRole() throws Exception {
        String url = databaseUrl("fresh");

        flyway(url).migrate();

        assertEquals(0, count(url, "SELECT COUNT(*) FROM app_role WHERE id = 'ADMIN'"));
        assertEquals(0, count(url, "SELECT COUNT(*) FROM app_user_role WHERE role_id = 'ADMIN'"));
        assertEquals(0, count(url, "SELECT COUNT(*) FROM app_role_permission WHERE role_id = 'ADMIN'"));
    }

    @Test
    void existingDatabaseRemovesLegacyAdminInForeignKeySafeOrder() throws Exception {
        String url = databaseUrl("upgrade");

        Flyway preCleanupFlyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations(MIGRATION_LOCATION)
                .target(MigrationVersion.fromVersion("10"))
                .load();
        preCleanupFlyway.migrate();

        assertEquals(1, count(url, "SELECT COUNT(*) FROM app_role WHERE id = 'ADMIN'"));
        assertTrue(count(url, "SELECT COUNT(*) FROM app_role_permission WHERE role_id = 'ADMIN'") > 0);

        flyway(url).migrate();

        assertEquals(0, count(url, "SELECT COUNT(*) FROM app_role WHERE id = 'ADMIN'"));
        assertEquals(0, count(url, "SELECT COUNT(*) FROM app_user_role WHERE role_id = 'ADMIN'"));
        assertEquals(0, count(url, "SELECT COUNT(*) FROM app_role_permission WHERE role_id = 'ADMIN'"));
    }

    private Flyway flyway(String url) {
        return Flyway.configure()
                .dataSource(url, "sa", "")
                .locations(MIGRATION_LOCATION)
                .load();
    }

    private long count(String url, String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private String databaseUrl(String suffix) {
        return "jdbc:h2:mem:legacy-admin-" + suffix + ";DB_CLOSE_DELAY=-1";
    }
}

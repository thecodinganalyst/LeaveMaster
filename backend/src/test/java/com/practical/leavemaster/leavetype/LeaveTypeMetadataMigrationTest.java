package com.practical.leavemaster.leavetype;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LeaveTypeMetadataMigrationTest {

    @Test
    void shouldBackfillExistingSourceLinkedLeaveTypeWhenUpgradingFromV32ToV33() throws Exception {
        String databaseName = "leave-type-metadata-" + UUID.randomUUID();
        String url = "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1";

        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration/h2")
                .target(MigrationVersion.fromVersion("32"))
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO jurisdiction_leave_type (
                        id, jurisdiction_id, code, name, description, statutory, paid, active,
                        source_url, source_name, effective_from, effective_to
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setString(1, "SG:TEST_METADATA_BACKFILL");
                statement.setString(2, "SG");
                statement.setString(3, "TEST_METADATA_BACKFILL");
                statement.setString(4, "Test Metadata Backfill");
                statement.setString(5, "Migration test source");
                statement.setBoolean(6, true);
                statement.setBoolean(7, true);
                statement.setBoolean(8, false);
                statement.setString(9, "https://example.com/test-leave");
                statement.setString(10, "Test Source");
                statement.setObject(11, LocalDate.of(2026, 1, 1));
                statement.setObject(12, LocalDate.of(2026, 12, 31));
                statement.executeUpdate();
            }

            try (var statement = connection.prepareStatement("""
                    INSERT INTO leave_type (id, name, used, source_jurisdiction_leave_type_id)
                    VALUES (?, ?, ?, ?)
                    """)) {
                statement.setString(1, "tenant:test-metadata-backfill");
                statement.setString(2, "Tenant Test Leave");
                statement.setBoolean(3, false);
                statement.setString(4, "SG:TEST_METADATA_BACKFILL");
                statement.executeUpdate();
            }
        }

        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration/h2")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.prepareStatement("""
                     SELECT active, statutory, paid, source_name, source_url, effective_from, effective_to
                     FROM leave_type
                     WHERE id = ?
                     """)) {
            statement.setString(1, "tenant:test-metadata-backfill");
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getBoolean("active")).isFalse();
                assertThat(result.getBoolean("statutory")).isTrue();
                assertThat(result.getBoolean("paid")).isTrue();
                assertThat(result.getString("source_name")).isEqualTo("Test Source");
                assertThat(result.getString("source_url")).isEqualTo("https://example.com/test-leave");
                assertThat(result.getObject("effective_from", LocalDate.class)).isEqualTo(LocalDate.of(2026, 1, 1));
                assertThat(result.getObject("effective_to", LocalDate.class)).isEqualTo(LocalDate.of(2026, 12, 31));
            }
        }
    }
}

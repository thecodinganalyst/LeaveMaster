package com.practical.leavemaster.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantLeaveTypeJurisdictionMigrationTest {

    @Test
    void v64AllowsTenantPolicyJurisdictionBackfill() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:v64-tenant-policy;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "")) {
            createPreV64Schema(connection);
            seedLegacyTenantPolicy(connection);

            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/h2/V64__scope_tenant_leave_types_by_jurisdiction.sql"));

            assertThat(singleValue(connection,
                    "SELECT jurisdiction_id FROM leave_type WHERE id = 'acme:ANNUAL_LEAVE'"))
                    .isEqualTo("SG");
            assertThat(singleValue(connection,
                    "SELECT jurisdiction_id FROM leave_entitlement_policy WHERE id = 'tenant-policy'"))
                    .isEqualTo("SG");

            // The replacement scope constraint still rejects malformed platform templates.
            assertThatThrownBy(() -> execute(connection, """
                    INSERT INTO leave_entitlement_policy
                        (id, scope, tenant_id, leave_type_id, jurisdiction_id, jurisdiction_leave_type_id)
                    VALUES ('bad-template', 'PLATFORM_TEMPLATE', NULL, NULL, NULL, 'SG:ANNUAL_LEAVE')
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }

    private void createPreV64Schema(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE tenant_jurisdiction (
                    tenant_id VARCHAR(255) NOT NULL,
                    jurisdiction_id VARCHAR(32) NOT NULL
                )
                """);
        execute(connection, """
                CREATE TABLE jurisdiction_leave_type (
                    id VARCHAR(128) PRIMARY KEY,
                    jurisdiction_id VARCHAR(32) NOT NULL
                )
                """);
        execute(connection, """
                CREATE TABLE leave_type (
                    id VARCHAR(255) PRIMARY KEY,
                    tenant_id VARCHAR(255) NOT NULL,
                    source_jurisdiction_leave_type_id VARCHAR(128)
                )
                """);
        execute(connection, """
                CREATE TABLE leave_entitlement_policy (
                    id VARCHAR(255) PRIMARY KEY,
                    scope VARCHAR(32) NOT NULL,
                    tenant_id VARCHAR(255),
                    leave_type_id VARCHAR(255),
                    jurisdiction_id VARCHAR(32),
                    jurisdiction_leave_type_id VARCHAR(128),
                    CONSTRAINT CK_leave_entitlement_policy_scope CHECK (
                        (scope = 'PLATFORM_TEMPLATE' AND tenant_id IS NULL AND leave_type_id IS NULL
                            AND jurisdiction_id IS NOT NULL AND jurisdiction_leave_type_id IS NOT NULL)
                        OR
                        (scope = 'TENANT' AND tenant_id IS NOT NULL AND leave_type_id IS NOT NULL
                            AND jurisdiction_id IS NULL AND jurisdiction_leave_type_id IS NULL)
                    )
                )
                """);
    }

    private void seedLegacyTenantPolicy(Connection connection) throws SQLException {
        execute(connection, "INSERT INTO tenant_jurisdiction VALUES ('acme', 'SG')");
        execute(connection, "INSERT INTO jurisdiction_leave_type VALUES ('SG:ANNUAL_LEAVE', 'SG')");
        execute(connection, "INSERT INTO leave_type VALUES ('acme:ANNUAL_LEAVE', 'acme', 'SG:ANNUAL_LEAVE')");
        execute(connection, """
                INSERT INTO leave_entitlement_policy
                    (id, scope, tenant_id, leave_type_id, jurisdiction_id, jurisdiction_leave_type_id)
                VALUES ('tenant-policy', 'TENANT', 'acme', 'acme:ANNUAL_LEAVE', NULL, NULL)
                """);
    }

    private String singleValue(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}

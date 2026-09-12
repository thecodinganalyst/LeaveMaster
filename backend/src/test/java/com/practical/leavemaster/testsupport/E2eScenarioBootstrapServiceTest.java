package com.practical.leavemaster.testsupport;

import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.tenant.TenantRepository;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("e2e")
class E2eScenarioBootstrapServiceTest {

    @Autowired private E2eScenarioBootstrapService bootstrapService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private StaffRepository staffRepository;
    @Autowired private AppUserRepository appUserRepository;

    @AfterEach
    void cleanup() {
        bootstrapService.deleteScenario("bootstrap-test");
        bootstrapService.deleteScenario("bootstrap-second");
    }

    @Test
    void createsPersistedStandardScenarioAndReturnsLoginMetadata() {
        var result = bootstrapService.createStandardSingaporeScenario(
                "bootstrap-test", LocalDate.of(2026, 9, 12));

        assertThat(result.scenarioId()).isEqualTo("bootstrap-test");
        assertThat(result.tenantId()).isEqualTo("E2E-bootstrap-test");
        assertThat(result.password()).isEqualTo("e2e-password");
        assertThat(result.users()).containsKeys("admin", "hr", "manager01", "manager02", "staff001");
        assertThat(result.users().get("staff001").staffId()).isEqualTo("E2E-bootstrap-test-staff001");
        assertThat(tenantRepository.existsById(result.tenantId())).isTrue();
        assertThat(staffRepository.findAllByTenantId(result.tenantId())).hasSize(9);
        assertThat(appUserRepository.findAll().stream()
                .filter(user -> result.tenantId().equals(user.getTenantId())))
                .hasSize(9);
    }

    @Test
    void uniqueScenarioIdsCreateIsolatedTenantsAndDeleteOnlyRequestedScenario() {
        var first = bootstrapService.createStandardSingaporeScenario(
                "bootstrap-test", LocalDate.of(2026, 9, 12));
        var second = bootstrapService.createStandardSingaporeScenario(
                "bootstrap-second", LocalDate.of(2026, 9, 12));

        assertThat(first.tenantId()).isNotEqualTo(second.tenantId());

        bootstrapService.deleteScenario(first.scenarioId());

        assertThat(tenantRepository.existsById(first.tenantId())).isFalse();
        assertThat(tenantRepository.existsById(second.tenantId())).isTrue();
    }
}

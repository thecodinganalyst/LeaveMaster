package com.practical.leavemaster.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TenantRepositoryTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    @Test
    void shouldSaveAndFindTenant() {
        Tenant tenant = tenant("tenant-1", "Acme Corp", TenantStatus.ACTIVE);
        tenant.setStartDate(LocalDate.of(2024, 1, 1));
        tenantRepository.save(tenant);

        Optional<Tenant> found = tenantRepository.findById("tenant-1");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Acme Corp");
        assertThat(found.get().getJurisdictionId()).isEqualTo("SG");
        assertThat(found.get().getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(found.get().getEndDate()).isNull();
        assertThat(found.get().getLastModified()).isNotNull();
    }

    @Test
    void shouldSaveTenantWithEndDate() {
        Tenant tenant = tenant("tenant-2", "Beta Ltd", TenantStatus.TERMINATED);
        tenant.setStartDate(LocalDate.of(2024, 1, 1));
        tenant.setEndDate(LocalDate.of(2025, 12, 31));
        tenantRepository.save(tenant);

        Optional<Tenant> found = tenantRepository.findById("tenant-2");
        assertThat(found).isPresent();
        assertThat(found.get().getEndDate()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(found.get().getStatus()).isEqualTo(TenantStatus.TERMINATED);
    }

    @Test
    void shouldFindAllTenants() {
        tenantRepository.save(tenant("t1", "Tenant 1", TenantStatus.ACTIVE));
        tenantRepository.save(tenant("t2", "Tenant 2", TenantStatus.DORMANT));
        assertThat(tenantRepository.findAll()).hasSize(2);
    }

    @Test
    void shouldDeleteTenant() {
        tenantRepository.save(tenant("t1", "Tenant 1", TenantStatus.ACTIVE));
        tenantRepository.deleteById("t1");
        assertThat(tenantRepository.findById("t1")).isEmpty();
    }

    @Test
    void shouldFindActiveTenantsInactiveForMoreThanAMonth() {
        tenantRepository.save(tenant("t1", "Tenant 1", TenantStatus.ACTIVE));
        tenantRepository.save(tenant("t2", "Tenant 2", TenantStatus.ACTIVE));
        tenantRepository.save(tenant("t3", "Tenant 3", TenantStatus.DORMANT));

        tenantRepository.updateLastModified("t1", LocalDateTime.now().minusMonths(2));
        tenantRepository.updateLastModified("t2", LocalDateTime.now().minusDays(10));
        tenantRepository.updateLastModified("t3", LocalDateTime.now().minusMonths(2));
        entityManager.clear();

        List<Tenant> dormantCandidates = tenantRepository.findAllByStatusAndLastModifiedBefore(
                TenantStatus.ACTIVE, LocalDateTime.now().minusMonths(1));
        assertThat(dormantCandidates).extracting(Tenant::getId).containsExactly("t1");
    }

    private Tenant tenant(String id, String name, TenantStatus status) {
        return Tenant.builder()
                .id(id)
                .name(name)
                .jurisdictionId("SG")
                .startDate(LocalDate.now())
                .status(status)
                .build();
    }
}

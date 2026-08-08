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

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    void shouldSaveAndFindTenant() {
        Tenant tenant = Tenant.builder()
                .id("tenant-1")
                .name("Acme Corp")
                .startDate(LocalDate.of(2024, 1, 1))
                .status(TenantStatus.ACTIVE)
                .build();

        tenantRepository.save(tenant);

        Optional<Tenant> found = tenantRepository.findById("tenant-1");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Acme Corp");
        assertThat(found.get().getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(found.get().getEndDate()).isNull();
        assertThat(found.get().getLastModified()).isNotNull();
    }

    @Test
    void shouldSaveTenantWithEndDate() {
        Tenant tenant = Tenant.builder()
                .id("tenant-2")
                .name("Beta Ltd")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .status(TenantStatus.TERMINATED)
                .build();

        tenantRepository.save(tenant);

        Optional<Tenant> found = tenantRepository.findById("tenant-2");
        assertThat(found).isPresent();
        assertThat(found.get().getEndDate()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(found.get().getStatus()).isEqualTo(TenantStatus.TERMINATED);
    }

    @Test
    void shouldFindAllTenants() {
        tenantRepository.save(Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).build());
        tenantRepository.save(Tenant.builder().id("t2").name("Tenant 2").startDate(LocalDate.now()).status(TenantStatus.DORMANT).build());

        List<Tenant> all = tenantRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    void shouldDeleteTenant() {
        tenantRepository.save(Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).build());
        tenantRepository.deleteById("t1");
        assertThat(tenantRepository.findById("t1")).isEmpty();
    }

    @Test
    void shouldFindActiveTenantsInactiveForMoreThanAMonth() {
        tenantRepository.save(Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).build());
        tenantRepository.save(Tenant.builder().id("t2").name("Tenant 2").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).build());
        tenantRepository.save(Tenant.builder().id("t3").name("Tenant 3").startDate(LocalDate.now()).status(TenantStatus.DORMANT).build());

        tenantRepository.updateLastModified("t1", LocalDateTime.now().minusMonths(2));
        tenantRepository.updateLastModified("t2", LocalDateTime.now().minusDays(10));
        tenantRepository.updateLastModified("t3", LocalDateTime.now().minusMonths(2));
        entityManager.clear();

        List<Tenant> dormantCandidates = tenantRepository.findAllByStatusAndLastModifiedBefore(
                TenantStatus.ACTIVE, LocalDateTime.now().minusMonths(1));

        assertThat(dormantCandidates).extracting(Tenant::getId).containsExactly("t1");
    }
}

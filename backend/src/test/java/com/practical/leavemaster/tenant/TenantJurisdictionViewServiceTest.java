package com.practical.leavemaster.tenant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantJurisdictionViewServiceTest {

    @Mock
    private TenantJurisdictionRepository tenantJurisdictionRepository;

    @InjectMocks
    private TenantJurisdictionViewService service;

    @Test
    void shouldExposeEveryAssignedJurisdictionInSortedRepositoryOrder() {
        Tenant tenant = Tenant.builder().id("acme").jurisdictionId("SG").build();
        when(tenantJurisdictionRepository.findAllByTenantIdOrderByJurisdictionIdAsc("acme"))
                .thenReturn(List.of(
                        TenantJurisdiction.builder().tenantId("acme").jurisdictionId("AU-NSW").build(),
                        TenantJurisdiction.builder().tenantId("acme").jurisdictionId("SG").build()));

        Tenant result = service.enrich(tenant);

        assertThat(result.getJurisdictionIds()).containsExactly("AU-NSW", "SG");
    }

    @Test
    void shouldFallBackToLegacyJurisdictionWhenNoAssociationExists() {
        Tenant tenant = Tenant.builder().id("legacy").jurisdictionId("SG").build();
        when(tenantJurisdictionRepository.findAllByTenantIdOrderByJurisdictionIdAsc("legacy"))
                .thenReturn(List.of());

        Tenant result = service.enrich(tenant);

        assertThat(result.getJurisdictionIds()).containsExactly("SG");
    }
}

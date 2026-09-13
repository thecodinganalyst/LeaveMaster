package com.practical.leavemaster.tenant;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DemoTenantPolicyTest {

    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final DemoTenantPolicy policy = new DemoTenantPolicy(tenantRepository);

    @Test
    void missingTenantContextDefaultsToStandard() {
        assertThat(policy.tenantType(null)).isEqualTo(TenantType.STANDARD);
        assertThat(policy.tenantType(" ")).isEqualTo(TenantType.STANDARD);
        assertThat(policy.isDemoTenant(null)).isFalse();
    }

    @Test
    void unknownOrLegacyTenantDefaultsToStandard() {
        when(tenantRepository.findById("unknown")).thenReturn(Optional.empty());
        when(tenantRepository.findById("legacy")).thenReturn(Optional.of(Tenant.builder().id("legacy").type(null).build()));

        assertThat(policy.tenantType("unknown")).isEqualTo(TenantType.STANDARD);
        assertThat(policy.tenantType("legacy")).isEqualTo(TenantType.STANDARD);
    }

    @Test
    void demoTenantSuppressesOutboundSideEffectsAndRestrictedOperations() {
        when(tenantRepository.findById("demo")).thenReturn(Optional.of(Tenant.builder().id("demo").type(TenantType.DEMO).build()));

        assertThat(policy.isDemoTenant("demo")).isTrue();
        assertThat(policy.suppressOutboundSideEffects("demo", "email")).isTrue();
        assertThatThrownBy(() -> policy.requireStandardTenant("demo", "external integration"))
                .isInstanceOf(DemoTenantOperationException.class)
                .hasMessageContaining("external integration");
    }

    @Test
    void standardTenantAllowsNormalOperations() {
        when(tenantRepository.findById("standard")).thenReturn(Optional.of(Tenant.builder().id("standard").type(TenantType.STANDARD).build()));

        assertThat(policy.suppressOutboundSideEffects("standard", "email")).isFalse();
        assertThatCode(() -> policy.requireStandardTenant("standard", "external integration")).doesNotThrowAnyException();
    }
}

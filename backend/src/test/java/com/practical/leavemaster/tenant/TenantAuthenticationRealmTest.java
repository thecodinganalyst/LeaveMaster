package com.practical.leavemaster.tenant;

import com.practical.leavemaster.user.AuthenticationRealm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantAuthenticationRealmTest {

    @Test
    void reservedPlatformRealmCannotBePersistedAsTenantId() {
        Tenant tenant = Tenant.builder()
                .id(" platform ")
                .name("Invalid")
                .jurisdictionId("SG")
                .build();

        assertThatThrownBy(tenant::refreshLastModified)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(AuthenticationRealm.PLATFORM_REALM_ID);
    }

    @Test
    void ordinaryTenantRefreshesLastModified() {
        Tenant tenant = Tenant.builder()
                .id("tenant-a")
                .name("Tenant A")
                .jurisdictionId("SG")
                .build();
        assertThat(tenant.getLastModified()).isNull();

        tenant.refreshLastModified();

        assertThat(tenant.getLastModified()).isNotNull();
    }
}

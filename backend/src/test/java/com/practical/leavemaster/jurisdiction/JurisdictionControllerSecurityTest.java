package com.practical.leavemaster.jurisdiction;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class JurisdictionControllerSecurityTest {

    private static final String REQUIRED_WRITE_EXPRESSION =
            "hasAuthority('JURISDICTION_WRITE') and hasAuthority('ROLE_PLATFORM_ADMIN')";

    @Test
    void allGlobalJurisdictionMutationEndpointsRequirePlatformAdminAndWritePermission() throws Exception {
        assertWriteAuthorization(JurisdictionController.class.getMethod("create", Jurisdiction.class));
        assertWriteAuthorization(JurisdictionController.class.getMethod("update", String.class, Jurisdiction.class));
        assertWriteAuthorization(JurisdictionController.class.getMethod("delete", String.class));
    }

    @Test
    void platformAdminMarkerIsRequiredInAdditionToGenericJurisdictionWritePermission() {
        assertThat(REQUIRED_WRITE_EXPRESSION)
                .contains("hasAuthority('JURISDICTION_WRITE')")
                .contains("hasAuthority('ROLE_PLATFORM_ADMIN')")
                .contains(" and ");
    }

    private void assertWriteAuthorization(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize)
                .as("%s must declare method-level authorization", method.getName())
                .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(REQUIRED_WRITE_EXPRESSION);
    }
}

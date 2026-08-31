package com.practical.leavemaster.staff;

import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StaffRoleAssignmentPolicyNoAuthTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedTokenHasNoAssignableRoles() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "n/a"));
        StaffRoleAssignmentPolicy policy = new StaffRoleAssignmentPolicy(
                mock(AppRoleRepository.class), mock(AppUserRepository.class));

        assertThat(policy.findAssignableRoles()).isEmpty();
    }
}

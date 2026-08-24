package com.practical.leavemaster.user;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AppUserDetailsService appUserDetailsService;

    @Test
    void shouldLoadDistinctUnionOfAuthoritiesFromAllActiveRoles() {
        AppPermission tenantRead = AppPermission.builder().code("TENANT_READ").description("Read tenant data").build();
        AppPermission staffWrite = AppPermission.builder().code("STAFF_WRITE").description("Write staff data").build();
        AppPermission leaveApprove = AppPermission.builder().code("LEAVE_APPROVE").description("Approve leave").build();

        AppRole managerRole = AppRole.builder()
                .id("MANAGER")
                .description("Manager")
                .active(true)
                .permissions(Set.of(tenantRead, staffWrite))
                .build();
        AppRole approverRole = AppRole.builder()
                .id("APPROVER")
                .description("Approver")
                .active(true)
                .permissions(Set.of(tenantRead, leaveApprove))
                .build();
        AppRole disabledRole = AppRole.builder()
                .id("OLD")
                .description("Disabled")
                .active(false)
                .permissions(Set.of(AppPermission.builder().code("USER_WRITE").description("Write users").build()))
                .build();

        AppUser user = AppUser.builder()
                .loginName("alice")
                .password("{noop}password")
                .active(true)
                .roles(Set.of(managerRole, approverRole, disabledRole))
                .build();

        when(appUserRepository.findById("alice")).thenReturn(Optional.of(user));

        UserDetails details = appUserDetailsService.loadUserByUsername("alice");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("TENANT_READ", "STAFF_WRITE", "LEAVE_APPROVE")
                .doesNotContain("USER_WRITE");
    }

    @Test
    void shouldThrowWhenUserMissing() {
        when(appUserRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserDetailsService.loadUserByUsername("missing"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }
}

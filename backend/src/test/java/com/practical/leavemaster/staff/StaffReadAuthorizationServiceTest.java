package com.practical.leavemaster.staff;

import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffReadAuthorizationServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private StaffRepository staffRepository;

    private StaffReadAuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new StaffReadAuthorizationService(appUserRepository, staffRepository);
    }

    @Test
    void allowsUserToReadOwnStaffRecordWithoutStaffReadPermission() {
        AppUser user = AppUser.builder().userId("user-1").tenantId("tenant-a").staffId("S001").build();
        Staff staff = Staff.builder().id("S001").tenantId("tenant-a").build();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));

        assertThat(service.canRead("S001", authentication("user-1"))).isTrue();
    }

    @Test
    void deniesUserReadingAnotherStaffRecordWithoutStaffReadPermission() {
        AppUser user = AppUser.builder().userId("user-1").tenantId("tenant-a").staffId("S001").build();
        Staff staff = Staff.builder().id("S002").tenantId("tenant-a").build();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(staffRepository.findById("S002")).thenReturn(Optional.of(staff));

        assertThat(service.canRead("S002", authentication("user-1"))).isFalse();
    }

    @Test
    void allowsStaffReadPermissionWithinSameTenant() {
        AppUser user = AppUser.builder().userId("user-1").tenantId("tenant-a").staffId("S001").build();
        Staff staff = Staff.builder().id("S002").tenantId("tenant-a").build();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(staffRepository.findById("S002")).thenReturn(Optional.of(staff));

        Authentication auth = authentication("user-1", RbacPermissions.STAFF_READ);
        assertThat(service.canRead("S002", auth)).isTrue();
    }

    @Test
    void deniesCrossTenantReadEvenWithStaffReadPermission() {
        AppUser user = AppUser.builder().userId("user-1").tenantId("tenant-a").staffId("S001").build();
        Staff staff = Staff.builder().id("S999").tenantId("tenant-b").build();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(staffRepository.findById("S999")).thenReturn(Optional.of(staff));

        Authentication auth = authentication("user-1", RbacPermissions.STAFF_READ);
        assertThat(service.canRead("S999", auth)).isFalse();
    }

    @Test
    void deniesReadWhenRequestedStaffDoesNotExist() {
        AppUser user = AppUser.builder().userId("user-1").tenantId("tenant-a").staffId("S001").build();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(staffRepository.findById("S001")).thenReturn(Optional.empty());

        assertThat(service.canRead("S001", authentication("user-1"))).isFalse();
    }

    @Test
    void allowsPlatformAdministrator() {
        Authentication auth = authentication("platform-user", "ROLE_PLATFORM_ADMIN");
        assertThat(service.canRead("S001", auth)).isTrue();
    }

    @Test
    void rejectsMissingOrUnauthenticatedIdentity() {
        assertThat(service.canRead("S001", null)).isFalse();
        assertThat(service.canRead("", authentication("user-1"))).isFalse();
    }

    private Authentication authentication(String principal, String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                principal,
                "n/a",
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }
}

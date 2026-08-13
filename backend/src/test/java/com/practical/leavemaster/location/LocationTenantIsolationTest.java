package com.practical.leavemaster.location;

import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationTenantIsolationTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private TenantActivityService tenantActivityService;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private LocationService locationService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantUserShouldOnlyListLocationsFromOwnTenant() {
        authenticate("admin-a");
        AppUser currentUser = user("admin-a", "tenant-a", Set.of());
        Location ownLocation = location("sg", "tenant-a");
        when(appUserRepository.findById("admin-a")).thenReturn(Optional.of(currentUser));
        when(locationRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(ownLocation));

        List<Location> result = locationService.findAll();

        assertThat(result).containsExactly(ownLocation);
        verify(locationRepository, never()).findAll();
    }

    @Test
    void tenantUserShouldNotReadLocationFromAnotherTenantOrNullTenant() {
        authenticate("admin-a");
        when(appUserRepository.findById("admin-a")).thenReturn(Optional.of(user("admin-a", "tenant-a", Set.of())));
        when(locationRepository.findById("tenant-b-location")).thenReturn(Optional.of(location("tenant-b-location", "tenant-b")));
        when(locationRepository.findById("unassigned-location")).thenReturn(Optional.of(location("unassigned-location", null)));

        assertThat(locationService.findById("tenant-b-location")).isEmpty();
        assertThat(locationService.findById("unassigned-location")).isEmpty();
    }

    @Test
    void tenantUserCreateShouldOverrideRequestTenantWithOwnTenant() {
        authenticate("admin-a");
        when(appUserRepository.findById("admin-a")).thenReturn(Optional.of(user("admin-a", "tenant-a", Set.of())));
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Location request = location("sg", "tenant-b");
        Location saved = locationService.save(request);

        assertThat(saved.getTenantId()).isEqualTo("tenant-a");
        verify(tenantActivityService).touch("tenant-a");
    }

    @Test
    void tenantUserShouldNotUpdateOrDeleteLocationOutsideOwnTenant() {
        authenticate("admin-a");
        when(appUserRepository.findById("admin-a")).thenReturn(Optional.of(user("admin-a", "tenant-a", Set.of())));
        when(locationRepository.findById("foreign")).thenReturn(Optional.of(location("foreign", "tenant-b")));

        assertThatThrownBy(() -> locationService.update("foreign", location("foreign", "tenant-a")))
                .isInstanceOf(LocationNotFoundException.class);
        assertThatThrownBy(() -> locationService.delete("foreign"))
                .isInstanceOf(LocationNotFoundException.class);

        verify(locationRepository, never()).save(any(Location.class));
        verify(locationRepository, never()).deleteById("foreign");
    }

    @Test
    void platformAdminShouldRetainGlobalLocationAccess() {
        authenticate("platformadmin");
        AppRole platformAdminRole = AppRole.builder()
                .id("PLATFORM_ADMIN")
                .description("Platform administrator")
                .active(true)
                .build();
        when(appUserRepository.findById("platformadmin"))
                .thenReturn(Optional.of(user("platformadmin", null, Set.of(platformAdminRole))));
        Location unassigned = location("global", null);
        when(locationRepository.findAll()).thenReturn(List.of(unassigned));

        assertThat(locationService.findAll()).containsExactly(unassigned);
        verify(locationRepository, never()).findAllByTenantId(any());
    }

    private static void authenticate(String loginName) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginName, "n/a", List.of())
        );
    }

    private static AppUser user(String loginName, String tenantId, Set<AppRole> roles) {
        return AppUser.builder()
                .loginName(loginName)
                .password("encoded")
                .active(true)
                .tenantId(tenantId)
                .roles(roles)
                .build();
    }

    private static Location location(String id, String tenantId) {
        return Location.builder()
                .id(id)
                .locationName(id)
                .country("Singapore")
                .tenantId(tenantId)
                .build();
    }
}

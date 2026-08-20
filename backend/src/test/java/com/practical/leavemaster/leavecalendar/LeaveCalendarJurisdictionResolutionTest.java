package com.practical.leavemaster.leavecalendar;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.rbac.AppRole;
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

import java.time.LocalDate;
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
class LeaveCalendarJurisdictionResolutionTest {

    @Mock private LeaveCalendarRepository leaveCalendarRepository;
    @Mock private TenantActivityService tenantActivityService;
    @Mock private AppUserRepository appUserRepository;
    @InjectMocks private LeaveCalendarService leaveCalendarService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsBlankJurisdictionForJurisdictionLookup() {
        assertThatThrownBy(() -> leaveCalendarService.getCalendarFor("  ", LocalDate.of(2026, 8, 20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jurisdictionId");
    }

    @Test
    void unauthenticatedLookupFindsMatchingTenantCalendarByJurisdiction() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        LeaveCalendar singapore = tenantCalendar("sg", "tenant-1", "SG");
        LeaveCalendar malaysia = tenantCalendar("my", "tenant-1", "MY");
        when(leaveCalendarRepository.findAll()).thenReturn(List.of(malaysia, singapore));

        assertThat(leaveCalendarService.getCalendarFor(" SG ", date)).contains(singapore);
    }

    @Test
    void tenantLookupReturnsExistingCalendarForRequestedJurisdiction() {
        authenticateTenantUser("hr", "tenant-1");
        LocalDate date = LocalDate.of(2026, 8, 20);
        LeaveCalendar singapore = tenantCalendar("sg", "tenant-1", "SG");
        when(leaveCalendarRepository.findByTenantIdAndJurisdictionIdAndStartLessThanEqualAndEndGreaterThanEqual(
                "tenant-1", "SG", date, date)).thenReturn(Optional.of(singapore));

        assertThat(leaveCalendarService.getCalendarFor("SG", date)).contains(singapore);
        verify(leaveCalendarRepository, never()).findTopByTenantIdAndJurisdictionIdOrderByEndDesc(any(), any());
    }

    @Test
    void platformAdminJurisdictionLookupDoesNotReturnOperationalCalendar() {
        authenticatePlatformAdmin("platform");

        assertThat(leaveCalendarService.getCalendarFor("SG", LocalDate.of(2026, 8, 20))).isEmpty();
        verify(leaveCalendarRepository, never()).findAll();
    }

    private LeaveCalendar tenantCalendar(String id, String tenantId, String jurisdictionId) {
        return LeaveCalendar.builder()
                .id(id)
                .tenantId(tenantId)
                .scope(ConfigurationScope.TENANT)
                .jurisdictionId(jurisdictionId)
                .start(LocalDate.of(2026, 1, 1))
                .end(LocalDate.of(2026, 12, 31))
                .build();
    }

    private void authenticateTenantUser(String login, String tenantId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of()));
        when(appUserRepository.findById(login)).thenReturn(Optional.of(AppUser.builder()
                .loginName(login).active(true).tenantId(tenantId).roles(Set.of()).build()));
    }

    private void authenticatePlatformAdmin(String login) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of()));
        when(appUserRepository.findById(login)).thenReturn(Optional.of(AppUser.builder()
                .loginName(login).active(true)
                .roles(Set.of(AppRole.builder()
                        .id("PLATFORM_ADMIN").description("Platform admin").active(true).build()))
                .build()));
    }
}

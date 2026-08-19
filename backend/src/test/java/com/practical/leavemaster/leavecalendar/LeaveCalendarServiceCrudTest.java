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
class LeaveCalendarServiceCrudTest {
    @Mock private LeaveCalendarRepository leaveCalendarRepository;
    @Mock private TenantActivityService tenantActivityService;
    @Mock private AppUserRepository appUserRepository;
    @InjectMocks private LeaveCalendarService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void platformAdminCanUpdatePlatformCalendarTemplateWithoutChangingScope() {
        authenticatePlatformAdmin();
        LeaveCalendar existing = template("template:SG:2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        existing.setSourceTemplateId("origin");
        LeaveCalendar requested = template(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        requested.setSourceTemplateId("client-overwrite");

        when(leaveCalendarRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "SG"))
                .thenReturn(List.of(existing));
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveCalendar saved = service.update(existing.getId(), requested);

        assertThat(saved.getScope()).isEqualTo(ConfigurationScope.PLATFORM_TEMPLATE);
        assertThat(saved.getTenantId()).isNull();
        assertThat(saved.getJurisdictionId()).isEqualTo("SG");
        assertThat(saved.getSourceTemplateId()).isEqualTo("origin");
        verify(tenantActivityService, never()).touch(any());
    }

    @Test
    void platformAdminCanRetrieveTemplatesForJurisdictionAndYear() {
        authenticatePlatformAdmin();
        LeaveCalendar calendar2026 = template("template:SG:2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        LeaveCalendar calendar2027 = template("template:SG:2027", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31));
        LeaveCalendar tenantCalendar = LeaveCalendar.builder()
                .id("tenant-1:2026")
                .tenantId("tenant-1")
                .scope(ConfigurationScope.TENANT)
                .start(LocalDate.of(2026, 1, 1))
                .end(LocalDate.of(2026, 12, 31))
                .build();
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "SG"))
                .thenReturn(List.of(calendar2027, tenantCalendar, calendar2026));

        List<LeaveCalendar> result = service.findPlatformTemplates("SG", 2026);

        assertThat(result).extracting(LeaveCalendar::getId).containsExactly("template:SG:2026");
    }

    @Test
    void createRejectsDuplicateHolidayDateAndNameIgnoringCaseAndWhitespace() {
        LeaveCalendar requested = template(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        requested.setPublicHolidays(List.of(
                PublicHoliday.builder()
                        .holidayDate(LocalDate.of(2026, 8, 9))
                        .holidayName("National Day")
                        .build(),
                PublicHoliday.builder()
                        .holidayDate(LocalDate.of(2026, 8, 9))
                        .holidayName(" national day ")
                        .build()));

        assertThatThrownBy(() -> service.create(requested))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate public holiday");

        verify(leaveCalendarRepository, never()).save(any());
    }

    @Test
    void tenantUserCannotRetrievePlatformHolidayTemplates() {
        authenticateTenantUser();

        assertThatThrownBy(() -> service.findPlatformTemplates("SG", 2026))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only Platform Admin");

        verify(leaveCalendarRepository, never())
                .findAllByScopeAndJurisdictionId(any(), any());
    }

    @Test
    void tenantUserCanDeleteOwnTenantCalendarAndTenantActivityIsTouched() {
        authenticateTenantUser();
        LeaveCalendar existing = LeaveCalendar.builder()
                .id("tenant-1:2026")
                .tenantId("tenant-1")
                .scope(ConfigurationScope.TENANT)
                .start(LocalDate.of(2026, 1, 1))
                .end(LocalDate.of(2026, 12, 31))
                .publicHolidays(List.of())
                .build();
        when(leaveCalendarRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        service.delete(existing.getId());

        verify(leaveCalendarRepository).delete(existing);
        verify(tenantActivityService).touch("tenant-1");
    }

    private void authenticatePlatformAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("platform", "n/a", List.of()));
        when(appUserRepository.findById("platform")).thenReturn(Optional.of(AppUser.builder()
                .loginName("platform")
                .active(true)
                .roles(Set.of(AppRole.builder().id("PLATFORM_ADMIN").description("Platform admin").active(true).build()))
                .build()));
    }

    private void authenticateTenantUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hr", "n/a", List.of()));
        when(appUserRepository.findById("hr")).thenReturn(Optional.of(AppUser.builder()
                .loginName("hr")
                .active(true)
                .tenantId("tenant-1")
                .roles(Set.of())
                .build()));
    }

    private LeaveCalendar template(String id, LocalDate start, LocalDate end) {
        return LeaveCalendar.builder()
                .id(id)
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId("SG")
                .start(start)
                .end(end)
                .publicHolidays(List.of())
                .build();
    }
}

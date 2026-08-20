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
class LeaveCalendarServiceTest {

    @Mock private LeaveCalendarRepository leaveCalendarRepository;
    @Mock private TenantActivityService tenantActivityService;
    @Mock private AppUserRepository appUserRepository;
    @InjectMocks private LeaveCalendarService leaveCalendarService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantUserOnlySeesOwnTenantCalendars() {
        authenticateTenantUser("hr", "tenant-1");
        LeaveCalendar tenantCalendar = tenantCalendar("fy2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        when(leaveCalendarRepository.findAllByTenantIdOrderByStartAsc("tenant-1")).thenReturn(List.of(tenantCalendar));

        assertThat(leaveCalendarService.findAll()).containsExactly(tenantCalendar);
        verify(leaveCalendarRepository, never()).findAll();
    }

    @Test
    void platformAdminOnlySeesPlatformTemplates() {
        authenticatePlatformAdmin("platform");
        LeaveCalendar template = platformTemplate("template-sg-2026");
        LeaveCalendar tenantCalendar = tenantCalendar("fy2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        when(leaveCalendarRepository.findAll()).thenReturn(List.of(tenantCalendar, template));

        assertThat(leaveCalendarService.findAll()).containsExactly(template);
    }

    @Test
    void tenantUserCannotReadAnotherTenantCalendar() {
        authenticateTenantUser("hr", "tenant-1");
        LeaveCalendar other = tenantCalendar("other", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        other.setTenantId("tenant-2");
        when(leaveCalendarRepository.findById("other")).thenReturn(Optional.of(other));

        assertThat(leaveCalendarService.findById("other")).isEmpty();
    }

    @Test
    void platformAdminCannotReadTenantCalendar() {
        authenticatePlatformAdmin("platform");
        LeaveCalendar tenantCalendar = tenantCalendar("fy2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        when(leaveCalendarRepository.findById("fy2026")).thenReturn(Optional.of(tenantCalendar));

        assertThat(leaveCalendarService.findById("fy2026")).isEmpty();
    }

    @Test
    void tenantUserCreateForcesTenantScopeAndTenantIdAndRetainsJurisdiction() {
        authenticateTenantUser("hr", "tenant-1");
        LeaveCalendar requested = platformTemplate(null);
        when(leaveCalendarRepository.existsById("tenant-1:SG:2026-01-01_2026-12-31")).thenReturn(false);
        when(leaveCalendarRepository.existsByTenantIdAndJurisdictionIdAndStartLessThanEqualAndEndGreaterThanEqual(
                "tenant-1", "SG", LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1))).thenReturn(false);
        when(leaveCalendarRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveCalendar saved = leaveCalendarService.create(requested);

        assertThat(saved.getScope()).isEqualTo(ConfigurationScope.TENANT);
        assertThat(saved.getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.getJurisdictionId()).isEqualTo("SG");
        verify(tenantActivityService).touch("tenant-1");
    }

    @Test
    void platformAdminCreateForcesTemplateScopeAndNullTenant() {
        authenticatePlatformAdmin("platform");
        LeaveCalendar requested = tenantCalendar(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        when(leaveCalendarRepository.existsById("template:SG:2026-01-01_2026-12-31")).thenReturn(false);
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "SG")).thenReturn(List.of());
        when(leaveCalendarRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveCalendar saved = leaveCalendarService.create(requested);

        assertThat(saved.getScope()).isEqualTo(ConfigurationScope.PLATFORM_TEMPLATE);
        assertThat(saved.getTenantId()).isNull();
        assertThat(saved.getJurisdictionId()).isEqualTo("SG");
        verify(tenantActivityService, never()).touch(any());
    }

    @Test
    void tenantUserUsesJurisdictionScopedCalendarLookupAndGeneration() {
        authenticateTenantUser("hr", "tenant-1");
        LocalDate requestedDate = LocalDate.of(2027, 6, 1);
        LeaveCalendar existing = tenantCalendar("fy2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        when(leaveCalendarRepository.findByTenantIdAndJurisdictionIdAndStartLessThanEqualAndEndGreaterThanEqual(
                "tenant-1", "SG", requestedDate, requestedDate)).thenReturn(Optional.empty());
        when(leaveCalendarRepository.findTopByTenantIdAndJurisdictionIdOrderByEndDesc("tenant-1", "SG"))
                .thenReturn(Optional.of(existing));
        when(leaveCalendarRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<LeaveCalendar> result = leaveCalendarService.getCalendarFor("SG", requestedDate);

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo("tenant-1");
        assertThat(result.get().getJurisdictionId()).isEqualTo("SG");
        assertThat(result.get().getStart()).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    void tenantDateOnlyLookupReturnsEmptyWhenMultipleJurisdictionsMatch() {
        authenticateTenantUser("hr", "tenant-1");
        LocalDate date = LocalDate.of(2026, 6, 1);
        LeaveCalendar sg = tenantCalendar("sg", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        LeaveCalendar my = tenantCalendar("my", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        my.setJurisdictionId("MY");
        when(leaveCalendarRepository.findAllByTenantIdOrderByStartAsc("tenant-1")).thenReturn(List.of(sg, my));

        assertThat(leaveCalendarService.getCalendarFor(date)).isEmpty();
    }

    @Test
    void tenantUserWithoutTenantIdIsRejected() {
        authenticateTenantUser("hr", null);
        assertThatThrownBy(() -> leaveCalendarService.findAll())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenant id");
    }

    @Test
    void shouldCreateLeaveCalendar() {
        LeaveCalendar leaveCalendar = tenantCalendar("fy2026", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
        leaveCalendar.setPublicHolidays(List.of(PublicHoliday.builder().holidayDate(LocalDate.of(2026, 5, 1)).holidayName("Labour Day").build()));
        when(leaveCalendarRepository.existsById("fy2026")).thenReturn(false);
        when(leaveCalendarRepository.existsByTenantIdAndJurisdictionIdAndStartLessThanEqualAndEndGreaterThanEqual(
                "tenant-1", "SG", LocalDate.of(2027, 3, 31), LocalDate.of(2026, 4, 1))).thenReturn(false);
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveCalendar result = leaveCalendarService.create(leaveCalendar);
        assertThat(result.getId()).isEqualTo("fy2026");
        assertThat(result.getTenantId()).isEqualTo("tenant-1");
        assertThat(result.getScope()).isEqualTo(ConfigurationScope.TENANT);
        assertThat(result.getJurisdictionId()).isEqualTo("SG");
        assertThat(result.getPublicHolidays()).hasSize(1);
    }

    @Test
    void shouldAutoGenerateFutureCalendarFromLatestCalendar() {
        LeaveCalendar existing = tenantCalendar("fy2026", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
        existing.setPublicHolidays(List.of(PublicHoliday.builder().holidayDate(LocalDate.of(2026, 5, 1)).holidayName("Labour Day").build()));
        when(leaveCalendarRepository.findByStartLessThanEqualAndEndGreaterThanEqual(LocalDate.of(2027, 4, 15), LocalDate.of(2027, 4, 15))).thenReturn(Optional.empty());
        when(leaveCalendarRepository.findTopByOrderByEndDesc()).thenReturn(Optional.of(existing));
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<LeaveCalendar> result = leaveCalendarService.getCalendarFor(LocalDate.of(2027, 4, 15));
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("tenant-1:SG:2027-04-01_2028-03-31");
        assertThat(result.get().getStart()).isEqualTo(LocalDate.of(2027, 4, 1));
        assertThat(result.get().getEnd()).isEqualTo(LocalDate.of(2028, 3, 31));
        assertThat(result.get().getTenantId()).isEqualTo("tenant-1");
        assertThat(result.get().getJurisdictionId()).isEqualTo("SG");
        assertThat(result.get().getPublicHolidays()).containsExactly(PublicHoliday.builder().holidayDate(LocalDate.of(2027, 5, 1)).holidayName("Labour Day").build());
    }

    @Test
    void shouldRejectPublicHolidayOutsideCalendarRange() {
        LeaveCalendar leaveCalendar = tenantCalendar(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        leaveCalendar.setPublicHolidays(List.of(PublicHoliday.builder().holidayDate(LocalDate.of(2027, 1, 1)).holidayName("New Year").build()));
        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("within the leave calendar range");
    }

    @Test
    void shouldFindAllLeaveCalendars() {
        List<LeaveCalendar> calendars = List.of(
                tenantCalendar("fy2025", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31)),
                tenantCalendar("fy2026", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31)));
        when(leaveCalendarRepository.findAllByOrderByStartAsc()).thenReturn(calendars);
        assertThat(leaveCalendarService.findAll()).hasSize(2).first().extracting(LeaveCalendar::getId).isEqualTo("fy2025");
    }

    @Test
    void shouldFindLeaveCalendarById() {
        LeaveCalendar calendar = tenantCalendar("fy2026", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
        when(leaveCalendarRepository.findById("fy2026")).thenReturn(Optional.of(calendar));
        assertThat(leaveCalendarService.findById("fy2026")).contains(calendar);
    }

    @Test
    void shouldThrowWhenLeaveCalendarAlreadyExists() {
        LeaveCalendar leaveCalendar = tenantCalendar("fy2026", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
        when(leaveCalendarRepository.existsById("fy2026")).thenReturn(true);
        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar)).isInstanceOf(LeaveCalendarConflictException.class).hasMessageContaining("already exists");
    }

    @Test
    void shouldThrowWhenTenantLeaveCalendarOverlapsSameJurisdiction() {
        LeaveCalendar leaveCalendar = tenantCalendar(null, LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
        when(leaveCalendarRepository.existsById("tenant-1:SG:2026-04-01_2027-03-31")).thenReturn(false);
        when(leaveCalendarRepository.existsByTenantIdAndJurisdictionIdAndStartLessThanEqualAndEndGreaterThanEqual(
                "tenant-1", "SG", LocalDate.of(2027, 3, 31), LocalDate.of(2026, 4, 1))).thenReturn(true);
        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar)).isInstanceOf(LeaveCalendarConflictException.class).hasMessageContaining("overlaps");
    }

    @Test
    void shouldAllowSamePeriodForDifferentJurisdiction() {
        LeaveCalendar leaveCalendar = tenantCalendar(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        leaveCalendar.setJurisdictionId("MY");
        when(leaveCalendarRepository.existsById("tenant-1:MY:2026-01-01_2026-12-31")).thenReturn(false);
        when(leaveCalendarRepository.existsByTenantIdAndJurisdictionIdAndStartLessThanEqualAndEndGreaterThanEqual(
                "tenant-1", "MY", LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1))).thenReturn(false);
        when(leaveCalendarRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveCalendar saved = leaveCalendarService.create(leaveCalendar);

        assertThat(saved.getJurisdictionId()).isEqualTo("MY");
    }

    @Test
    void shouldThrowWhenStartOrEndDateIsNull() {
        LeaveCalendar leaveCalendar = tenantCalendar(null, null, LocalDate.of(2027, 3, 31));
        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("start and end dates are required");
    }

    @Test
    void shouldThrowWhenStartDateIsAfterEndDate() {
        LeaveCalendar leaveCalendar = tenantCalendar(null, LocalDate.of(2027, 3, 31), LocalDate.of(2026, 4, 1));
        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("start date must be on or before end date");
    }

    @Test
    void shouldThrowWhenJurisdictionIsMissing() {
        LeaveCalendar leaveCalendar = tenantCalendar(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        leaveCalendar.setJurisdictionId(null);
        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("jurisdictionId");
    }

    @Test
    void shouldThrowWhenPublicHolidayHasNullDate() {
        LeaveCalendar leaveCalendar = tenantCalendar(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        leaveCalendar.setPublicHolidays(List.of(PublicHoliday.builder().holidayDate(null).holidayName("New Year").build()));
        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Public holidays require both holidayDate and holidayName");
    }

    @Test
    void shouldReturnEmptyWhenNoCalendarExistsForDate() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        when(leaveCalendarRepository.findByStartLessThanEqualAndEndGreaterThanEqual(date, date)).thenReturn(Optional.empty());
        when(leaveCalendarRepository.findTopByOrderByEndDesc()).thenReturn(Optional.empty());
        assertThat(leaveCalendarService.getCalendarFor(date)).isEmpty();
    }

    @Test
    void shouldReturnExistingCalendarForDate() {
        LocalDate date = LocalDate.of(2026, 6, 15);
        LeaveCalendar calendar = tenantCalendar("fy2026", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
        when(leaveCalendarRepository.findByStartLessThanEqualAndEndGreaterThanEqual(date, date)).thenReturn(Optional.of(calendar));
        assertThat(leaveCalendarService.getCalendarFor(date)).contains(calendar);
    }

    @Test
    void shouldAutoGenerateCalendarWithNoPublicHolidaysFromLatestCalendar() {
        LeaveCalendar existing = tenantCalendar("fy2026", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
        existing.setPublicHolidays(List.of());
        when(leaveCalendarRepository.findByStartLessThanEqualAndEndGreaterThanEqual(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 1))).thenReturn(Optional.empty());
        when(leaveCalendarRepository.findTopByOrderByEndDesc()).thenReturn(Optional.of(existing));
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(leaveCalendarService.getCalendarFor(LocalDate.of(2027, 6, 1))).isPresent().get().extracting(LeaveCalendar::getPublicHolidays).asList().isEmpty();
    }

    @Test
    void shouldCreateCalendarWithAutoGeneratedIdWhenNoneProvided() {
        LeaveCalendar leaveCalendar = tenantCalendar(null, LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
        when(leaveCalendarRepository.existsById("tenant-1:SG:2026-04-01_2027-03-31")).thenReturn(false);
        when(leaveCalendarRepository.existsByTenantIdAndJurisdictionIdAndStartLessThanEqualAndEndGreaterThanEqual(
                "tenant-1", "SG", LocalDate.of(2027, 3, 31), LocalDate.of(2026, 4, 1))).thenReturn(false);
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(leaveCalendarService.create(leaveCalendar).getId()).isEqualTo("tenant-1:SG:2026-04-01_2027-03-31");
    }

    @Test
    void shouldCreatePlatformCalendarTemplate() {
        LeaveCalendar template = platformTemplate(null);
        when(leaveCalendarRepository.existsById("template:SG:2026-01-01_2026-12-31")).thenReturn(false);
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "SG")).thenReturn(List.of());
        when(leaveCalendarRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LeaveCalendar saved = leaveCalendarService.create(template);
        assertThat(saved.getTenantId()).isNull();
        assertThat(saved.getJurisdictionId()).isEqualTo("SG");
    }

    private LeaveCalendar tenantCalendar(String id, LocalDate start, LocalDate end) {
        return LeaveCalendar.builder().id(id).start(start).end(end).tenantId("tenant-1")
                .scope(ConfigurationScope.TENANT).jurisdictionId("SG").publicHolidays(List.of()).build();
    }

    private LeaveCalendar platformTemplate(String id) {
        return LeaveCalendar.builder().id(id).scope(ConfigurationScope.PLATFORM_TEMPLATE).jurisdictionId("SG")
                .start(LocalDate.of(2026, 1, 1)).end(LocalDate.of(2026, 12, 31)).publicHolidays(List.of()).build();
    }

    private void authenticateTenantUser(String login, String tenantId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, "n/a", List.of()));
        when(appUserRepository.findById(login)).thenReturn(Optional.of(AppUser.builder()
                .loginName(login).active(true).tenantId(tenantId).roles(Set.of()).build()));
    }

    private void authenticatePlatformAdmin(String login) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, "n/a", List.of()));
        when(appUserRepository.findById(login)).thenReturn(Optional.of(AppUser.builder()
                .loginName(login).active(true)
                .roles(Set.of(AppRole.builder().id("PLATFORM_ADMIN").description("Platform admin").active(true).build()))
                .build()));
    }
}

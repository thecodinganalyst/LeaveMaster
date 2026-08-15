package com.practical.leavemaster.leavecalendar;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveCalendarServiceTest {

    @Mock private LeaveCalendarRepository leaveCalendarRepository;
    @Mock private TenantActivityService tenantActivityService;
    @Mock private AppUserRepository appUserRepository;
    @InjectMocks private LeaveCalendarService leaveCalendarService;

    @Test
    void shouldCreateLeaveCalendar() {
        LeaveCalendar leaveCalendar = tenantCalendar("fy2026", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
        leaveCalendar.setPublicHolidays(List.of(PublicHoliday.builder().holidayDate(LocalDate.of(2026, 5, 1)).holidayName("Labour Day").build()));
        when(leaveCalendarRepository.existsById("fy2026")).thenReturn(false);
        when(leaveCalendarRepository.existsByTenantIdAndStartLessThanEqualAndEndGreaterThanEqual(
                "tenant-1", LocalDate.of(2027, 3, 31), LocalDate.of(2026, 4, 1))).thenReturn(false);
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveCalendar result = leaveCalendarService.create(leaveCalendar);
        assertThat(result.getId()).isEqualTo("fy2026");
        assertThat(result.getTenantId()).isEqualTo("tenant-1");
        assertThat(result.getScope()).isEqualTo(ConfigurationScope.TENANT);
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
        assertThat(result.get().getId()).isEqualTo("tenant-1:2027-04-01_2028-03-31");
        assertThat(result.get().getStart()).isEqualTo(LocalDate.of(2027, 4, 1));
        assertThat(result.get().getEnd()).isEqualTo(LocalDate.of(2028, 3, 31));
        assertThat(result.get().getTenantId()).isEqualTo("tenant-1");
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
    void shouldThrowWhenTenantLeaveCalendarOverlapsExisting() {
        LeaveCalendar leaveCalendar = tenantCalendar(null, LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
        when(leaveCalendarRepository.existsById("tenant-1:2026-04-01_2027-03-31")).thenReturn(false);
        when(leaveCalendarRepository.existsByTenantIdAndStartLessThanEqualAndEndGreaterThanEqual(
                "tenant-1", LocalDate.of(2027, 3, 31), LocalDate.of(2026, 4, 1))).thenReturn(true);
        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar)).isInstanceOf(LeaveCalendarConflictException.class).hasMessageContaining("overlaps");
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
        when(leaveCalendarRepository.existsById("tenant-1:2026-04-01_2027-03-31")).thenReturn(false);
        when(leaveCalendarRepository.existsByTenantIdAndStartLessThanEqualAndEndGreaterThanEqual(
                "tenant-1", LocalDate.of(2027, 3, 31), LocalDate.of(2026, 4, 1))).thenReturn(false);
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(leaveCalendarService.create(leaveCalendar).getId()).isEqualTo("tenant-1:2026-04-01_2027-03-31");
    }

    @Test
    void shouldCreatePlatformCalendarTemplate() {
        LeaveCalendar template = LeaveCalendar.builder().scope(ConfigurationScope.PLATFORM_TEMPLATE).jurisdictionId("SG")
                .start(LocalDate.of(2026, 1, 1)).end(LocalDate.of(2026, 12, 31)).publicHolidays(List.of()).build();
        when(leaveCalendarRepository.existsById("template:SG:2026-01-01_2026-12-31")).thenReturn(false);
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "SG")).thenReturn(List.of());
        when(leaveCalendarRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LeaveCalendar saved = leaveCalendarService.create(template);
        assertThat(saved.getTenantId()).isNull();
        assertThat(saved.getJurisdictionId()).isEqualTo("SG");
    }

    private LeaveCalendar tenantCalendar(String id, LocalDate start, LocalDate end) {
        return LeaveCalendar.builder().id(id).start(start).end(end).tenantId("tenant-1")
                .scope(ConfigurationScope.TENANT).publicHolidays(List.of()).build();
    }
}

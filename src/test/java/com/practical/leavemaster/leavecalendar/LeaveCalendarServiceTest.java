package com.practical.leavemaster.leavecalendar;

import com.practical.leavemaster.tenant.TenantActivityService;
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

    @Mock
    private LeaveCalendarRepository leaveCalendarRepository;

    @Mock
    private TenantActivityService tenantActivityService;

    @InjectMocks
    private LeaveCalendarService leaveCalendarService;

    @Test
    void shouldCreateLeaveCalendar() {
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .id("fy2026")
                .start(LocalDate.of(2026, 4, 1))
                .end(LocalDate.of(2027, 3, 31))
                .tenantId("tenant-1")
                .publicHolidays(List.of(
                        PublicHoliday.builder().holidayDate(LocalDate.of(2026, 5, 1)).holidayName("Labour Day").build()
                ))
                .build();

        when(leaveCalendarRepository.existsById("fy2026")).thenReturn(false);
        when(leaveCalendarRepository.existsByStartLessThanEqualAndEndGreaterThanEqual(
                LocalDate.of(2027, 3, 31), LocalDate.of(2026, 4, 1))).thenReturn(false);
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveCalendar result = leaveCalendarService.create(leaveCalendar);

        assertThat(result.getId()).isEqualTo("fy2026");
        assertThat(result.getTenantId()).isEqualTo("tenant-1");
        assertThat(result.getPublicHolidays()).hasSize(1);
    }

    @Test
    void shouldAutoGenerateFutureCalendarFromLatestCalendar() {
        LeaveCalendar existing = LeaveCalendar.builder()
                .id("fy2026")
                .start(LocalDate.of(2026, 4, 1))
                .end(LocalDate.of(2027, 3, 31))
                .tenantId("tenant-1")
                .publicHolidays(List.of(
                        PublicHoliday.builder().holidayDate(LocalDate.of(2026, 5, 1)).holidayName("Labour Day").build()
                ))
                .build();

        when(leaveCalendarRepository.findByStartLessThanEqualAndEndGreaterThanEqual(
                LocalDate.of(2027, 4, 15), LocalDate.of(2027, 4, 15))).thenReturn(Optional.empty());
        when(leaveCalendarRepository.findTopByOrderByEndDesc()).thenReturn(Optional.of(existing));
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<LeaveCalendar> result = leaveCalendarService.getCalendarFor(LocalDate.of(2027, 4, 15));

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("2027-04-01_2028-03-31");
        assertThat(result.get().getStart()).isEqualTo(LocalDate.of(2027, 4, 1));
        assertThat(result.get().getEnd()).isEqualTo(LocalDate.of(2028, 3, 31));
        assertThat(result.get().getTenantId()).isEqualTo("tenant-1");
        assertThat(result.get().getPublicHolidays()).containsExactly(
                PublicHoliday.builder().holidayDate(LocalDate.of(2027, 5, 1)).holidayName("Labour Day").build()
        );
    }

    @Test
    void shouldRejectPublicHolidayOutsideCalendarRange() {
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .start(LocalDate.of(2026, 1, 1))
                .end(LocalDate.of(2026, 12, 31))
                .publicHolidays(List.of(
                        PublicHoliday.builder().holidayDate(LocalDate.of(2027, 1, 1)).holidayName("New Year").build()
                ))
                .build();

        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("within the leave calendar range");
    }

    @Test
    void shouldFindAllLeaveCalendars() {
        List<LeaveCalendar> calendars = List.of(
                LeaveCalendar.builder().id("fy2025").start(LocalDate.of(2025, 4, 1)).end(LocalDate.of(2026, 3, 31)).build(),
                LeaveCalendar.builder().id("fy2026").start(LocalDate.of(2026, 4, 1)).end(LocalDate.of(2027, 3, 31)).build()
        );
        when(leaveCalendarRepository.findAllByOrderByStartAsc()).thenReturn(calendars);

        List<LeaveCalendar> result = leaveCalendarService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("fy2025");
    }

    @Test
    void shouldFindLeaveCalendarById() {
        LeaveCalendar calendar = LeaveCalendar.builder()
                .id("fy2026")
                .start(LocalDate.of(2026, 4, 1))
                .end(LocalDate.of(2027, 3, 31))
                .build();
        when(leaveCalendarRepository.findById("fy2026")).thenReturn(Optional.of(calendar));

        Optional<LeaveCalendar> result = leaveCalendarService.findById("fy2026");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("fy2026");
    }

    @Test
    void shouldThrowWhenLeaveCalendarAlreadyExists() {
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .id("fy2026")
                .start(LocalDate.of(2026, 4, 1))
                .end(LocalDate.of(2027, 3, 31))
                .build();

        when(leaveCalendarRepository.existsById("fy2026")).thenReturn(true);

        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar))
                .isInstanceOf(LeaveCalendarConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldThrowWhenLeaveCalendarOverlapsExisting() {
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .start(LocalDate.of(2026, 4, 1))
                .end(LocalDate.of(2027, 3, 31))
                .build();

        when(leaveCalendarRepository.existsById(any())).thenReturn(false);
        when(leaveCalendarRepository.existsByStartLessThanEqualAndEndGreaterThanEqual(
                LocalDate.of(2027, 3, 31), LocalDate.of(2026, 4, 1))).thenReturn(true);

        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar))
                .isInstanceOf(LeaveCalendarConflictException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    void shouldThrowWhenStartOrEndDateIsNull() {
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .start(null)
                .end(LocalDate.of(2027, 3, 31))
                .build();

        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("start and end dates are required");
    }

    @Test
    void shouldThrowWhenStartDateIsAfterEndDate() {
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .start(LocalDate.of(2027, 3, 31))
                .end(LocalDate.of(2026, 4, 1))
                .build();

        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("start date must be on or before end date");
    }

    @Test
    void shouldThrowWhenPublicHolidayHasNullDate() {
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .start(LocalDate.of(2026, 1, 1))
                .end(LocalDate.of(2026, 12, 31))
                .publicHolidays(List.of(
                        PublicHoliday.builder().holidayDate(null).holidayName("New Year").build()
                ))
                .build();

        assertThatThrownBy(() -> leaveCalendarService.create(leaveCalendar))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Public holidays require both holidayDate and holidayName");
    }

    @Test
    void shouldReturnEmptyWhenNoCalendarExistsForDate() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        when(leaveCalendarRepository.findByStartLessThanEqualAndEndGreaterThanEqual(date, date))
                .thenReturn(Optional.empty());
        when(leaveCalendarRepository.findTopByOrderByEndDesc()).thenReturn(Optional.empty());

        Optional<LeaveCalendar> result = leaveCalendarService.getCalendarFor(date);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnExistingCalendarForDate() {
        LocalDate date = LocalDate.of(2026, 6, 15);
        LeaveCalendar calendar = LeaveCalendar.builder()
                .id("fy2026")
                .start(LocalDate.of(2026, 4, 1))
                .end(LocalDate.of(2027, 3, 31))
                .build();
        when(leaveCalendarRepository.findByStartLessThanEqualAndEndGreaterThanEqual(date, date))
                .thenReturn(Optional.of(calendar));

        Optional<LeaveCalendar> result = leaveCalendarService.getCalendarFor(date);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("fy2026");
    }

    @Test
    void shouldAutoGenerateCalendarWithNoPublicHolidaysFromLatestCalendar() {
        LeaveCalendar existing = LeaveCalendar.builder()
                .id("fy2026")
                .start(LocalDate.of(2026, 4, 1))
                .end(LocalDate.of(2027, 3, 31))
                .publicHolidays(List.of())
                .build();

        when(leaveCalendarRepository.findByStartLessThanEqualAndEndGreaterThanEqual(
                LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 1))).thenReturn(Optional.empty());
        when(leaveCalendarRepository.findTopByOrderByEndDesc()).thenReturn(Optional.of(existing));
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<LeaveCalendar> result = leaveCalendarService.getCalendarFor(LocalDate.of(2027, 6, 1));

        assertThat(result).isPresent();
        assertThat(result.get().getPublicHolidays()).isEmpty();
    }

    @Test
    void shouldCreateCalendarWithAutoGeneratedIdWhenNoneProvided() {
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .start(LocalDate.of(2026, 4, 1))
                .end(LocalDate.of(2027, 3, 31))
                .build();

        when(leaveCalendarRepository.existsById("2026-04-01_2027-03-31")).thenReturn(false);
        when(leaveCalendarRepository.existsByStartLessThanEqualAndEndGreaterThanEqual(
                LocalDate.of(2027, 3, 31), LocalDate.of(2026, 4, 1))).thenReturn(false);
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveCalendar result = leaveCalendarService.create(leaveCalendar);

        assertThat(result.getId()).isEqualTo("2026-04-01_2027-03-31");
    }
}

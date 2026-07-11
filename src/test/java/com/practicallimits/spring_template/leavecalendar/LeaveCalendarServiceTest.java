package com.practicallimits.spring_template.leavecalendar;

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

    @InjectMocks
    private LeaveCalendarService leaveCalendarService;

    @Test
    void shouldCreateLeaveCalendar() {
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .id("fy2026")
                .start(LocalDate.of(2026, 4, 1))
                .end(LocalDate.of(2027, 3, 31))
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
        assertThat(result.getPublicHolidays()).hasSize(1);
    }

    @Test
    void shouldAutoGenerateFutureCalendarFromLatestCalendar() {
        LeaveCalendar existing = LeaveCalendar.builder()
                .id("fy2026")
                .start(LocalDate.of(2026, 4, 1))
                .end(LocalDate.of(2027, 3, 31))
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
}

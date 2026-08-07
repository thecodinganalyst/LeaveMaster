package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveCalendarMcpToolsTest {

    @Mock
    private LeaveCalendarService leaveCalendarService;

    @InjectMocks
    private LeaveCalendarMcpTools leaveCalendarMcpTools;

    @Test
    void shouldGetAllLeaveCalendars() {
        List<LeaveCalendar> calendars = List.of(LeaveCalendar.builder().id("2025_cal")
                .start(LocalDate.of(2025, 1, 1)).end(LocalDate.of(2025, 12, 31)).build());
        when(leaveCalendarService.findAll()).thenReturn(calendars);

        List<LeaveCalendar> result = leaveCalendarMcpTools.getAllLeaveCalendars();

        assertThat(result).hasSize(1);
        verify(leaveCalendarService).findAll();
    }

    @Test
    void shouldGetLeaveCalendarById() {
        LeaveCalendar calendar = LeaveCalendar.builder().id("2025_cal")
                .start(LocalDate.of(2025, 1, 1)).end(LocalDate.of(2025, 12, 31)).build();
        when(leaveCalendarService.findById("2025_cal")).thenReturn(Optional.of(calendar));

        Optional<LeaveCalendar> result = leaveCalendarMcpTools.getLeaveCalendarById("2025_cal");

        assertThat(result).isPresent();
        verify(leaveCalendarService).findById("2025_cal");
    }

    @Test
    void shouldCreateLeaveCalendar() {
        LeaveCalendar calendar = LeaveCalendar.builder().id("2025_cal")
                .start(LocalDate.of(2025, 1, 1)).end(LocalDate.of(2025, 12, 31)).build();
        when(leaveCalendarService.create(calendar)).thenReturn(calendar);

        LeaveCalendar result = leaveCalendarMcpTools.createLeaveCalendar(calendar);

        assertThat(result.getId()).isEqualTo("2025_cal");
        verify(leaveCalendarService).create(calendar);
    }
}

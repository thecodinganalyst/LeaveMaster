package com.practical.leavemaster.leavecalendar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveCalendarController.class)
class LeaveCalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeaveCalendarService leaveCalendarService;

    @Test
    void shouldCreateLeaveCalendar() throws Exception {
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .id("fy2026")
                .start(LocalDate.of(2026, 4, 1))
                .end(LocalDate.of(2027, 3, 31))
                .publicHolidays(List.of(
                        PublicHoliday.builder().holidayDate(LocalDate.of(2026, 5, 1)).holidayName("Labour Day").build()
                ))
                .build();

        when(leaveCalendarService.create(any(LeaveCalendar.class))).thenReturn(leaveCalendar);

        mockMvc.perform(post("/leave-calendars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leaveCalendar)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("fy2026"))
                .andExpect(jsonPath("$.publicHolidays[0].holidayName").value("Labour Day"));
    }

    @Test
    void shouldReturnCurrentLeaveCalendar() throws Exception {
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .id("fy2026")
                .start(LocalDate.of(2026, 4, 1))
                .end(LocalDate.of(2027, 3, 31))
                .publicHolidays(List.of(
                        PublicHoliday.builder().holidayDate(LocalDate.of(2026, 5, 1)).holidayName("Labour Day").build()
                ))
                .build();

        when(leaveCalendarService.getCalendarFor(LocalDate.of(2026, 4, 15))).thenReturn(Optional.of(leaveCalendar));

        mockMvc.perform(get("/leave-calendars/current").param("date", "2026-04-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("fy2026"))
                .andExpect(jsonPath("$.start").value("2026-04-01"))
                .andExpect(jsonPath("$.publicHolidays[0].holidayDate").value("2026-05-01"));
    }

    @Test
    void shouldReturn404WhenCurrentLeaveCalendarIsUnavailable() throws Exception {
        when(leaveCalendarService.getCalendarFor(LocalDate.of(2025, 12, 31))).thenReturn(Optional.empty());

        mockMvc.perform(get("/leave-calendars/current").param("date", "2025-12-31"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnValidationMessageWhenCreatingInvalidLeaveCalendar() throws Exception {
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .start(LocalDate.of(2027, 3, 31))
                .end(LocalDate.of(2026, 4, 1))
                .build();

        when(leaveCalendarService.create(any(LeaveCalendar.class)))
                .thenThrow(new IllegalArgumentException("Leave calendar start date must be on or before end date"));

        mockMvc.perform(post("/leave-calendars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leaveCalendar)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Leave calendar start date must be on or before end date"));
    }
}

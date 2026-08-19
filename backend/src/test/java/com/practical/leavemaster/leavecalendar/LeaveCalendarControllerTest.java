package com.practical.leavemaster.leavecalendar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveCalendarController.class)
@WithMockUser
class LeaveCalendarControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private LeaveCalendarService leaveCalendarService;
    @MockitoBean private SecurityFilterChain securityFilterChain;

    @Test
    void shouldCreateLeaveCalendar() throws Exception {
        LeaveCalendar leaveCalendar = calendar("fy2026");
        leaveCalendar.setPublicHolidays(List.of(PublicHoliday.builder().holidayDate(LocalDate.of(2026, 5, 1)).holidayName("Labour Day").build()));
        when(leaveCalendarService.create(any(LeaveCalendar.class))).thenReturn(leaveCalendar);
        mockMvc.perform(post("/leave-calendars").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(leaveCalendar)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value("fy2026"))
                .andExpect(jsonPath("$.publicHolidays[0].holidayName").value("Labour Day"));
    }

    @Test
    void shouldUpdateLeaveCalendar() throws Exception {
        LeaveCalendar leaveCalendar = calendar("fy2026");
        when(leaveCalendarService.update(org.mockito.ArgumentMatchers.eq("fy2026"), any(LeaveCalendar.class))).thenReturn(leaveCalendar);
        mockMvc.perform(put("/leave-calendars/fy2026").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(leaveCalendar)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("fy2026"));
    }

    @Test
    void shouldReturnConflictWhenUpdateOverlaps() throws Exception {
        LeaveCalendar leaveCalendar = calendar("fy2026");
        when(leaveCalendarService.update(org.mockito.ArgumentMatchers.eq("fy2026"), any(LeaveCalendar.class)))
                .thenThrow(new LeaveCalendarConflictException("overlap"));
        mockMvc.perform(put("/leave-calendars/fy2026").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(leaveCalendar)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("overlap"));
    }

    @Test
    void shouldDeleteLeaveCalendar() throws Exception {
        doNothing().when(leaveCalendarService).delete("fy2026");
        mockMvc.perform(delete("/leave-calendars/fy2026")).andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnCurrentLeaveCalendar() throws Exception {
        LeaveCalendar leaveCalendar = calendar("fy2026");
        leaveCalendar.setPublicHolidays(List.of(PublicHoliday.builder().holidayDate(LocalDate.of(2026, 5, 1)).holidayName("Labour Day").build()));
        when(leaveCalendarService.getCalendarFor(LocalDate.of(2026, 4, 15))).thenReturn(Optional.of(leaveCalendar));
        mockMvc.perform(get("/leave-calendars/current").param("date", "2026-04-15"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("fy2026"))
                .andExpect(jsonPath("$.start").value("2026-04-01"));
    }

    @Test
    void shouldReturn404WhenCurrentLeaveCalendarIsUnavailable() throws Exception {
        when(leaveCalendarService.getCalendarFor(LocalDate.of(2025, 12, 31))).thenReturn(Optional.empty());
        mockMvc.perform(get("/leave-calendars/current").param("date", "2025-12-31")).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnLeaveCalendarById() throws Exception {
        LeaveCalendar leaveCalendar = calendar("template:SG:2026-01-01_2026-12-31");
        when(leaveCalendarService.findById("template:SG:2026-01-01_2026-12-31")).thenReturn(Optional.of(leaveCalendar));

        mockMvc.perform(get("/api/leave-calendars/template:SG:2026-01-01_2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("template:SG:2026-01-01_2026-12-31"));
    }

    @Test
    void shouldReturn404WhenLeaveCalendarByIdIsNotAccessible() throws Exception {
        when(leaveCalendarService.findById("missing")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/leave-calendars/missing")).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnValidationMessageWhenCreatingInvalidLeaveCalendar() throws Exception {
        LeaveCalendar leaveCalendar = calendar(null);
        when(leaveCalendarService.create(any(LeaveCalendar.class)))
                .thenThrow(new IllegalArgumentException("Leave calendar start date must be on or before end date"));
        mockMvc.perform(post("/leave-calendars").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(leaveCalendar)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Leave calendar start date must be on or before end date"));
    }

    @Test
    void shouldReturnAllLeaveCalendars() throws Exception {
        when(leaveCalendarService.findAll()).thenReturn(List.of(calendar("fy2025"), calendar("fy2026")));
        mockMvc.perform(get("/leave-calendars")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturn409WhenCreatingConflictingLeaveCalendar() throws Exception {
        LeaveCalendar leaveCalendar = calendar(null);
        when(leaveCalendarService.create(any(LeaveCalendar.class))).thenThrow(new LeaveCalendarConflictException("overlap"));
        mockMvc.perform(post("/leave-calendars").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(leaveCalendar)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("overlap"));
    }

    private LeaveCalendar calendar(String id) {
        return LeaveCalendar.builder().id(id).start(LocalDate.of(2026, 4, 1)).end(LocalDate.of(2027, 3, 31)).publicHolidays(List.of()).build();
    }
}

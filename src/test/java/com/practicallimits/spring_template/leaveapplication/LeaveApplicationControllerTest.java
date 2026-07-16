package com.practicallimits.spring_template.leaveapplication;

import com.practicallimits.spring_template.leavecalendar.LeaveCalendarNotFoundException;
import com.practicallimits.spring_template.leaveentitlement.LeaveEntitlement;
import com.practicallimits.spring_template.leavetype.LeaveType;
import com.practicallimits.spring_template.leavetype.LeaveTypeNotFoundException;
import com.practicallimits.spring_template.staff.DaySchedule;
import com.practicallimits.spring_template.staff.Staff;
import com.practicallimits.spring_template.staff.StaffNotFoundException;
import com.practicallimits.spring_template.staff.WorkScheduleDay;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveApplicationController.class)
class LeaveApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeaveApplicationService leaveApplicationService;

    private Staff staff() {
        return Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 1, 1))
                .workSchedule(List.of(
                        WorkScheduleDay.builder().dayOfWeek(DayOfWeek.MONDAY).daySchedule(DaySchedule.FULL).build()
                ))
                .build();
    }

    private LeaveApplication application(String id, LocalDate leaveDate) {
        return LeaveApplication.builder()
                .id(id)
                .staff(staff())
                .leaveDate(leaveDate)
                .leaveType(LeaveType.builder().id("annual").name("Annual Leave").used(true).build())
                .leaveDuration(LeaveDuration.FULL)
                .status(LeaveStatus.DRAFT)
                .applicationDate(LocalDate.now())
                .build();
    }

    @Test
    void shouldReturnAllLeaveApplications() throws Exception {
        when(leaveApplicationService.findAll()).thenReturn(
                List.of(application("id1", LocalDate.of(2024, 1, 8))));

        mockMvc.perform(get("/leave-applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("id1"));
    }

    @Test
    void shouldReturnLeaveApplicationById() throws Exception {
        LeaveApplication app = application("id1", LocalDate.of(2024, 1, 8));
        when(leaveApplicationService.findById("id1")).thenReturn(Optional.of(app));

        mockMvc.perform(get("/leave-applications/id1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id1"))
                .andExpect(jsonPath("$.leaveDate").value("2024-01-08"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void shouldReturn404WhenLeaveApplicationNotFound() throws Exception {
        when(leaveApplicationService.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/leave-applications/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnLeaveApplicationsByStaffId() throws Exception {
        when(leaveApplicationService.findByStaffId(eq("S001"), any(LocalDate.class)))
                .thenReturn(List.of(application("id1", LocalDate.of(2024, 1, 8))));

        mockMvc.perform(get("/leave-applications/staff/S001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldReturnLeaveApplicationsByStaffIdAndDate() throws Exception {
        when(leaveApplicationService.findByStaffId("S001", LocalDate.of(2024, 3, 15)))
                .thenReturn(List.of(application("id1", LocalDate.of(2024, 1, 8))));

        mockMvc.perform(get("/leave-applications/staff/S001").param("date", "2024-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].leaveDate").value("2024-01-08"));
    }

    @Test
    void shouldReturn404WhenStaffNotFoundForLeaveApplications() throws Exception {
        when(leaveApplicationService.findByStaffId(eq("nonexistent"), any(LocalDate.class)))
                .thenThrow(new StaffNotFoundException("nonexistent"));

        mockMvc.perform(get("/leave-applications/staff/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenStaffNotFoundForLeaveApplicationsByDate() throws Exception {
        when(leaveApplicationService.findByStaffId("nonexistent", LocalDate.of(2024, 3, 15)))
                .thenThrow(new StaffNotFoundException("nonexistent"));

        mockMvc.perform(get("/leave-applications/staff/nonexistent").param("date", "2024-03-15"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenLeaveCalendarNotFoundForDate() throws Exception {
        when(leaveApplicationService.findByStaffId("S001", LocalDate.of(2024, 3, 15)))
                .thenThrow(new LeaveCalendarNotFoundException("2024-03-15"));

        mockMvc.perform(get("/leave-applications/staff/S001").param("date", "2024-03-15"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldApplyLeaveAndReturnCreated() throws Exception {
        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .staffId("S001")
                .fromDate(LocalDate.of(2024, 1, 8))
                .toDate(LocalDate.of(2024, 1, 8))
                .leaveTypeId("annual")
                .leaveDuration(LeaveDuration.FULL)
                .status(LeaveStatus.DRAFT)
                .build();
        when(leaveApplicationService.apply(any(LeaveApplicationRequest.class)))
                .thenReturn(List.of(application("id1", LocalDate.of(2024, 1, 8))));

        mockMvc.perform(post("/leave-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("id1"));
    }

    @Test
    void shouldReturn404WhenApplyingWithUnknownStaff() throws Exception {
        when(leaveApplicationService.apply(any(LeaveApplicationRequest.class)))
                .thenThrow(new StaffNotFoundException("nonexistent"));

        mockMvc.perform(post("/leave-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LeaveApplicationRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenApplyingWithInvalidDates() throws Exception {
        when(leaveApplicationService.apply(any(LeaveApplicationRequest.class)))
                .thenThrow(new IllegalArgumentException("fromDate must be on or before toDate"));

        mockMvc.perform(post("/leave-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LeaveApplicationRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("fromDate must be on or before toDate"));
    }

    @Test
    void shouldReturn404WhenApplyingWithUnknownLeaveType() throws Exception {
        when(leaveApplicationService.apply(any(LeaveApplicationRequest.class)))
                .thenThrow(new LeaveTypeNotFoundException("nonexistent"));

        mockMvc.perform(post("/leave-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LeaveApplicationRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateLeaveApplication() throws Exception {
        LeaveApplication updated = application("id1", LocalDate.of(2024, 1, 8));
        updated.setStatus(LeaveStatus.APPROVED);
        updated.setApprovalDate(LocalDate.now());
        when(leaveApplicationService.update(eq("id1"), any(LeaveApplication.class))).thenReturn(updated);

        mockMvc.perform(put("/leave-applications/id1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentLeaveApplication() throws Exception {
        when(leaveApplicationService.update(eq("nonexistent"), any(LeaveApplication.class)))
                .thenThrow(new LeaveApplicationNotFoundException("nonexistent"));

        mockMvc.perform(put("/leave-applications/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LeaveApplication())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteLeaveApplication() throws Exception {
        doNothing().when(leaveApplicationService).delete("id1");

        mockMvc.perform(delete("/leave-applications/id1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentLeaveApplication() throws Exception {
        doThrow(new LeaveApplicationNotFoundException("nonexistent")).when(leaveApplicationService).delete("nonexistent");

        mockMvc.perform(delete("/leave-applications/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn204WhenDeletingPastApprovedLeaveApplicationRequestsCancellation() throws Exception {
        doNothing().when(leaveApplicationService).delete("id1");

        mockMvc.perform(delete("/leave-applications/id1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldApproveCancellationRequest() throws Exception {
        LeaveApplication updated = application("id1", LocalDate.now().minusDays(1));
        updated.setStatus(LeaveStatus.CANCELLED);
        when(leaveApplicationService.approveCancellation("id1")).thenReturn(updated);

        mockMvc.perform(put("/leave-applications/id1/approve-cancellation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturn400WhenApprovingCancellationOnWrongStatus() throws Exception {
        when(leaveApplicationService.approveCancellation("id1"))
                .thenThrow(new IllegalArgumentException("Leave application is not pending cancellation approval"));

        mockMvc.perform(put("/leave-applications/id1/approve-cancellation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Leave application is not pending cancellation approval"));
    }

    @Test
    void shouldReturn404WhenApprovingCancellationOnNonExistentApplication() throws Exception {
        when(leaveApplicationService.approveCancellation("nonexistent"))
                .thenThrow(new LeaveApplicationNotFoundException("nonexistent"));

        mockMvc.perform(put("/leave-applications/nonexistent/approve-cancellation"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectCancellationRequest() throws Exception {
        LeaveApplication updated = application("id1", LocalDate.now().minusDays(1));
        updated.setStatus(LeaveStatus.APPROVED);
        when(leaveApplicationService.rejectCancellation("id1")).thenReturn(updated);

        mockMvc.perform(put("/leave-applications/id1/reject-cancellation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldReturn400WhenRejectingCancellationOnWrongStatus() throws Exception {
        when(leaveApplicationService.rejectCancellation("id1"))
                .thenThrow(new IllegalArgumentException("Leave application is not pending cancellation approval"));

        mockMvc.perform(put("/leave-applications/id1/reject-cancellation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Leave application is not pending cancellation approval"));
    }

    @Test
    void shouldReturn404WhenRejectingCancellationOnNonExistentApplication() throws Exception {
        when(leaveApplicationService.rejectCancellation("nonexistent"))
                .thenThrow(new LeaveApplicationNotFoundException("nonexistent"));

        mockMvc.perform(put("/leave-applications/nonexistent/reject-cancellation"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnLeaveBalancesForStaff() throws Exception {
        LeaveType leaveType = LeaveType.builder().id("annual").name("Annual Leave").used(true).build();
        List<LeaveBalance> balances = List.of(
                new LeaveBalance(leaveType, new BigDecimal("14.00"), new BigDecimal("2.00"), new BigDecimal("12.00")));
        when(leaveApplicationService.getLeaveBalances("S001")).thenReturn(balances);

        mockMvc.perform(get("/leave-applications/staff/S001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].leaveType.id").value("annual"))
                .andExpect(jsonPath("$[0].entitlement").value(14.00))
                .andExpect(jsonPath("$[0].used").value(2.00))
                .andExpect(jsonPath("$[0].balance").value(12.00));
    }

    @Test
    void shouldReturn404WhenGettingBalancesForUnknownStaff() throws Exception {
        when(leaveApplicationService.getLeaveBalances("nonexistent"))
                .thenThrow(new StaffNotFoundException("nonexistent"));

        mockMvc.perform(get("/leave-applications/staff/nonexistent/balance"))
                .andExpect(status().isNotFound());
    }
}

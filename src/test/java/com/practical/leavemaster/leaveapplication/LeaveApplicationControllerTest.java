package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.leavecalendar.LeaveCalendarNotFoundException;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeNotFoundException;
import com.practical.leavemaster.staff.DaySchedule;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffNotFoundException;
import com.practical.leavemaster.staff.WorkScheduleDay;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
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
@WithMockUser
class LeaveApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeaveApplicationService leaveApplicationService;

    @MockitoBean
    private SecurityFilterChain securityFilterChain;

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
    void shouldReturnVisibleLeaveApplicationsForStaff() throws Exception {
        when(leaveApplicationService.findVisibleForStaff("S001")).thenReturn(
                List.of(application("id1", LocalDate.of(2024, 1, 8))));

        mockMvc.perform(get("/leave-applications").param("staffId", "S001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("id1"));
    }

    @Test
    void shouldReturn404WhenStaffNotFoundForVisibleLeaveApplications() throws Exception {
        when(leaveApplicationService.findVisibleForStaff("nonexistent"))
                .thenThrow(new StaffNotFoundException("nonexistent"));

        mockMvc.perform(get("/leave-applications").param("staffId", "nonexistent"))
                .andExpect(status().isNotFound());
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
    void shouldApprovePendingLeaveApplication() throws Exception {
        LeaveApplication updated = application("id1", LocalDate.now().plusDays(1));
        updated.setStatus(LeaveStatus.APPROVED);
        when(leaveApplicationService.approve("id1", "S002")).thenReturn(updated);

        mockMvc.perform(put("/leave-applications/id1/approve").param("approverId", "S002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldReturn400WhenApprovingLeaveApplicationOnWrongStatus() throws Exception {
        when(leaveApplicationService.approve("id1", "S002"))
                .thenThrow(new IllegalArgumentException("Leave application is not pending approval"));

        mockMvc.perform(put("/leave-applications/id1/approve").param("approverId", "S002"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Leave application is not pending approval"));
    }

    @Test
    void shouldReturn404WhenApprovingLeaveApplicationForUnknownApprover() throws Exception {
        when(leaveApplicationService.approve("id1", "unknown"))
                .thenThrow(new StaffNotFoundException("unknown"));

        mockMvc.perform(put("/leave-applications/id1/approve").param("approverId", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenApprovingLeaveApplicationByUnauthorizedApprover() throws Exception {
        when(leaveApplicationService.approve("id1", "S999"))
                .thenThrow(new IllegalArgumentException("Leave application is not pending for this approver"));

        mockMvc.perform(put("/leave-applications/id1/approve").param("approverId", "S999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Leave application is not pending for this approver"));
    }

    @Test
    void shouldRejectPendingLeaveApplication() throws Exception {
        LeaveApplication updated = application("id1", LocalDate.now().plusDays(1));
        updated.setStatus(LeaveStatus.DENIED);
        when(leaveApplicationService.reject("id1", "S002")).thenReturn(updated);

        mockMvc.perform(put("/leave-applications/id1/reject").param("approverId", "S002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DENIED"));
    }

    @Test
    void shouldReturn400WhenRejectingLeaveApplicationOnWrongStatus() throws Exception {
        when(leaveApplicationService.reject("id1", "S002"))
                .thenThrow(new IllegalArgumentException("Leave application is not pending approval"));

        mockMvc.perform(put("/leave-applications/id1/reject").param("approverId", "S002"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Leave application is not pending approval"));
    }

    @Test
    void shouldReturn404WhenRejectingNonExistentLeaveApplication() throws Exception {
        when(leaveApplicationService.reject("nonexistent", "S002"))
                .thenThrow(new LeaveApplicationNotFoundException("nonexistent"));

        mockMvc.perform(put("/leave-applications/nonexistent/reject").param("approverId", "S002"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenRejectingLeaveApplicationForUnknownApprover() throws Exception {
        when(leaveApplicationService.reject("id1", "unknown"))
                .thenThrow(new StaffNotFoundException("unknown"));

        mockMvc.perform(put("/leave-applications/id1/reject").param("approverId", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenRejectingLeaveApplicationByUnauthorizedApprover() throws Exception {
        when(leaveApplicationService.reject("id1", "S999"))
                .thenThrow(new IllegalArgumentException("Leave application is not pending for this approver"));

        mockMvc.perform(put("/leave-applications/id1/reject").param("approverId", "S999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Leave application is not pending for this approver"));
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

    @Test
    void shouldReturnPendingLeaveApplicationsByApproverId() throws Exception {
        LeaveApplication app = LeaveApplication.builder()
                .id("LA001")
                .staff(staff())
                .leaveDate(LocalDate.of(2026, 8, 1))
                .leaveType(LeaveType.builder().id("annual").name("Annual Leave").used(true).build())
                .leaveDuration(com.practical.leavemaster.leaveapplication.LeaveDuration.FULL)
                .status(com.practical.leavemaster.leaveapplication.LeaveStatus.PENDING)
                .applicationDate(LocalDate.of(2026, 7, 1))
                .build();
        when(leaveApplicationService.findPendingByApproverId("S002")).thenReturn(List.of(app));

        mockMvc.perform(get("/leave-applications/approver/S002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("LA001"));
    }

    @Test
    void shouldReturn404WhenApproverNotFoundForPendingApplications() throws Exception {
        when(leaveApplicationService.findPendingByApproverId("nonexistent"))
                .thenThrow(new StaffNotFoundException("nonexistent"));

        mockMvc.perform(get("/leave-applications/approver/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenDeletingLeaveApplicationWithBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Cannot delete")).when(leaveApplicationService).delete("id1");

        mockMvc.perform(delete("/leave-applications/id1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot delete"));
    }
}

package com.practical.leavemaster.leaveapprover;

import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveApproverController.class)
@WithMockUser
class LeaveApproverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeaveApproverService leaveApproverService;

    private Staff staff(String id) {
        return Staff.builder().id(id).name("Name " + id)
                .joinDate(LocalDate.of(2023, 1, 1)).build();
    }

    private LeaveApprover approver(String id) {
        return LeaveApprover.builder()
                .id(id)
                .staff(staff("S001"))
                .approver(staff("S002"))
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .admin(staff("S003"))
                .adminDate(LocalDate.of(2023, 12, 1))
                .build();
    }

    private LeaveApproverRequest request() {
        return LeaveApproverRequest.builder()
                .staffId("S001")
                .approverId("S002")
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .adminId("S003")
                .build();
    }

    @Test
    void shouldReturnAllLeaveApprovers() throws Exception {
        when(leaveApproverService.findAll()).thenReturn(List.of(approver("id1"), approver("id2")));

        mockMvc.perform(get("/leave-approvers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturnLeaveApproverById() throws Exception {
        LeaveApprover la = approver("id1");
        when(leaveApproverService.findById("id1")).thenReturn(Optional.of(la));

        mockMvc.perform(get("/leave-approvers/id1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id1"))
                .andExpect(jsonPath("$.staff.id").value("S001"))
                .andExpect(jsonPath("$.approver.id").value("S002"));
    }

    @Test
    void shouldReturn404WhenLeaveApproverNotFound() throws Exception {
        when(leaveApproverService.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/leave-approvers/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnLeaveApproversByStaffId() throws Exception {
        when(leaveApproverService.findByStaffId("S001")).thenReturn(List.of(approver("id1")));

        mockMvc.perform(get("/leave-approvers/staff/S001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldReturn404WhenStaffNotFoundForApprovers() throws Exception {
        when(leaveApproverService.findByStaffId("nonexistent"))
                .thenThrow(new StaffNotFoundException("nonexistent"));

        mockMvc.perform(get("/leave-approvers/staff/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateLeaveApprover() throws Exception {
        LeaveApprover la = approver("id1");
        when(leaveApproverService.create(any(LeaveApproverRequest.class))).thenReturn(la);

        mockMvc.perform(post("/leave-approvers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id1"))
                .andExpect(jsonPath("$.staff.id").value("S001"));
    }

    @Test
    void shouldReturn404WhenStaffNotFoundOnCreate() throws Exception {
        when(leaveApproverService.create(any(LeaveApproverRequest.class)))
                .thenThrow(new StaffNotFoundException("nonexistent"));

        mockMvc.perform(post("/leave-approvers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenInvalidDatesOnCreate() throws Exception {
        when(leaveApproverService.create(any(LeaveApproverRequest.class)))
                .thenThrow(new IllegalArgumentException("effectiveTo must be after effectiveFrom"));

        LeaveApproverRequest badRequest = LeaveApproverRequest.builder()
                .staffId("S001").approverId("S002").adminId("S003")
                .effectiveFrom(LocalDate.of(2024, 6, 1))
                .effectiveTo(LocalDate.of(2024, 1, 1))
                .build();

        mockMvc.perform(post("/leave-approvers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("effectiveTo must be after effectiveFrom"));
    }

    @Test
    void shouldUpdateLeaveApprover() throws Exception {
        LeaveApprover updated = LeaveApprover.builder()
                .id("id1")
                .staff(staff("S001"))
                .approver(staff("S004"))
                .effectiveFrom(LocalDate.of(2024, 6, 1))
                .effectiveTo(LocalDate.of(2025, 6, 1))
                .admin(staff("S003"))
                .adminDate(LocalDate.of(2024, 5, 1))
                .build();
        when(leaveApproverService.update(eq("id1"), any(LeaveApproverRequest.class))).thenReturn(updated);

        LeaveApproverRequest updateRequest = LeaveApproverRequest.builder()
                .staffId("S001").approverId("S004").adminId("S003")
                .effectiveFrom(LocalDate.of(2024, 6, 1))
                .effectiveTo(LocalDate.of(2025, 6, 1))
                .build();

        mockMvc.perform(put("/leave-approvers/id1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approver.id").value("S004"))
                .andExpect(jsonPath("$.effectiveTo").value("2025-06-01"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentLeaveApprover() throws Exception {
        when(leaveApproverService.update(eq("nonexistent"), any(LeaveApproverRequest.class)))
                .thenThrow(new LeaveApproverNotFoundException("nonexistent"));

        mockMvc.perform(put("/leave-approvers/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenStaffNotFoundOnUpdate() throws Exception {
        when(leaveApproverService.update(eq("id1"), any(LeaveApproverRequest.class)))
                .thenThrow(new StaffNotFoundException("nonexistent"));

        mockMvc.perform(put("/leave-approvers/id1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenInvalidDatesOnUpdate() throws Exception {
        when(leaveApproverService.update(eq("id1"), any(LeaveApproverRequest.class)))
                .thenThrow(new IllegalArgumentException("effectiveTo must be after effectiveFrom"));

        LeaveApproverRequest badRequest = LeaveApproverRequest.builder()
                .staffId("S001").approverId("S002").adminId("S003")
                .effectiveFrom(LocalDate.of(2024, 6, 1))
                .effectiveTo(LocalDate.of(2024, 1, 1))
                .build();

        mockMvc.perform(put("/leave-approvers/id1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("effectiveTo must be after effectiveFrom"));
    }

    @Test
    void shouldDeleteLeaveApprover() throws Exception {
        doNothing().when(leaveApproverService).delete("id1");

        mockMvc.perform(delete("/leave-approvers/id1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentLeaveApprover() throws Exception {
        doThrow(new LeaveApproverNotFoundException("nonexistent")).when(leaveApproverService).delete("nonexistent");

        mockMvc.perform(delete("/leave-approvers/nonexistent"))
                .andExpect(status().isNotFound());
    }
}


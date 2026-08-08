package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveapprover.LeaveApprover;
import com.practical.leavemaster.leaveapprover.LeaveApproverRequest;
import com.practical.leavemaster.leaveapprover.LeaveApproverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveApproverMcpToolsTest {

    @Mock
    private LeaveApproverService leaveApproverService;

    @InjectMocks
    private LeaveApproverMcpTools leaveApproverMcpTools;

    @Test
    void shouldGetAllLeaveApprovers() {
        List<LeaveApprover> approvers = List.of(LeaveApprover.builder().id("ap1").build());
        when(leaveApproverService.findAll()).thenReturn(approvers);

        List<LeaveApprover> result = leaveApproverMcpTools.getAllLeaveApprovers();

        assertThat(result).hasSize(1);
        verify(leaveApproverService).findAll();
    }

    @Test
    void shouldGetLeaveApproversByStaffId() {
        List<LeaveApprover> approvers = List.of(LeaveApprover.builder().id("ap1").build());
        when(leaveApproverService.findByStaffId("s1")).thenReturn(approvers);

        List<LeaveApprover> result = leaveApproverMcpTools.getLeaveApproversByStaffId("s1");

        assertThat(result).hasSize(1);
        verify(leaveApproverService).findByStaffId("s1");
    }

    @Test
    void shouldGetLeaveApproverById() {
        LeaveApprover approver = LeaveApprover.builder().id("ap1").build();
        when(leaveApproverService.findById("ap1")).thenReturn(Optional.of(approver));

        Optional<LeaveApprover> result = leaveApproverMcpTools.getLeaveApproverById("ap1");

        assertThat(result).isPresent();
        verify(leaveApproverService).findById("ap1");
    }

    @Test
    void shouldCreateLeaveApprover() {
        LeaveApproverRequest request = LeaveApproverRequest.builder().staffId("s1").approverId("a1").build();
        LeaveApprover approver = LeaveApprover.builder().id("ap1").build();
        when(leaveApproverService.create(request)).thenReturn(approver);

        LeaveApprover result = leaveApproverMcpTools.createLeaveApprover(request);

        assertThat(result.getId()).isEqualTo("ap1");
        verify(leaveApproverService).create(request);
    }

    @Test
    void shouldUpdateLeaveApprover() {
        LeaveApproverRequest request = LeaveApproverRequest.builder().staffId("s1").approverId("a1").build();
        LeaveApprover approver = LeaveApprover.builder().id("ap1").build();
        when(leaveApproverService.update("ap1", request)).thenReturn(approver);

        LeaveApprover result = leaveApproverMcpTools.updateLeaveApprover("ap1", request);

        assertThat(result.getId()).isEqualTo("ap1");
        verify(leaveApproverService).update("ap1", request);
    }

    @Test
    void shouldDeleteLeaveApprover() {
        doNothing().when(leaveApproverService).delete("ap1");

        leaveApproverMcpTools.deleteLeaveApprover("ap1");

        verify(leaveApproverService).delete("ap1");
    }
}

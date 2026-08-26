package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRequest;
import com.practical.leavemaster.leaveapplication.LeaveApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveApplicationMcpToolsTest {

    @Mock
    private LeaveApplicationService leaveApplicationService;

    @Mock
    private LeaveBalanceAssistantReadService leaveBalanceAssistantReadService;

    @InjectMocks
    private LeaveApplicationMcpTools leaveApplicationMcpTools;

    @Test
    void shouldGetAllLeaveApplications() {
        List<LeaveApplication> applications = List.of(LeaveApplication.builder().id("la1").build());
        when(leaveApplicationService.findAll()).thenReturn(applications);

        List<LeaveApplication> result = leaveApplicationMcpTools.getAllLeaveApplications();

        assertThat(result).hasSize(1);
        verify(leaveApplicationService).findAll();
    }

    @Test
    void shouldGetLeaveApplicationById() {
        LeaveApplication application = LeaveApplication.builder().id("la1").build();
        when(leaveApplicationService.findById("la1")).thenReturn(Optional.of(application));

        Optional<LeaveApplication> result = leaveApplicationMcpTools.getLeaveApplicationById("la1");

        assertThat(result).isPresent();
        verify(leaveApplicationService).findById("la1");
    }

    @Test
    void shouldGetLeaveApplicationsByStaffId() {
        List<LeaveApplication> applications = List.of(LeaveApplication.builder().id("la1").build());
        when(leaveApplicationService.findByStaffId("s1")).thenReturn(applications);

        List<LeaveApplication> result = leaveApplicationMcpTools.getLeaveApplicationsByStaffId("s1");

        assertThat(result).hasSize(1);
        verify(leaveApplicationService).findByStaffId("s1");
    }

    @Test
    void shouldGetVisibleLeaveApplicationsForStaff() {
        List<LeaveApplication> applications = List.of(LeaveApplication.builder().id("la1").build());
        when(leaveApplicationService.findVisibleForStaff("s1")).thenReturn(applications);

        List<LeaveApplication> result = leaveApplicationMcpTools.getVisibleLeaveApplicationsForStaff("s1");

        assertThat(result).hasSize(1);
        verify(leaveApplicationService).findVisibleForStaff("s1");
    }

    @Test
    void shouldGetPendingLeaveApplicationsByApproverId() {
        List<LeaveApplication> applications = List.of(LeaveApplication.builder().id("la1").build());
        when(leaveApplicationService.findPendingByApproverId("a1")).thenReturn(applications);

        List<LeaveApplication> result = leaveApplicationMcpTools.getPendingLeaveApplicationsByApproverId("a1");

        assertThat(result).hasSize(1);
        verify(leaveApplicationService).findPendingByApproverId("a1");
    }

    @Test
    void shouldGetLeaveBalancesFromAssistantReadService() {
        LeaveBalanceAssistantReadService.LeaveBalanceResult balance =
                new LeaveBalanceAssistantReadService.LeaveBalanceResult(
                        "annual", "Annual Leave",
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                        new BigDecimal("14.00"), BigDecimal.ONE, new BigDecimal("13.00"),
                        "policy-1", new BigDecimal("14.00"), BigDecimal.ZERO, BigDecimal.ZERO, null);
        when(leaveBalanceAssistantReadService.findByStaffId("s1")).thenReturn(List.of(balance));

        List<LeaveBalanceAssistantReadService.LeaveBalanceResult> result =
                leaveApplicationMcpTools.getLeaveBalances("s1");

        assertThat(result).containsExactly(balance);
        verify(leaveBalanceAssistantReadService).findByStaffId("s1");
        verify(leaveApplicationService, never()).getLeaveBalances(anyString());
    }

    @Test
    void shouldApplyForLeave() {
        LeaveApplicationRequest request = LeaveApplicationRequest.builder().staffId("s1").build();
        List<LeaveApplication> applications = List.of(LeaveApplication.builder().id("la1").build());
        when(leaveApplicationService.apply(request, null)).thenReturn(applications);

        List<LeaveApplication> result = leaveApplicationMcpTools.applyForLeave(request);

        assertThat(result).hasSize(1);
        verify(leaveApplicationService).apply(request, null);
    }

    @Test
    void shouldUpdateLeaveApplication() {
        LeaveApplication application = LeaveApplication.builder().id("la1").build();
        when(leaveApplicationService.update("la1", application)).thenReturn(application);

        LeaveApplication result = leaveApplicationMcpTools.updateLeaveApplication("la1", application);

        assertThat(result.getId()).isEqualTo("la1");
        verify(leaveApplicationService).update("la1", application);
    }

    @Test
    void shouldDeleteLeaveApplication() {
        doNothing().when(leaveApplicationService).delete("la1");

        leaveApplicationMcpTools.deleteLeaveApplication("la1");

        verify(leaveApplicationService).delete("la1");
    }

    @Test
    void shouldApproveLeaveApplication() {
        LeaveApplication application = LeaveApplication.builder().id("la1").build();
        when(leaveApplicationService.approve("la1", "a1")).thenReturn(application);

        LeaveApplication result = leaveApplicationMcpTools.approveLeaveApplication("la1", "a1");

        assertThat(result.getId()).isEqualTo("la1");
        verify(leaveApplicationService).approve("la1", "a1");
    }

    @Test
    void shouldRejectLeaveApplication() {
        LeaveApplication application = LeaveApplication.builder().id("la1").build();
        when(leaveApplicationService.reject("la1", "a1")).thenReturn(application);

        LeaveApplication result = leaveApplicationMcpTools.rejectLeaveApplication("la1", "a1");

        assertThat(result.getId()).isEqualTo("la1");
        verify(leaveApplicationService).reject("la1", "a1");
    }

    @Test
    void shouldApproveCancellation() {
        LeaveApplication application = LeaveApplication.builder().id("la1").build();
        when(leaveApplicationService.approveCancellation("la1")).thenReturn(application);

        LeaveApplication result = leaveApplicationMcpTools.approveCancellation("la1");

        assertThat(result.getId()).isEqualTo("la1");
        verify(leaveApplicationService).approveCancellation("la1");
    }

    @Test
    void shouldRejectCancellation() {
        LeaveApplication application = LeaveApplication.builder().id("la1").build();
        when(leaveApplicationService.rejectCancellation("la1")).thenReturn(application);

        LeaveApplication result = leaveApplicationMcpTools.rejectCancellation("la1");

        assertThat(result.getId()).isEqualTo("la1");
        verify(leaveApplicationService).rejectCancellation("la1");
    }
}

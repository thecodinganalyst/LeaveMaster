package com.practical.leavemaster.mcp;

import com.practical.leavemaster.assistant.LeaveInsightService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LeaveInsightMcpToolsTest {
    @Test
    void delegatesAllStructuredInsightTools() {
        LeaveInsightService service=mock(LeaveInsightService.class);
        LeaveInsightMcpTools tools=new LeaveInsightMcpTools(service);
        var finding=new LeaveInsightService.StaffLeaveFinding("MISSING_APPROVER","S1","One","reason","evidence","T1");
        var action=new LeaveInsightService.PendingAction("A1","S1","One",LocalDate.of(2026,9,30),"Annual Leave","T1");
        var leave=new LeaveInsightService.UpcomingLeave("S1","One",LocalDate.of(2026,9,30),"APPROVED","Annual Leave");
        when(service.findStaffAnomalies("S1",LocalDate.of(2026,9,24))).thenReturn(List.of(finding));
        when(service.pendingActionsForApprover("M1")).thenReturn(List.of(action));
        when(service.upcomingLeaveForApprover("M1",LocalDate.of(2026,9,28),LocalDate.of(2026,10,4))).thenReturn(List.of(leave));
        assertThat(tools.getStaffLeaveAnomalies("S1",LocalDate.of(2026,9,24))).containsExactly(finding);
        assertThat(tools.getPendingLeaveActions("M1")).containsExactly(action);
        assertThat(tools.getPermittedTeamLeave("M1",LocalDate.of(2026,9,28),LocalDate.of(2026,10,4))).containsExactly(leave);
    }
}

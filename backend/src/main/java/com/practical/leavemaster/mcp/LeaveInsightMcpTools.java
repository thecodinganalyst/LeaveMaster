package com.practical.leavemaster.mcp;

import com.practical.leavemaster.assistant.LeaveInsightService;
import com.practical.leavemaster.rbac.RbacPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LeaveInsightMcpTools {
    private final LeaveInsightService insightService;

    @Tool(description = "Return deterministic leave-data anomaly findings for one staff member, with reason and evidence. Never invent additional anomalies.")
    @PreAuthorize("hasAuthority('" + RbacPermissions.STAFF_READ + "') and @leaveAuthorization.canReadStaffData(authentication, #staffId)")
    public List<LeaveInsightService.StaffLeaveFinding> getStaffLeaveAnomalies(String staffId, LocalDate asOf) {
        return insightService.findStaffAnomalies(staffId, asOf);
    }

    @Tool(description = "Return pending leave requests that require action by the authenticated manager/approver.")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_READ + "') and @leaveAuthorization.canActAsApprover(authentication, #approverId)")
    public List<LeaveInsightService.PendingAction> getPendingLeaveActions(String approverId) {
        return insightService.pendingActionsForApprover(approverId);
    }

    @Tool(description = "Return approved or pending leave in a date range only for staff assigned to the authenticated manager/approver.")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_READ + "') and @leaveAuthorization.canActAsApprover(authentication, #approverId)")
    public List<LeaveInsightService.UpcomingLeave> getPermittedTeamLeave(String approverId, LocalDate from, LocalDate to) {
        return insightService.upcomingLeaveForApprover(approverId, from, to);
    }
}

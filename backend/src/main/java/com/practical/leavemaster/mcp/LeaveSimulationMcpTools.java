package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveapplication.LeaveDuration;
import com.practical.leavemaster.leaveapplication.LeaveSimulationService;
import com.practical.leavemaster.rbac.RbacPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class LeaveSimulationMcpTools {
    private final LeaveSimulationService simulationService;

    @Tool(description = "READ-ONLY SIMULATION: calculate chargeable leave dates and projected balance for hypothetical leave. This never creates a leave request. Always describe the result as a simulation and repeat its assumptions.")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_READ + "') and @leaveAuthorization.canReadStaffData(authentication, #staffId)")
    public LeaveSimulationService.LeaveUsageSimulation simulateLeaveUsage(
            String staffId, String leaveTypeId, LocalDate from, LocalDate to, LeaveDuration duration) {
        return simulationService.simulateLeaveUsage(staffId, leaveTypeId, from, to, duration);
    }

    @Tool(description = "READ-ONLY SIMULATION: project an entitlement if the employee terminated on the supplied date. Never changes staff or entitlement data.")
    @PreAuthorize("hasAuthority('" + RbacPermissions.STAFF_READ + "') and @leaveAuthorization.canReadStaffData(authentication, #staffId)")
    public LeaveSimulationService.EntitlementSimulation simulateTerminationEntitlement(
            String staffId, String leaveTypeId, LocalDate terminationDate) {
        return simulationService.simulateTerminationEntitlement(staffId, leaveTypeId, terminationDate);
    }

    @Tool(description = "READ-ONLY SIMULATION: project entitlement under a hypothetical full-period policy amount. Never changes policy or entitlement data.")
    @PreAuthorize("hasAuthority('" + RbacPermissions.STAFF_READ + "') and @leaveAuthorization.canReadStaffData(authentication, #staffId)")
    public LeaveSimulationService.EntitlementSimulation simulatePolicyEntitlement(
            String staffId, String leaveTypeId, BigDecimal proposedFullPeriodAmount) {
        return simulationService.simulatePolicyValue(staffId, leaveTypeId, proposedFullPeriodAmount);
    }
}

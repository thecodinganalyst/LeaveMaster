package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveapplication.LeaveDuration;
import com.practical.leavemaster.leaveapplication.LeaveSimulationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LeaveSimulationMcpToolsTest {
    @Test
    void delegatesAllReadOnlySimulationTools() {
        LeaveSimulationService service=mock(LeaveSimulationService.class);
        LeaveSimulationMcpTools tools=new LeaveSimulationMcpTools(service);
        var usage=new LeaveSimulationService.LeaveUsageSimulation(true,"E","AL",LocalDate.now(),LocalDate.now(),
                LeaveDuration.FULL,List.of(),BigDecimal.ONE,BigDecimal.TEN,BigDecimal.ZERO,BigDecimal.TEN,
                new BigDecimal("9"),"SG",List.of());
        var entitlement=new LeaveSimulationService.EntitlementSimulation(true,"E","AL",BigDecimal.TEN,
                BigDecimal.ONE,LocalDate.now(),LocalDate.now(),"SG",List.of());
        when(service.simulateLeaveUsage(any(),any(),any(),any(),any())).thenReturn(usage);
        when(service.simulateTerminationEntitlement(any(),any(),any())).thenReturn(entitlement);
        when(service.simulatePolicyValue(any(),any(),any())).thenReturn(entitlement);

        assertThat(tools.simulateLeaveUsage("E","AL",LocalDate.now(),LocalDate.now(),LeaveDuration.FULL)).isSameAs(usage);
        assertThat(tools.simulateTerminationEntitlement("E","AL",LocalDate.now())).isSameAs(entitlement);
        assertThat(tools.simulatePolicyEntitlement("E","AL",BigDecimal.TEN)).isSameAs(entitlement);
    }
}

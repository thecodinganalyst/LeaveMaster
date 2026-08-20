package com.practical.leavemaster.config;

import com.practical.leavemaster.mcp.AppRoleMcpTools;
import com.practical.leavemaster.mcp.AppUserMcpTools;
import com.practical.leavemaster.mcp.LeaveApplicationMcpTools;
import com.practical.leavemaster.mcp.LeaveApproverMcpTools;
import com.practical.leavemaster.mcp.LeaveCalendarMcpTools;
import com.practical.leavemaster.mcp.LeaveTypeMcpTools;
import com.practical.leavemaster.mcp.StaffMcpTools;
import com.practical.leavemaster.mcp.TenantMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfiguration {

    @Bean
    public ToolCallbackProvider leaveMasterTools(
            LeaveTypeMcpTools leaveTypeMcpTools,
            StaffMcpTools staffMcpTools,
            TenantMcpTools tenantMcpTools,
            LeaveApplicationMcpTools leaveApplicationMcpTools,
            LeaveApproverMcpTools leaveApproverMcpTools,
            LeaveCalendarMcpTools leaveCalendarMcpTools,
            AppUserMcpTools appUserMcpTools,
            AppRoleMcpTools appRoleMcpTools
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        leaveTypeMcpTools,
                        staffMcpTools,
                        tenantMcpTools,
                        leaveApplicationMcpTools,
                        leaveApproverMcpTools,
                        leaveCalendarMcpTools,
                        appUserMcpTools,
                        appRoleMcpTools
                )
                .build();
    }
}

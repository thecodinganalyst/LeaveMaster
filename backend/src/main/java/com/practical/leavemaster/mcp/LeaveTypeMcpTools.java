package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeService;
import com.practical.leavemaster.rbac.RbacPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeaveTypeMcpTools {

    private final LeaveTypeService leaveTypeService;

    @Tool(description = "Get all leave types")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_TYPE_READ + "')")
    public List<LeaveType> getAllLeaveTypes() {
        return leaveTypeService.findAll();
    }

    @Tool(description = "Get a leave type by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_TYPE_READ + "')")
    public Optional<LeaveType> getLeaveTypeById(String id) {
        return leaveTypeService.findById(id);
    }

    @Tool(description = "Create a new leave type")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_TYPE_WRITE + "')")
    public LeaveType createLeaveType(LeaveType leaveType) {
        return leaveTypeService.save(leaveType);
    }

    @Tool(description = "Update an existing leave type")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_TYPE_WRITE + "')")
    public LeaveType updateLeaveType(String id, LeaveType leaveType) {
        return leaveTypeService.update(id, leaveType);
    }

    @Tool(description = "Delete a leave type by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_TYPE_WRITE + "')")
    public void deleteLeaveType(String id) {
        leaveTypeService.delete(id);
    }
}

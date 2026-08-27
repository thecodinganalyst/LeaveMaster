package com.practical.leavemaster.mcp;

import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffService;
import com.practical.leavemaster.staff.TerminationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StaffMcpTools {

    private final StaffService staffService;
    private final StaffAssistantReadService staffAssistantReadService;

    @Tool(description = "Get all staff members")
    @PreAuthorize("hasAuthority('" + RbacPermissions.STAFF_READ + "')")
    public List<StaffAssistantReadService.StaffResult> getAllStaff() {
        return staffAssistantReadService.findAll();
    }

    @Tool(description = "Get a staff member by ID. Use for broad staff-profile questions; for a question about one leave entitlement value, prefer getStaffLeaveEntitlement.")
    @PreAuthorize("hasAuthority('" + RbacPermissions.STAFF_READ + "')")
    public Optional<StaffAssistantReadService.StaffResult> getStaffById(String id) {
        return staffAssistantReadService.findById(id);
    }

    @Tool(description = "Get focused evidence for one staff leave entitlement. Prefer this when the user asks why a specific entitlement has a particular value. leaveType may be the leave type ID or human-readable name; year may be omitted for the current year. Returns only the staff and entitlement fields needed for explanation, not the full staff profile.")
    @PreAuthorize("hasAuthority('" + RbacPermissions.STAFF_READ + "')")
    public Optional<StaffAssistantReadService.StaffLeaveEntitlementResult> getStaffLeaveEntitlement(
            String staffId, String leaveType, Integer year) {
        return staffAssistantReadService.findLeaveEntitlement(staffId, leaveType, year);
    }

    @Tool(description = "Create a new staff member")
    @PreAuthorize("hasAuthority('" + RbacPermissions.STAFF_WRITE + "')")
    public Staff createStaff(Staff staff) {
        return staffService.save(staff);
    }

    @Tool(description = "Update an existing staff member")
    @PreAuthorize("hasAuthority('" + RbacPermissions.STAFF_WRITE + "')")
    public Staff updateStaff(String id, Staff staff) {
        return staffService.update(id, staff);
    }

    @Tool(description = "Delete a staff member by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.STAFF_WRITE + "')")
    public void deleteStaff(String id) {
        staffService.delete(id);
    }

    @Tool(description = "Terminate a staff member on a given termination date (format: YYYY-MM-DD)")
    @PreAuthorize("hasAuthority('" + RbacPermissions.STAFF_WRITE + "')")
    public TerminationResult terminateStaff(String id, LocalDate termDate) {
        return staffService.terminate(id, termDate);
    }
}

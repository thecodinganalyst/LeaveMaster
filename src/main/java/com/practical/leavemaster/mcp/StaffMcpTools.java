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

    @Tool(description = "Get all staff members")
    @PreAuthorize("hasAuthority('" + RbacPermissions.STAFF_READ + "')")
    public List<Staff> getAllStaff() {
        return staffService.findAll();
    }

    @Tool(description = "Get a staff member by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.STAFF_READ + "')")
    public Optional<Staff> getStaffById(String id) {
        return staffService.findById(id);
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

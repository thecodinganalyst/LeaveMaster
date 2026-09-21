package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRequest;
import com.practical.leavemaster.leaveapplication.LeaveApplicationService;
import com.practical.leavemaster.rbac.RbacPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeaveApplicationMcpTools {

    private final LeaveApplicationService leaveApplicationService;
    private final LeaveBalanceAssistantReadService leaveBalanceAssistantReadService;

    @Tool(description = "Get all leave applications")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_READ + "')")
    public List<LeaveApplicationReadResult> getAllLeaveApplications() {
        return leaveApplicationService.findAll().stream().map(this::toReadResult).toList();
    }

    @Tool(description = "Get a leave application by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_READ + "')")
    public Optional<LeaveApplicationReadResult> getLeaveApplicationById(String id) {
        return leaveApplicationService.findById(id).map(this::toReadResult);
    }

    @Tool(description = "Get all leave applications for a staff member by staff ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_READ + "')")
    public List<LeaveApplicationReadResult> getLeaveApplicationsByStaffId(String staffId) {
        return leaveApplicationService.findByStaffId(staffId).stream().map(this::toReadResult).toList();
    }

    @Tool(description = "Get visible leave applications for a staff member, including their own and team members pending/approved")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_READ + "')")
    public List<LeaveApplicationReadResult> getVisibleLeaveApplicationsForStaff(String staffId) {
        return leaveApplicationService.findVisibleForStaff(staffId).stream().map(this::toReadResult).toList();
    }

    @Tool(description = "Get pending leave applications awaiting approval by a given approver")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_READ + "')")
    public List<LeaveApplicationReadResult> getPendingLeaveApplicationsByApproverId(String approverId) {
        return leaveApplicationService.findPendingByApproverId(approverId).stream().map(this::toReadResult).toList();
    }

    @Tool(description = "Get leave balances for a staff member by staff ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_READ + "') and @leaveAuthorization.canAccessStaff(authentication, #staffId)")
    public List<LeaveBalanceAssistantReadService.LeaveBalanceResult> getLeaveBalances(String staffId) {
        return leaveBalanceAssistantReadService.findByStaffId(staffId);
    }

    @Tool(description = "Apply for leave (without attachment)")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_WRITE + "')")
    public List<LeaveApplication> applyForLeave(LeaveApplicationRequest request) {
        return leaveApplicationService.apply(request, null);
    }

    @Tool(description = "Update a leave application by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_WRITE + "')")
    public LeaveApplication updateLeaveApplication(String id, LeaveApplication leaveApplication) {
        return leaveApplicationService.update(id, leaveApplication);
    }

    @Tool(description = "Delete a leave application by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_WRITE + "')")
    public void deleteLeaveApplication(String id) {
        leaveApplicationService.delete(id);
    }

    @Tool(description = "Approve a leave application by ID with the given approver ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_APPROVE + "')")
    public LeaveApplication approveLeaveApplication(String id, String approverId) {
        return leaveApplicationService.approve(id, approverId);
    }

    @Tool(description = "Reject a leave application by ID with the given approver ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_APPROVE + "')")
    public LeaveApplication rejectLeaveApplication(String id, String approverId) {
        return leaveApplicationService.reject(id, approverId);
    }

    @Tool(description = "Approve the cancellation of a leave application by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_APPROVE + "')")
    public LeaveApplication approveCancellation(String id) {
        return leaveApplicationService.approveCancellation(id);
    }

    @Tool(description = "Reject the cancellation of a leave application by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_APPLICATION_APPROVE + "')")
    public LeaveApplication rejectCancellation(String id) {
        return leaveApplicationService.rejectCancellation(id);
    }

    private LeaveApplicationReadResult toReadResult(LeaveApplication application) {
        return new LeaveApplicationReadResult(
                application.getId(),
                application.getStaff() == null ? null : application.getStaff().getId(),
                application.getLeaveDate(),
                application.getLeaveType() == null ? null : application.getLeaveType().getId(),
                application.getLeaveType() == null ? null : application.getLeaveType().getName(),
                application.getLeaveDuration(),
                application.getStatus(),
                application.getAttachmentUrl(),
                application.getApprover() == null ? null : application.getApprover().getId(),
                application.getApplicationDate(),
                application.getApprovalDate(),
                application.getEventEntitlementId());
    }

    public record LeaveApplicationReadResult(
            String id,
            String staffId,
            LocalDate leaveDate,
            String leaveTypeId,
            String leaveTypeName,
            com.practical.leavemaster.leaveapplication.LeaveDuration leaveDuration,
            com.practical.leavemaster.leaveapplication.LeaveStatus status,
            String attachmentUrl,
            String approverId,
            LocalDate applicationDate,
            LocalDate approvalDate,
            String eventEntitlementId
    ) {
    }
}

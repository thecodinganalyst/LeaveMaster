package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRequest;
import com.practical.leavemaster.leaveapplication.LeaveApplicationService;
import com.practical.leavemaster.leaveapplication.LeaveBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeaveApplicationMcpTools {

    private final LeaveApplicationService leaveApplicationService;

    @Tool(description = "Get all leave applications")
    public List<LeaveApplication> getAllLeaveApplications() {
        return leaveApplicationService.findAll();
    }

    @Tool(description = "Get a leave application by ID")
    public Optional<LeaveApplication> getLeaveApplicationById(String id) {
        return leaveApplicationService.findById(id);
    }

    @Tool(description = "Get all leave applications for a staff member by staff ID")
    public List<LeaveApplication> getLeaveApplicationsByStaffId(String staffId) {
        return leaveApplicationService.findByStaffId(staffId);
    }

    @Tool(description = "Get visible leave applications for a staff member, including their own and team members pending/approved")
    public List<LeaveApplication> getVisibleLeaveApplicationsForStaff(String staffId) {
        return leaveApplicationService.findVisibleForStaff(staffId);
    }

    @Tool(description = "Get pending leave applications awaiting approval by a given approver")
    public List<LeaveApplication> getPendingLeaveApplicationsByApproverId(String approverId) {
        return leaveApplicationService.findPendingByApproverId(approverId);
    }

    @Tool(description = "Get leave balances for a staff member by staff ID")
    public List<LeaveBalance> getLeaveBalances(String staffId) {
        return leaveApplicationService.getLeaveBalances(staffId);
    }

    @Tool(description = "Apply for leave (without attachment)")
    public List<LeaveApplication> applyForLeave(LeaveApplicationRequest request) {
        return leaveApplicationService.apply(request, null);
    }

    @Tool(description = "Update a leave application by ID")
    public LeaveApplication updateLeaveApplication(String id, LeaveApplication leaveApplication) {
        return leaveApplicationService.update(id, leaveApplication);
    }

    @Tool(description = "Delete a leave application by ID")
    public void deleteLeaveApplication(String id) {
        leaveApplicationService.delete(id);
    }

    @Tool(description = "Approve a leave application by ID with the given approver ID")
    public LeaveApplication approveLeaveApplication(String id, String approverId) {
        return leaveApplicationService.approve(id, approverId);
    }

    @Tool(description = "Reject a leave application by ID with the given approver ID")
    public LeaveApplication rejectLeaveApplication(String id, String approverId) {
        return leaveApplicationService.reject(id, approverId);
    }

    @Tool(description = "Approve the cancellation of a leave application by ID")
    public LeaveApplication approveCancellation(String id) {
        return leaveApplicationService.approveCancellation(id);
    }

    @Tool(description = "Reject the cancellation of a leave application by ID")
    public LeaveApplication rejectCancellation(String id) {
        return leaveApplicationService.rejectCancellation(id);
    }
}

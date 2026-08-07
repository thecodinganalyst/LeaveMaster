package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveapprover.LeaveApprover;
import com.practical.leavemaster.leaveapprover.LeaveApproverRequest;
import com.practical.leavemaster.leaveapprover.LeaveApproverService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeaveApproverMcpTools {

    private final LeaveApproverService leaveApproverService;

    @Tool(description = "Get all leave approver assignments")
    public List<LeaveApprover> getAllLeaveApprovers() {
        return leaveApproverService.findAll();
    }

    @Tool(description = "Get leave approver assignments for a staff member by staff ID")
    public List<LeaveApprover> getLeaveApproversByStaffId(String staffId) {
        return leaveApproverService.findByStaffId(staffId);
    }

    @Tool(description = "Get a leave approver assignment by ID")
    public Optional<LeaveApprover> getLeaveApproverById(String id) {
        return leaveApproverService.findById(id);
    }

    @Tool(description = "Create a new leave approver assignment")
    public LeaveApprover createLeaveApprover(LeaveApproverRequest request) {
        return leaveApproverService.create(request);
    }

    @Tool(description = "Update an existing leave approver assignment by ID")
    public LeaveApprover updateLeaveApprover(String id, LeaveApproverRequest request) {
        return leaveApproverService.update(id, request);
    }

    @Tool(description = "Delete a leave approver assignment by ID")
    public void deleteLeaveApprover(String id) {
        leaveApproverService.delete(id);
    }
}

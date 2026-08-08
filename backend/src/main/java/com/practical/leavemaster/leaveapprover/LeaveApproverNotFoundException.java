package com.practical.leavemaster.leaveapprover;

public class LeaveApproverNotFoundException extends RuntimeException {
    public LeaveApproverNotFoundException(String id) {
        super("Leave approver not found: " + id);
    }
}

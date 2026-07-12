package com.practicallimits.spring_template.leaveapprover;

public class LeaveApproverNotFoundException extends RuntimeException {
    public LeaveApproverNotFoundException(String id) {
        super("Leave approver not found: " + id);
    }
}

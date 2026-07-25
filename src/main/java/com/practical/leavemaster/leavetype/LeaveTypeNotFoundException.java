package com.practical.leavemaster.leavetype;

public class LeaveTypeNotFoundException extends RuntimeException {
    public LeaveTypeNotFoundException(String id) {
        super("Leave type not found: " + id);
    }
}

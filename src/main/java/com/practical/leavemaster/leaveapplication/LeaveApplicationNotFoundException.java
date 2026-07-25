package com.practical.leavemaster.leaveapplication;

public class LeaveApplicationNotFoundException extends RuntimeException {
    public LeaveApplicationNotFoundException(String id) {
        super("Leave application not found: " + id);
    }
}

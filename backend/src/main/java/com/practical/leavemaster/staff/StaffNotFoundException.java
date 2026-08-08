package com.practical.leavemaster.staff;

public class StaffNotFoundException extends RuntimeException {
    public StaffNotFoundException(String id) {
        super("Staff not found: " + id);
    }
}

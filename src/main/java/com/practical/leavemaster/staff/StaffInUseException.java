package com.practical.leavemaster.staff;

public class StaffInUseException extends RuntimeException {
    public StaffInUseException(String id) {
        super("Staff is in use and cannot be deleted: " + id);
    }
}

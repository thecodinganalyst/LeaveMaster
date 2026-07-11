package com.practicallimits.spring_template.leavetype;

public class LeaveTypeInUseException extends RuntimeException {
    public LeaveTypeInUseException(String id) {
        super("Leave type is in use and cannot be deleted: " + id);
    }
}

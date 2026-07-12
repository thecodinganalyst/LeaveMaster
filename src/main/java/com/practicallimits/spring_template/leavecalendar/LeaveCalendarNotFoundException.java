package com.practicallimits.spring_template.leavecalendar;

public class LeaveCalendarNotFoundException extends RuntimeException {
    public LeaveCalendarNotFoundException(String id) {
        super("Leave calendar not found: " + id);
    }
}

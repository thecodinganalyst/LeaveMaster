package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeaveCalendarMcpTools {

    private final LeaveCalendarService leaveCalendarService;

    @Tool(description = "Get all leave calendars ordered by start date")
    public List<LeaveCalendar> getAllLeaveCalendars() {
        return leaveCalendarService.findAll();
    }

    @Tool(description = "Get a leave calendar by ID")
    public Optional<LeaveCalendar> getLeaveCalendarById(String id) {
        return leaveCalendarService.findById(id);
    }

    @Tool(description = "Create a new leave calendar")
    public LeaveCalendar createLeaveCalendar(LeaveCalendar leaveCalendar) {
        return leaveCalendarService.create(leaveCalendar);
    }
}

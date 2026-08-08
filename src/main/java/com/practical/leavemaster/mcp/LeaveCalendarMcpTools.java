package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.rbac.RbacPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeaveCalendarMcpTools {

    private final LeaveCalendarService leaveCalendarService;

    @Tool(description = "Get all leave calendars ordered by start date")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_CALENDAR_READ + "')")
    public List<LeaveCalendar> getAllLeaveCalendars() {
        return leaveCalendarService.findAll();
    }

    @Tool(description = "Get a leave calendar by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_CALENDAR_READ + "')")
    public Optional<LeaveCalendar> getLeaveCalendarById(String id) {
        return leaveCalendarService.findById(id);
    }

    @Tool(description = "Create a new leave calendar")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_CALENDAR_WRITE + "')")
    public LeaveCalendar createLeaveCalendar(LeaveCalendar leaveCalendar) {
        return leaveCalendarService.create(leaveCalendar);
    }
}

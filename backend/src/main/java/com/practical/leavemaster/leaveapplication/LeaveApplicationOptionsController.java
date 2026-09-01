package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/leave-application-options", "/api/leave-application-options"})
@RequiredArgsConstructor
public class LeaveApplicationOptionsController {

    private final LeaveTypeService leaveTypeService;

    @GetMapping("/leave-types")
    @PreAuthorize("hasAuthority('LEAVE_APPLICATION_WRITE')")
    public List<LeaveTypeOption> getLeaveTypes() {
        return leaveTypeService.findAll().stream()
                .filter(LeaveType::isActive)
                .map(leaveType -> new LeaveTypeOption(leaveType.getId(), leaveType.getName()))
                .toList();
    }

    public record LeaveTypeOption(String id, String name) {
    }
}

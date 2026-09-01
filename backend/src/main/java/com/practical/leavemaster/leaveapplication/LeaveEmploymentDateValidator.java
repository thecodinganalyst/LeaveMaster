package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffNotFoundException;
import com.practical.leavemaster.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaveEmploymentDateValidator {

    private final StaffRepository staffRepository;

    public void validate(LeaveApplicationRequest request) {
        if (request == null || request.getStaffId() == null) {
            return;
        }
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new StaffNotFoundException(request.getStaffId()));

        if (request.getFromDate() != null && request.getFromDate().isBefore(staff.getJoinDate())) {
            throw new IllegalArgumentException(
                    "Cannot apply for leave before join date " + staff.getJoinDate());
        }
        if (staff.getTermDate() != null
                && request.getToDate() != null
                && request.getToDate().isAfter(staff.getTermDate())) {
            throw new IllegalArgumentException(
                    "Cannot apply for leave after termination date " + staff.getTermDate());
        }
    }
}

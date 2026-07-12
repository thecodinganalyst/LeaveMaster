package com.practicallimits.spring_template.staff;

import com.practicallimits.spring_template.leavecalendar.LeaveCalendar;
import com.practicallimits.spring_template.leavecalendar.LeaveCalendarService;
import com.practicallimits.spring_template.leaveentitlement.LeaveEntitlement;
import com.practicallimits.spring_template.leavetype.LeaveType;
import com.practicallimits.spring_template.leavetype.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StaffService {

    private static final long INCLUSIVE_DAY_OFFSET = 1L;

    private final StaffRepository staffRepository;
    private final LeaveCalendarService leaveCalendarService;
    private final LeaveTypeRepository leaveTypeRepository;

    public List<Staff> findAll() {
        return staffRepository.findAll();
    }

    public Optional<Staff> findById(String id) {
        return staffRepository.findById(id);
    }

    public Staff save(Staff staff) {
        if (staff.getLeaveEntitlements() != null) {
            staff.setLeaveEntitlements(normalizeLeaveEntitlements(staff, staff.getLeaveEntitlements()));
        }
        return staffRepository.save(staff);
    }

    public Staff update(String id, Staff updated) {
        Staff existing = staffRepository.findById(id)
                .orElseThrow(() -> new StaffNotFoundException(id));
        existing.setName(updated.getName());
        existing.setJoinDate(updated.getJoinDate());
        if (updated.getWorkSchedule() != null) {
            existing.setWorkSchedule(new ArrayList<>(updated.getWorkSchedule()));
        }
        existing.setTermDate(updated.getTermDate());
        if (updated.getLeaveEntitlements() != null) {
            List<LeaveEntitlement> normalized = normalizeLeaveEntitlements(existing, updated.getLeaveEntitlements());
            existing.getLeaveEntitlements().clear();
            existing.getLeaveEntitlements().addAll(normalized);
        }
        return staffRepository.save(existing);
    }

    public void delete(String id) {
        staffRepository.findById(id)
                .orElseThrow(() -> new StaffNotFoundException(id));
        staffRepository.deleteById(id);
    }

    private List<LeaveEntitlement> normalizeLeaveEntitlements(Staff staff, List<LeaveEntitlement> leaveEntitlements) {
        if (leaveEntitlements == null) {
            return new ArrayList<>();
        }

        List<LeaveEntitlement> normalized = new ArrayList<>();
        for (LeaveEntitlement leaveEntitlement : leaveEntitlements) {
            if (leaveEntitlement.getLeaveType() == null || leaveEntitlement.getLeaveType().getId() == null) {
                throw new IllegalArgumentException("Leave entitlement must specify a leave type ID");
            }

            LeaveType leaveType = leaveTypeRepository.findById(leaveEntitlement.getLeaveType().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Leave type not found: " + leaveEntitlement.getLeaveType().getId()));

            leaveEntitlement.setStaff(staff);
            leaveEntitlement.setLeaveType(leaveType);
            applyPeriodAndProration(staff, leaveEntitlement);
            normalized.add(leaveEntitlement);
        }
        return normalized;
    }

    private void applyPeriodAndProration(Staff staff, LeaveEntitlement leaveEntitlement) {
        if (staff.getJoinDate() == null) {
            throw new IllegalArgumentException("Staff joinDate is required");
        }

        LocalDate from = leaveEntitlement.getFrom();
        LocalDate to = leaveEntitlement.getTo();
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("Leave entitlement from must be on or before to");
            }
            return;
        }

        if (from != null || to != null) {
            throw new IllegalArgumentException("Leave entitlement requires both from and to when setting period manually");
        }

        Optional<LeaveCalendar> leaveCalendar = leaveCalendarService.getCalendarFor(staff.getJoinDate());
        if (leaveCalendar.isEmpty()) {
            throw new IllegalArgumentException("No leave calendar found for join date: " + staff.getJoinDate());
        }

        LeaveCalendar calendar = leaveCalendar.get();
        leaveEntitlement.setFrom(calendar.getStart());
        leaveEntitlement.setTo(calendar.getEnd());
        leaveEntitlement.setEntitlement(prorateEntitlement(leaveEntitlement.getEntitlement(),
                staff.getJoinDate(), calendar.getStart(), calendar.getEnd()));
    }

    private BigDecimal prorateEntitlement(BigDecimal fullPeriodEntitlement, LocalDate joinDate, LocalDate from, LocalDate to) {
        if (fullPeriodEntitlement == null) {
            throw new IllegalArgumentException("Leave entitlement amount is required");
        }

        if (joinDate.isBefore(from) || joinDate.isEqual(from)) {
            return fullPeriodEntitlement;
        }

        long totalPeriodDays = ChronoUnit.DAYS.between(from, to) + INCLUSIVE_DAY_OFFSET;
        long remainingPeriodDays = ChronoUnit.DAYS.between(joinDate, to) + INCLUSIVE_DAY_OFFSET;
        if (remainingPeriodDays <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return fullPeriodEntitlement
                .multiply(BigDecimal.valueOf(remainingPeriodDays))
                .divide(BigDecimal.valueOf(totalPeriodDays), 2, RoundingMode.HALF_UP);
    }
}

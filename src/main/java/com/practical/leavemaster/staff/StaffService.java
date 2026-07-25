package com.practical.leavemaster.staff;

import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leaveapprover.LeaveApprover;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StaffService {

    private static final long INCLUSIVE_DAY_OFFSET = 1L;

    private final StaffRepository staffRepository;
    private final LeaveCalendarService leaveCalendarService;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveApproverRepository leaveApproverRepository;

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

    public TerminationResult terminate(String id, LocalDate termDate) {
        Staff existing = staffRepository.findById(id)
                .orElseThrow(() -> new StaffNotFoundException(id));

        if (termDate == null) {
            throw new IllegalArgumentException("Termination date is required");
        }
        if (termDate.isBefore(existing.getJoinDate())) {
            throw new IllegalArgumentException("Termination date must not be before join date");
        }

        existing.setTermDate(termDate);

        for (LeaveEntitlement entitlement : existing.getLeaveEntitlements()) {
            if (entitlement.getTo() != null && entitlement.getTo().isAfter(termDate)) {
                LocalDate effectiveFrom = (existing.getJoinDate() != null && existing.getJoinDate().isAfter(entitlement.getFrom()))
                        ? existing.getJoinDate() : entitlement.getFrom();
                long totalEffectiveDays = ChronoUnit.DAYS.between(effectiveFrom, entitlement.getTo()) + INCLUSIVE_DAY_OFFSET;
                long workedDays = ChronoUnit.DAYS.between(effectiveFrom, termDate) + INCLUSIVE_DAY_OFFSET;
                if (workedDays <= 0) {
                    entitlement.setEntitlement(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                } else if (workedDays < totalEffectiveDays) {
                    entitlement.setEntitlement(entitlement.getEntitlement()
                            .multiply(BigDecimal.valueOf(workedDays))
                            .divide(BigDecimal.valueOf(totalEffectiveDays), 2, RoundingMode.HALF_UP));
                }
                entitlement.setTo(termDate);
            }
        }

        List<LeaveApprover> approverRecords = leaveApproverRepository.findByApprover(existing);
        for (LeaveApprover record : approverRecords) {
            if (record.getEffectiveTo() == null || record.getEffectiveTo().isAfter(termDate)) {
                record.setEffectiveTo(termDate);
                leaveApproverRepository.save(record);
            }
        }

        Set<String> checkedStaffIds = new HashSet<>();
        List<Staff> staffWithNoApprover = new ArrayList<>();
        for (LeaveApprover record : approverRecords) {
            Staff staffMember = record.getStaff();
            if (!staffMember.getId().equals(id) && checkedStaffIds.add(staffMember.getId())) {
                List<LeaveApprover> remaining =
                        leaveApproverRepository.findActiveApproversForStaff(staffMember, termDate.plusDays(1));
                if (remaining.isEmpty()) {
                    staffWithNoApprover.add(staffMember);
                }
            }
        }

        Staff saved = staffRepository.save(existing);
        return new TerminationResult(saved, staffWithNoApprover);
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
        LocalDate periodEnd = (staff.getTermDate() != null && staff.getTermDate().isBefore(calendar.getEnd()))
                ? staff.getTermDate() : calendar.getEnd();
        leaveEntitlement.setFrom(calendar.getStart());
        leaveEntitlement.setTo(periodEnd);
        leaveEntitlement.setEntitlement(prorateEntitlement(leaveEntitlement.getEntitlement(),
                staff.getJoinDate(), staff.getTermDate(), calendar.getStart(), calendar.getEnd()));
    }

    private BigDecimal prorateEntitlement(BigDecimal fullPeriodEntitlement, LocalDate joinDate, LocalDate termDate, LocalDate from, LocalDate to) {
        if (fullPeriodEntitlement == null) {
            throw new IllegalArgumentException("Leave entitlement amount is required");
        }

        LocalDate effectiveFrom = (joinDate != null && joinDate.isAfter(from)) ? joinDate : from;
        LocalDate effectiveTo = (termDate != null && termDate.isBefore(to)) ? termDate : to;

        if (!effectiveFrom.isAfter(from) && effectiveTo.equals(to)) {
            return fullPeriodEntitlement;
        }

        long totalPeriodDays = ChronoUnit.DAYS.between(from, to) + INCLUSIVE_DAY_OFFSET;
        long effectiveDays = ChronoUnit.DAYS.between(effectiveFrom, effectiveTo) + INCLUSIVE_DAY_OFFSET;
        if (effectiveDays <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return fullPeriodEntitlement
                .multiply(BigDecimal.valueOf(effectiveDays))
                .divide(BigDecimal.valueOf(totalPeriodDays), 2, RoundingMode.HALF_UP);
    }
}

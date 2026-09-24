package com.practical.leavemaster.assistant;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapplication.LeaveStatus;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.staff.DaySchedule;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveInsightService {
    private final StaffRepository staffRepository;
    private final LeaveApplicationRepository applicationRepository;
    private final LeaveApproverRepository approverRepository;

    @Transactional(readOnly = true)
    public List<StaffLeaveFinding> findStaffAnomalies(String staffId, LocalDate asOf) {
        Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        LocalDate date = asOf == null ? LocalDate.now() : asOf;
        List<StaffLeaveFinding> findings = new ArrayList<>();
        if (staff.getJurisdictionId() == null || staff.getJurisdictionId().isBlank()) {
            findings.add(finding("MISSING_JURISDICTION", staff, "Staff has no jurisdiction mapping", "jurisdictionId is blank"));
        }
        if (staff.getWorkSchedule() == null || staff.getWorkSchedule().isEmpty()
                || staff.getWorkSchedule().stream().noneMatch(d -> d.getSchedule() != DaySchedule.NOT_WORKING)) {
            findings.add(finding("INVALID_WORK_SCHEDULE", staff, "Staff has no working day configured", "no work-schedule day is working"));
        }
        if (approverRepository.findActiveApproversForStaff(staff, date).isEmpty()) {
            findings.add(finding("MISSING_APPROVER", staff, "Staff has no active leave approver", "no approver is effective on " + date));
        }
        if (staff.getLeaveEntitlements() == null || staff.getLeaveEntitlements().isEmpty()) {
            findings.add(finding("MISSING_ENTITLEMENT", staff, "Staff has no leave entitlement", "leaveEntitlements is empty"));
        } else {
            staff.getLeaveEntitlements().stream().filter(e -> e.getPolicyId() == null || e.getPolicyId().isBlank())
                    .forEach(e -> findings.add(finding("MISSING_POLICY_MAPPING", staff,
                            "Leave entitlement is not mapped to a policy",
                            "leaveType=" + (e.getLeaveType() == null ? "unknown" : e.getLeaveType().getName()))));
        }
        for (LeaveApplication leave : applicationRepository.findByStaff(staff)) {
            if (staff.getJoinDate() != null && leave.getLeaveDate().isBefore(staff.getJoinDate())) {
                findings.add(finding("LEAVE_BEFORE_JOIN_DATE", staff, "Leave exists before employment start",
                        "leaveDate=" + leave.getLeaveDate() + ", joinDate=" + staff.getJoinDate()));
            }
            if (staff.getTermDate() != null && leave.getLeaveDate().isAfter(staff.getTermDate())) {
                findings.add(finding("LEAVE_AFTER_TERMINATION", staff, "Leave exists after termination",
                        "leaveDate=" + leave.getLeaveDate() + ", termDate=" + staff.getTermDate()));
            }
        }
        return List.copyOf(findings);
    }

    @Transactional(readOnly = true)
    public List<PendingAction> pendingActionsForApprover(String approverId) {
        return applicationRepository.findPendingByApproverId(approverId).stream()
                .filter(a -> a.getStatus() == LeaveStatus.PENDING)
                .map(a -> new PendingAction(a.getId(), a.getStaff().getId(), a.getStaff().getName(),
                        a.getLeaveDate(), a.getLeaveType() == null ? null : a.getLeaveType().getName(), a.getTenantId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UpcomingLeave> upcomingLeaveForApprover(String approverId, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) throw new IllegalArgumentException("A valid from/to range is required");
        Staff approver = staffRepository.findById(approverId).orElseThrow(() -> new IllegalArgumentException("Approver not found"));
        return approverRepository.findByApprover(approver).stream()
                .filter(link -> link.getStaff() != null)
                .flatMap(link -> applicationRepository.findByStaffAndLeaveDateBetween(link.getStaff(), from, to).stream())
                .filter(a -> a.getStatus() == LeaveStatus.APPROVED || a.getStatus() == LeaveStatus.PENDING)
                .filter(a -> a.getStaff() != null && java.util.Objects.equals(approver.getTenantId(), a.getStaff().getTenantId()))
                .map(a -> new UpcomingLeave(a.getStaff().getId(), a.getStaff().getName(), a.getLeaveDate(),
                        a.getStatus().name(), a.getLeaveType() == null ? null : a.getLeaveType().getName()))
                .distinct().toList();
    }

    private StaffLeaveFinding finding(String code, Staff staff, String reason, String evidence) {
        return new StaffLeaveFinding(code, staff.getId(), staff.getName(), reason, evidence, staff.getTenantId());
    }

    public record StaffLeaveFinding(String code, String staffId, String staffName, String reason, String evidence, String tenantId) {}
    public record PendingAction(String applicationId, String staffId, String staffName, LocalDate leaveDate, String leaveType, String tenantId) {}
    public record UpcomingLeave(String staffId, String staffName, LocalDate leaveDate, String status, String leaveType) {}
}

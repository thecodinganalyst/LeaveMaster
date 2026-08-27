package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.staff.DaySchedule;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.staff.WorkScheduleDay;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class StaffAssistantReadService {

    private final StaffRepository staffRepository;

    public StaffAssistantReadService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Transactional(readOnly = true)
    public List<StaffResult> findAll() {
        return staffRepository.findAll().stream()
                .map(this::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<StaffResult> findById(String id) {
        return staffRepository.findById(id).map(this::toResult);
    }

    @Transactional(readOnly = true)
    public Optional<StaffLeaveEntitlementResult> findLeaveEntitlement(String staffId, String leaveType, Integer year) {
        if (staffId == null || staffId.isBlank()) {
            throw new IllegalArgumentException("staffId is required");
        }
        if (leaveType == null || leaveType.isBlank()) {
            throw new IllegalArgumentException("leaveType is required");
        }

        int targetYear = year == null ? LocalDate.now().getYear() : year;
        LocalDate yearStart = LocalDate.of(targetYear, 1, 1);
        LocalDate yearEnd = LocalDate.of(targetYear, 12, 31);
        String requestedLeaveType = leaveType.trim();

        return staffRepository.findById(staffId.trim()).flatMap(staff -> {
            List<LeaveEntitlement> entitlements = staff.getLeaveEntitlements() == null
                    ? List.of()
                    : staff.getLeaveEntitlements();
            return entitlements.stream()
                    .filter(entitlement -> overlaps(entitlement, yearStart, yearEnd))
                    .filter(entitlement -> matchesLeaveType(entitlement, requestedLeaveType))
                    .findFirst()
                    .map(entitlement -> toStaffLeaveEntitlementResult(staff, entitlement));
        });
    }

    private boolean overlaps(LeaveEntitlement entitlement, LocalDate yearStart, LocalDate yearEnd) {
        return entitlement.getFrom() != null
                && entitlement.getTo() != null
                && !entitlement.getFrom().isAfter(yearEnd)
                && !entitlement.getTo().isBefore(yearStart);
    }

    private boolean matchesLeaveType(LeaveEntitlement entitlement, String requestedLeaveType) {
        if (entitlement.getLeaveType() == null) return false;
        String id = entitlement.getLeaveType().getId();
        String name = entitlement.getLeaveType().getName();
        return (id != null && id.equalsIgnoreCase(requestedLeaveType))
                || (name != null && name.equalsIgnoreCase(requestedLeaveType));
    }

    private StaffLeaveEntitlementResult toStaffLeaveEntitlementResult(Staff staff, LeaveEntitlement entitlement) {
        return new StaffLeaveEntitlementResult(
                staff.getName(),
                staff.getJoinDate(),
                staff.getJurisdictionId(),
                entitlement.getLeaveType() == null ? null : entitlement.getLeaveType().getName(),
                entitlement.getFrom(),
                entitlement.getTo(),
                entitlement.getEntitlement(),
                entitlement.getBaseEntitlementAmount(),
                entitlement.getCarriedForwardAmount(),
                entitlement.getAdjustmentAmount());
    }

    private StaffResult toResult(Staff staff) {
        List<WorkScheduleResult> workSchedule = staff.getWorkSchedule() == null
                ? List.of()
                : staff.getWorkSchedule().stream().map(this::toWorkScheduleResult).toList();
        List<LeaveEntitlementResult> leaveEntitlements = staff.getLeaveEntitlements() == null
                ? List.of()
                : staff.getLeaveEntitlements().stream().map(this::toLeaveEntitlementResult).toList();

        return new StaffResult(
                staff.getId(),
                staff.getName(),
                staff.getEmail(),
                staff.getJoinDate(),
                staff.getTermDate(),
                staff.getJurisdictionId(),
                staff.getTenantId(),
                workSchedule,
                leaveEntitlements);
    }

    private WorkScheduleResult toWorkScheduleResult(WorkScheduleDay day) {
        return new WorkScheduleResult(day.getDayOfWeek(), day.getDaySchedule());
    }

    private LeaveEntitlementResult toLeaveEntitlementResult(LeaveEntitlement entitlement) {
        String leaveTypeId = entitlement.getLeaveType() == null ? null : entitlement.getLeaveType().getId();
        String leaveTypeName = entitlement.getLeaveType() == null ? null : entitlement.getLeaveType().getName();
        return new LeaveEntitlementResult(
                leaveTypeId,
                leaveTypeName,
                entitlement.getFrom(),
                entitlement.getTo(),
                entitlement.getEntitlement(),
                entitlement.getPolicyId(),
                entitlement.getBaseEntitlementAmount(),
                entitlement.getCarriedForwardAmount(),
                entitlement.getAdjustmentAmount(),
                entitlement.getGeneratedAt());
    }

    public record StaffResult(
            String id,
            String name,
            String email,
            LocalDate joinDate,
            LocalDate termDate,
            String jurisdictionId,
            String tenantId,
            List<WorkScheduleResult> workSchedule,
            List<LeaveEntitlementResult> leaveEntitlements
    ) {
    }

    public record WorkScheduleResult(DayOfWeek dayOfWeek, DaySchedule daySchedule) {
    }

    public record LeaveEntitlementResult(
            String leaveTypeId,
            String leaveTypeName,
            LocalDate from,
            LocalDate to,
            BigDecimal entitlement,
            String policyId,
            BigDecimal baseEntitlementAmount,
            BigDecimal carriedForwardAmount,
            BigDecimal adjustmentAmount,
            Instant generatedAt
    ) {
    }

    public record StaffLeaveEntitlementResult(
            String staffName,
            LocalDate joinDate,
            String jurisdictionId,
            String leaveTypeName,
            LocalDate from,
            LocalDate to,
            BigDecimal entitlement,
            BigDecimal baseEntitlementAmount,
            BigDecimal carriedForwardAmount,
            BigDecimal adjustmentAmount
    ) {
    }
}

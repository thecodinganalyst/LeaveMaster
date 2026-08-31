package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveProrationRounding;
import com.practical.leavemaster.leaveentitlementpolicy.ProrationMethod;
import com.practical.leavemaster.staff.DaySchedule;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.staff.WorkScheduleDay;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class StaffAssistantReadService {

    private final StaffRepository staffRepository;
    private final LeaveEntitlementPolicyRepository policyRepository;

    public StaffAssistantReadService(
            StaffRepository staffRepository,
            LeaveEntitlementPolicyRepository policyRepository) {
        this.staffRepository = staffRepository;
        this.policyRepository = policyRepository;
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
        Optional<LeaveEntitlementPolicy> sourcePolicy = Optional.ofNullable(entitlement.getPolicyId())
                .filter(policyId -> !policyId.isBlank())
                .flatMap(policyRepository::findById);
        ProrationEvidence proration = sourcePolicy
                .map(policy -> prorationEvidence(policy, staff.getJoinDate(), entitlement.getFrom(), entitlement.getTo()))
                .orElse(ProrationEvidence.none());

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
                entitlement.getAdjustmentAmount(),
                sourcePolicy.map(LeaveEntitlementPolicy::getId).orElse(entitlement.getPolicyId()),
                sourcePolicy.map(LeaveEntitlementPolicy::getName).orElse(null),
                sourcePolicy.map(LeaveEntitlementPolicy::getEntitlementAmount).orElse(null),
                sourcePolicy.map(policy -> policy.getEntitlementUnit() == null ? null : policy.getEntitlementUnit().name()).orElse(null),
                sourcePolicy.map(policy -> policy.getAccrualMethod() == null ? null : policy.getAccrualMethod().name()).orElse(null),
                sourcePolicy.map(policy -> policy.getProrationMethod() == null ? null : policy.getProrationMethod().name()).orElse(null),
                sourcePolicy.map(LeaveEntitlementPolicy::isCarryForwardAllowed).orElse(false),
                sourcePolicy.map(LeaveEntitlementPolicy::getCarryForwardLimit).orElse(null),
                sourcePolicy.map(LeaveEntitlementPolicy::getCarryForwardExpiryMonths).orElse(null),
                proration.eligibleUnits(),
                proration.periodUnits(),
                proration.rawProratedAmount(),
                LeaveProrationRounding.HALF_DAY,
                "NEAREST_HALF_DAY",
                sourcePolicy.isPresent());
    }

    private ProrationEvidence prorationEvidence(
            LeaveEntitlementPolicy policy,
            LocalDate joinDate,
            LocalDate periodStart,
            LocalDate periodEnd) {
        if (policy.getEntitlementAmount() == null
                || policy.getProrationMethod() == null
                || policy.getProrationMethod() == ProrationMethod.NONE
                || joinDate == null
                || periodStart == null
                || periodEnd == null
                || !joinDate.isAfter(periodStart)
                || joinDate.isAfter(periodEnd)) {
            return ProrationEvidence.none();
        }

        return switch (policy.getProrationMethod()) {
            case CALENDAR_DAYS -> {
                long periodDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
                long eligibleDays = ChronoUnit.DAYS.between(joinDate, periodEnd) + 1;
                BigDecimal raw = policy.getEntitlementAmount()
                        .multiply(BigDecimal.valueOf(eligibleDays))
                        .divide(BigDecimal.valueOf(periodDays), 8, RoundingMode.HALF_UP);
                yield new ProrationEvidence(eligibleDays, periodDays, raw);
            }
            case MONTHS -> {
                long periodMonths = ChronoUnit.MONTHS.between(YearMonth.from(periodStart), YearMonth.from(periodEnd)) + 1;
                long eligibleMonths = ChronoUnit.MONTHS.between(YearMonth.from(joinDate), YearMonth.from(periodEnd)) + 1;
                BigDecimal raw = policy.getEntitlementAmount()
                        .multiply(BigDecimal.valueOf(eligibleMonths))
                        .divide(BigDecimal.valueOf(periodMonths), 8, RoundingMode.HALF_UP);
                yield new ProrationEvidence(eligibleMonths, periodMonths, raw);
            }
            case NONE -> ProrationEvidence.none();
        };
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
            BigDecimal adjustmentAmount,
            String sourcePolicyId,
            String sourcePolicyName,
            BigDecimal configuredEntitlementAmount,
            String entitlementUnit,
            String accrualMethod,
            String prorationMethod,
            boolean carryForwardAllowed,
            BigDecimal carryForwardLimit,
            Integer carryForwardExpiryMonths,
            Long prorationEligibleUnits,
            Long prorationPeriodUnits,
            BigDecimal rawProratedAmount,
            BigDecimal prorationDenominationDays,
            String prorationRoundingRule,
            boolean sourcePolicyResolved
    ) {
    }

    private record ProrationEvidence(Long eligibleUnits, Long periodUnits, BigDecimal rawProratedAmount) {
        private static ProrationEvidence none() {
            return new ProrationEvidence(null, null, null);
        }
    }
}

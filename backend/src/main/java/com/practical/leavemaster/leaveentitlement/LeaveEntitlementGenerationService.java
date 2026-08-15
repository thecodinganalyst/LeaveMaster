package com.practical.leavemaster.leaveentitlement;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapplication.LeaveDuration;
import com.practical.leavemaster.leaveapplication.LeaveStatus;
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyResolutionService;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyValidationException;
import com.practical.leavemaster.leaveentitlementpolicy.PolicyResolutionResult;
import com.practical.leavemaster.leaveentitlementpolicy.ProrationMethod;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveEntitlementGenerationService {
    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";
    private static final BigDecimal HALF_DAY = new BigDecimal("0.5");

    private final StaffRepository staffRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveEntitlementRepository entitlementRepository;
    private final LeaveEntitlementPolicyRepository policyRepository;
    private final LeaveEntitlementPolicyResolutionService resolutionService;
    private final LeaveApplicationRepository applicationRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public List<EntitlementGenerationResult> generateForStaff(String staffId, LocalDate periodStart, LocalDate periodEnd) {
        validatePeriod(periodStart, periodEnd);
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown staff id: " + staffId));
        requireTenantAccess(staff.getTenantId());
        List<EntitlementGenerationResult> results = new ArrayList<>();
        for (LeaveType leaveType : leaveTypeRepository.findAllByTenantId(staff.getTenantId())) {
            results.add(generateForLeaveType(staff, leaveType, periodStart, periodEnd));
        }
        return results;
    }

    @Transactional
    public List<EntitlementGenerationResult> generateForTenant(String tenantId, LocalDate periodStart, LocalDate periodEnd) {
        validatePeriod(periodStart, periodEnd);
        requireTenantAccess(tenantId);
        List<EntitlementGenerationResult> results = new ArrayList<>();
        for (Staff staff : staffRepository.findAllByTenantId(tenantId)) {
            for (LeaveType leaveType : leaveTypeRepository.findAllByTenantId(tenantId)) {
                results.add(generateForLeaveType(staff, leaveType, periodStart, periodEnd));
            }
        }
        return results;
    }

    private EntitlementGenerationResult generateForLeaveType(
            Staff staff, LeaveType leaveType, LocalDate periodStart, LocalDate periodEnd) {
        PolicyResolutionResult resolution = resolutionService.resolve(staff.getId(), leaveType.getId(), periodStart);
        if (resolution.ambiguous()) {
            return result(staff, leaveType, null, null, EntitlementGenerationResult.Status.AMBIGUOUS_POLICY,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "Multiple matching policies have the same highest priority");
        }
        if (resolution.selectedPolicyId() == null) {
            return result(staff, leaveType, null, null, EntitlementGenerationResult.Status.NO_MATCHING_POLICY,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "No matching entitlement policy");
        }

        LeaveEntitlementPolicy policy = policyRepository.findById(resolution.selectedPolicyId())
                .orElseThrow(() -> new IllegalStateException("Resolved policy no longer exists: " + resolution.selectedPolicyId()));
        if (policy.getEntitlementUnit() != EntitlementUnit.DAYS) {
            throw new LeaveEntitlementPolicyValidationException("Only DAYS entitlement policies can currently generate employee balances");
        }
        if (policy.getAccrualMethod() == AccrualMethod.PER_PAY_PERIOD) {
            throw new LeaveEntitlementPolicyValidationException("PER_PAY_PERIOD entitlement generation requires a payroll schedule and is not supported yet");
        }

        Optional<LeaveEntitlement> existingOpt = entitlementRepository
                .findByStaffAndLeaveTypeAndFromAndTo(staff, leaveType, periodStart, periodEnd);
        if (existingOpt.isPresent() && existingOpt.get().getPolicyId() == null) {
            LeaveEntitlement legacy = existingOpt.get();
            Usage usage = usage(staff, leaveType, periodStart, periodEnd);
            return result(staff, leaveType, legacy, null, EntitlementGenerationResult.Status.LEGACY_PROTECTED,
                    valueOrZero(legacy.getBaseEntitlementAmount()), valueOrZero(legacy.getCarriedForwardAmount()),
                    valueOrZero(legacy.getAdjustmentAmount()), usage.used(), usage.reserved(), legacy.getEntitlement(),
                    "Existing legacy entitlement has no policy source and was left unchanged");
        }
        if (existingOpt.isPresent() && periodEnd.isBefore(LocalDate.now())) {
            LeaveEntitlement historical = existingOpt.get();
            Usage usage = usage(staff, leaveType, periodStart, periodEnd);
            return result(staff, leaveType, historical, historical.getPolicyId(), EntitlementGenerationResult.Status.HISTORICAL_PROTECTED,
                    valueOrZero(historical.getBaseEntitlementAmount()), valueOrZero(historical.getCarriedForwardAmount()),
                    valueOrZero(historical.getAdjustmentAmount()), usage.used(), usage.reserved(), historical.getEntitlement(),
                    "Historical generated entitlement was left unchanged");
        }

        BigDecimal base = calculateBase(policy, staff, periodStart, periodEnd);
        BigDecimal carriedForward = calculateCarryForward(policy, staff, leaveType, periodStart);
        BigDecimal adjustment = existingOpt.map(LeaveEntitlement::getAdjustmentAmount).map(this::valueOrZero).orElse(BigDecimal.ZERO);
        BigDecimal total = base.add(carriedForward).add(adjustment).setScale(2, RoundingMode.HALF_UP);
        Usage usage = usage(staff, leaveType, periodStart, periodEnd);
        BigDecimal committed = usage.used().add(usage.reserved());
        if (total.compareTo(committed) < 0) {
            throw new LeaveEntitlementPolicyValidationException(
                    "Recalculated entitlement " + total + " cannot be lower than used/reserved leave " + committed);
        }

        LeaveEntitlement entitlement = existingOpt.orElseGet(() -> LeaveEntitlement.builder()
                .staff(staff)
                .leaveType(leaveType)
                .from(periodStart)
                .to(periodEnd)
                .tenantId(staff.getTenantId())
                .adjustmentAmount(BigDecimal.ZERO)
                .carriedForwardAmount(BigDecimal.ZERO)
                .build());
        entitlement.setPolicyId(policy.getId());
        entitlement.setBaseEntitlementAmount(base);
        entitlement.setCarriedForwardAmount(carriedForward);
        entitlement.setAdjustmentAmount(adjustment);
        entitlement.setEntitlement(total);
        entitlement.setGeneratedAt(Instant.now());
        entitlement.setTenantId(staff.getTenantId());
        LeaveEntitlement saved = entitlementRepository.save(entitlement);
        if (!staff.getLeaveEntitlements().contains(saved)) {
            staff.getLeaveEntitlements().add(saved);
        }
        return result(staff, leaveType, saved, policy.getId(),
                existingOpt.isPresent() ? EntitlementGenerationResult.Status.UPDATED : EntitlementGenerationResult.Status.CREATED,
                base, carriedForward, adjustment, usage.used(), usage.reserved(), total,
                existingOpt.isPresent() ? "Existing generated entitlement reconciled" : "Entitlement generated from policy");
    }

    private BigDecimal calculateBase(LeaveEntitlementPolicy policy, Staff staff, LocalDate periodStart, LocalDate periodEnd) {
        if (policy.getAccrualMethod() == AccrualMethod.MONTHLY && policy.getAccrualRate() != null) {
            LocalDate eligibleStart = staff.getJoinDate() != null && staff.getJoinDate().isAfter(periodStart) ? staff.getJoinDate() : periodStart;
            if (eligibleStart.isAfter(periodEnd)) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            long months = ChronoUnit.MONTHS.between(YearMonth.from(eligibleStart), YearMonth.from(periodEnd)) + 1;
            BigDecimal accrued = policy.getAccrualRate().multiply(BigDecimal.valueOf(months));
            return accrued.min(policy.getEntitlementAmount()).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal amount = policy.getEntitlementAmount();
        if (policy.getProrationMethod() == ProrationMethod.NONE || staff.getJoinDate() == null || !staff.getJoinDate().isAfter(periodStart)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        if (staff.getJoinDate().isAfter(periodEnd)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return switch (policy.getProrationMethod()) {
            case CALENDAR_DAYS -> {
                long totalDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
                long eligibleDays = ChronoUnit.DAYS.between(staff.getJoinDate(), periodEnd) + 1;
                yield amount.multiply(BigDecimal.valueOf(eligibleDays))
                        .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
            }
            case MONTHS -> {
                long totalMonths = ChronoUnit.MONTHS.between(YearMonth.from(periodStart), YearMonth.from(periodEnd)) + 1;
                long eligibleMonths = ChronoUnit.MONTHS.between(YearMonth.from(staff.getJoinDate()), YearMonth.from(periodEnd)) + 1;
                yield amount.multiply(BigDecimal.valueOf(eligibleMonths))
                        .divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);
            }
            case NONE -> amount.setScale(2, RoundingMode.HALF_UP);
        };
    }

    private BigDecimal calculateCarryForward(
            LeaveEntitlementPolicy policy, Staff staff, LeaveType leaveType, LocalDate periodStart) {
        if (!policy.isCarryForwardAllowed()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        Optional<LeaveEntitlement> previous = entitlementRepository
                .findAllByStaffAndLeaveTypeAndToBeforeOrderByToDesc(staff, leaveType, periodStart)
                .stream().findFirst();
        if (previous.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        LeaveEntitlement source = previous.get();
        if (policy.getCarryForwardExpiryMonths() != null
                && periodStart.isAfter(source.getTo().plusMonths(policy.getCarryForwardExpiryMonths()))) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        Usage usage = usage(staff, leaveType, source.getFrom(), source.getTo());
        BigDecimal available = source.getEntitlement().subtract(usage.used()).subtract(usage.reserved()).max(BigDecimal.ZERO);
        if (policy.getCarryForwardLimit() != null) {
            available = available.min(policy.getCarryForwardLimit());
        }
        return available.setScale(2, RoundingMode.HALF_UP);
    }

    private Usage usage(Staff staff, LeaveType leaveType, LocalDate from, LocalDate to) {
        List<LeaveApplication> applications = applicationRepository
                .findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(
                        staff, leaveType, from, to, List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING));
        BigDecimal used = BigDecimal.ZERO;
        BigDecimal reserved = BigDecimal.ZERO;
        for (LeaveApplication application : applications) {
            BigDecimal amount = application.getLeaveDuration() == LeaveDuration.FULL ? BigDecimal.ONE : HALF_DAY;
            if (application.getStatus() == LeaveStatus.APPROVED) {
                used = used.add(amount);
            } else if (application.getStatus() == LeaveStatus.PENDING) {
                reserved = reserved.add(amount);
            }
        }
        return new Usage(used, reserved);
    }

    private EntitlementGenerationResult result(
            Staff staff, LeaveType leaveType, LeaveEntitlement entitlement, String policyId,
            EntitlementGenerationResult.Status status, BigDecimal base, BigDecimal carry, BigDecimal adjustment,
            BigDecimal used, BigDecimal reserved, BigDecimal total, String reason) {
        return new EntitlementGenerationResult(staff.getId(), leaveType.getId(), entitlement == null ? null : entitlement.getId(),
                policyId, status, base, carry, adjustment, used, reserved, total, reason);
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) {
            throw new IllegalArgumentException("A valid entitlement period start/end is required");
        }
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void requireTenantAccess(String tenantId) {
        Optional<AppUser> user = currentUser();
        if (user.isEmpty() || isPlatformAdmin(user.get())) {
            return;
        }
        if (user.get().getTenantId() == null || !user.get().getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Tenant access denied");
        }
    }

    private Optional<AppUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return Optional.empty();
        }
        return appUserRepository.findById(authentication.getName());
    }

    private boolean isPlatformAdmin(AppUser user) {
        return user.isActive() && user.getRoles() != null && user.getRoles().stream()
                .filter(AppRole::isActive)
                .anyMatch(role -> PLATFORM_ADMIN_ROLE_ID.equalsIgnoreCase(role.getId()));
    }

    private record Usage(BigDecimal used, BigDecimal reserved) {}
}

package com.practical.leavemaster.staff;

import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyResolutionService;
import com.practical.leavemaster.leaveentitlementpolicy.PolicyResolutionResult;
import com.practical.leavemaster.leaveentitlementpolicy.ProrationMethod;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffEntitlementProposalService {
    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    private final LeaveCalendarService leaveCalendarService;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveEntitlementPolicyRepository policyRepository;
    private final LeaveEntitlementPolicyResolutionService resolutionService;
    private final StaffRepository staffRepository;
    private final AppUserRepository appUserRepository;

    public List<LeaveEntitlement> propose(StaffEntitlementProposalRequest request) {
        if (request == null || request.jurisdictionId() == null || request.jurisdictionId().isBlank()) {
            throw new IllegalArgumentException("jurisdictionId is required");
        }
        if (request.joinDate() == null) {
            throw new IllegalArgumentException("joinDate is required");
        }
        if (request.termDate() != null && request.termDate().isBefore(request.joinDate())) {
            throw new IllegalArgumentException("termDate must not be before joinDate");
        }

        String tenantId = currentTenantId();
        String jurisdictionId = request.jurisdictionId().trim();
        LeaveCalendar calendar = leaveCalendarService.getCalendarFor(jurisdictionId, request.joinDate())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No leave calendar is configured for jurisdiction " + jurisdictionId + " and join date " + request.joinDate()));

        Staff profile = previewProfile(request, tenantId, jurisdictionId);
        LocalDate periodStart = calendar.getStart();
        LocalDate periodEnd = request.termDate() != null && request.termDate().isBefore(calendar.getEnd())
                ? request.termDate() : calendar.getEnd();

        List<LeaveEntitlement> proposals = new ArrayList<>();
        for (LeaveType leaveType : leaveTypeRepository.findAllByTenantId(tenantId)) {
            PolicyResolutionResult resolution = resolutionService.resolve(profile, leaveType.getId(), periodStart);
            if (resolution.ambiguous()) {
                throw new IllegalArgumentException(
                        "Multiple matching entitlement policies have the same highest priority for leave type " + leaveType.getName());
            }
            if (resolution.selectedPolicyId() == null) {
                continue;
            }

            LeaveEntitlementPolicy policy = policyRepository.findById(resolution.selectedPolicyId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Resolved policy no longer exists: " + resolution.selectedPolicyId()));
            if (!tenantId.equals(policy.getTenantId())) {
                throw new IllegalArgumentException("Resolved entitlement policy does not belong to the current tenant");
            }
            if (policy.getEntitlementUnit() != EntitlementUnit.DAYS) {
                throw new IllegalArgumentException("Only DAYS entitlement policies can currently generate employee balances");
            }
            if (policy.getAccrualMethod() == AccrualMethod.PER_PAY_PERIOD) {
                throw new IllegalArgumentException("PER_PAY_PERIOD entitlement generation requires a payroll schedule and is not supported yet");
            }

            BigDecimal base = calculateBase(policy, profile, periodStart, periodEnd);
            proposals.add(LeaveEntitlement.builder()
                    .leaveType(leaveType)
                    .from(periodStart)
                    .to(periodEnd)
                    .entitlement(base)
                    .tenantId(tenantId)
                    .policyId(policy.getId())
                    .baseEntitlementAmount(base)
                    .carriedForwardAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .adjustmentAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return proposals;
    }

    private Staff previewProfile(StaffEntitlementProposalRequest request, String tenantId, String jurisdictionId) {
        if (request.staffId() != null && !request.staffId().isBlank()) {
            Staff existing = staffRepository.findById(request.staffId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown staff id: " + request.staffId()));
            if (!tenantId.equals(existing.getTenantId())) {
                throw new IllegalArgumentException("Staff does not belong to the current tenant");
            }
        }
        return Staff.builder()
                .id(request.staffId() == null || request.staffId().isBlank() ? "__preview__" : request.staffId())
                .name("Entitlement preview")
                .joinDate(request.joinDate())
                .termDate(request.termDate())
                .jurisdictionId(jurisdictionId)
                .tenantId(tenantId)
                .build();
    }

    private BigDecimal calculateBase(
            LeaveEntitlementPolicy policy, Staff staff, LocalDate periodStart, LocalDate periodEnd) {
        if (policy.getAccrualMethod() == AccrualMethod.MONTHLY) {
            LocalDate eligibleStart = staff.getJoinDate() != null && staff.getJoinDate().isAfter(periodStart)
                    ? staff.getJoinDate() : periodStart;
            if (eligibleStart.isAfter(periodEnd)) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            long months = ChronoUnit.MONTHS.between(YearMonth.from(eligibleStart), YearMonth.from(periodEnd)) + 1;
            BigDecimal monthlyRate = policy.getEntitlementAmount().divide(MONTHS_PER_YEAR, 8, RoundingMode.HALF_UP);
            return monthlyRate.multiply(BigDecimal.valueOf(months))
                    .min(policy.getEntitlementAmount())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal amount = policy.getEntitlementAmount();
        if (policy.getProrationMethod() == ProrationMethod.NONE
                || staff.getJoinDate() == null
                || !staff.getJoinDate().isAfter(periodStart)) {
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

    private String currentTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new IllegalArgumentException("Authenticated tenant user is required");
        }
        AppUser user = appUserRepository.findById(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        if (isPlatformAdmin(user)) {
            throw new IllegalArgumentException("Staff entitlement proposals require a tenant user");
        }
        if (user.getTenantId() == null || user.getTenantId().isBlank()) {
            throw new IllegalArgumentException("Authenticated tenant user does not have a tenant id");
        }
        return user.getTenantId();
    }

    private boolean isPlatformAdmin(AppUser user) {
        return user.isActive() && user.getRoles() != null && user.getRoles().stream()
                .filter(AppRole::isActive)
                .anyMatch(role -> PLATFORM_ADMIN_ROLE_ID.equalsIgnoreCase(role.getId()));
    }
}

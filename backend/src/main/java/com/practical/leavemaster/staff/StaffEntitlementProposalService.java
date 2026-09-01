package com.practical.leavemaster.staff;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leaveeligibility.StaffDependant;
import com.practical.leavemaster.leaveeligibility.StaffDependantWriteRequest;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyResolutionService;
import com.practical.leavemaster.leaveentitlementpolicy.LeavePolicyModel;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveProrationRounding;
import com.practical.leavemaster.leaveentitlementpolicy.PolicyPeriodResolutionResult;
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
        return analyze(request).proposals();
    }

    public StaffEntitlementProposalAnalysis analyze(StaffEntitlementProposalRequest request) {
        validateRequest(request);

        String tenantId = currentTenantId();
        String jurisdictionId = request.jurisdictionId().trim();
        LocalDate calendarDate = calendarLookupDate(request.joinDate(), LocalDate.now());
        LeaveCalendar calendar = leaveCalendarService.getCalendarFor(jurisdictionId, calendarDate)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No leave calendar is configured for jurisdiction " + jurisdictionId + " and entitlement date " + calendarDate));

        Staff profile = previewProfile(request, tenantId, jurisdictionId);
        LocalDate periodStart = calendar.getStart();
        LocalDate periodEnd = request.termDate() != null && request.termDate().isBefore(calendar.getEnd())
                ? request.termDate() : calendar.getEnd();
        LocalDate policyEvaluationDate = evaluationDate(periodStart, periodEnd, LocalDate.now());

        List<LeaveEntitlement> proposals = new ArrayList<>();
        boolean anyTemplateFound = false;
        for (LeaveType leaveType : leaveTypeRepository.findAllByTenantId(tenantId)) {
            String sourceLeaveTypeId = leaveType.getSourceJurisdictionLeaveTypeId();
            if (sourceLeaveTypeId == null || sourceLeaveTypeId.isBlank()) continue;

            ResolutionAttempt attempt = resolvePolicy(profile, leaveType, sourceLeaveTypeId, policyEvaluationDate, periodEnd);
            anyTemplateFound = anyTemplateFound || attempt.templatesFound();
            if (attempt.resolved() == null) continue;

            ResolvedProposalPolicy resolved = attempt.resolved();
            LeaveEntitlementPolicy policy = policyRepository.findById(resolved.policyId())
                    .orElseThrow(() -> new IllegalStateException("Resolved policy template no longer exists: " + resolved.policyId()));
            validateTemplatePolicyIdentity(policy, sourceLeaveTypeId);
            if (!supportsOnboardingBalance(policy)) continue;

            LocalDate entitlementStart = laterOf(periodStart, profile.getJoinDate());
            if (resolved.futureEligibility()) {
                entitlementStart = laterOf(entitlementStart, resolved.eligibleFrom());
            }
            BigDecimal base = calculateBase(policy, entitlementStart, periodStart, periodEnd);
            proposals.add(LeaveEntitlement.builder()
                    .leaveType(leaveType)
                    .from(entitlementStart)
                    .to(periodEnd)
                    .entitlement(base)
                    .tenantId(tenantId)
                    .policyId(policy.getId())
                    .baseEntitlementAmount(base)
                    .carriedForwardAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .adjustmentAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        StaffEntitlementProposalAnalysis.Status status = !proposals.isEmpty()
                ? StaffEntitlementProposalAnalysis.Status.AVAILABLE
                : anyTemplateFound
                        ? StaffEntitlementProposalAnalysis.Status.NOT_ELIGIBLE_IN_PERIOD
                        : StaffEntitlementProposalAnalysis.Status.NO_TEMPLATE;
        return new StaffEntitlementProposalAnalysis(List.copyOf(proposals), status);
    }

    private void validateRequest(StaffEntitlementProposalRequest request) {
        if (request == null || request.jurisdictionId() == null || request.jurisdictionId().isBlank()) {
            throw new IllegalArgumentException("jurisdictionId is required");
        }
        if (request.joinDate() == null) throw new IllegalArgumentException("joinDate is required");
        if (request.termDate() != null && request.termDate().isBefore(request.joinDate())) {
            throw new IllegalArgumentException("termDate must not be before joinDate");
        }
    }

    private ResolutionAttempt resolvePolicy(
            Staff profile, LeaveType leaveType, String sourceLeaveTypeId,
            LocalDate evaluationDate, LocalDate periodEnd) {
        PolicyResolutionResult current = resolutionService.resolveTemplate(profile, sourceLeaveTypeId, evaluationDate);
        rejectAmbiguous(current, leaveType);
        boolean templatesFound = !current.consideredPolicies().isEmpty();
        if (current.selectedPolicyId() != null) {
            return new ResolutionAttempt(new ResolvedProposalPolicy(current.selectedPolicyId(), evaluationDate, false), true);
        }
        if (!evaluationDate.isBefore(periodEnd)) return new ResolutionAttempt(null, templatesFound);

        PolicyPeriodResolutionResult future = resolutionService.resolveTemplateInPeriod(
                profile, sourceLeaveTypeId, evaluationDate.plusDays(1), periodEnd);
        rejectAmbiguous(future.resolution(), leaveType);
        templatesFound = templatesFound || future.templatesFound();
        if (future.resolution().selectedPolicyId() == null || future.matchedDate() == null) {
            return new ResolutionAttempt(null, templatesFound);
        }
        return new ResolutionAttempt(
                new ResolvedProposalPolicy(future.resolution().selectedPolicyId(), future.matchedDate(), true), true);
    }

    private void rejectAmbiguous(PolicyResolutionResult resolution, LeaveType leaveType) {
        if (resolution.ambiguous()) {
            throw new IllegalArgumentException(
                    "Multiple matching entitlement policy templates have the same highest priority for leave type " + leaveType.getName());
        }
    }

    private void validateTemplatePolicyIdentity(LeaveEntitlementPolicy policy, String sourceLeaveTypeId) {
        if (policy.getTenantId() != null) {
            throw new IllegalArgumentException("Resolved entitlement policy must be a platform template without a tenant id");
        }
        if (policy.getScope() != ConfigurationScope.PLATFORM_TEMPLATE) {
            throw new IllegalArgumentException("Resolved entitlement policy must have PLATFORM_TEMPLATE scope");
        }
        if (!sourceLeaveTypeId.equals(policy.getJurisdictionLeaveTypeId())) {
            throw new IllegalArgumentException("Resolved entitlement policy template does not match the tenant leave type source");
        }
    }

    private boolean supportsOnboardingBalance(LeaveEntitlementPolicy policy) {
        boolean annualBalanceModel = policy.getPolicyModel() == LeavePolicyModel.ANNUAL_ENTITLEMENT
                || policy.getPolicyModel() == LeavePolicyModel.CONDITIONAL_ANNUAL_ENTITLEMENT;
        return annualBalanceModel
                && policy.getEntitlementUnit() == EntitlementUnit.DAYS
                && policy.getAccrualMethod() != AccrualMethod.PER_PAY_PERIOD;
    }

    private Staff previewProfile(StaffEntitlementProposalRequest request, String tenantId, String jurisdictionId) {
        if (request.staffId() != null && !request.staffId().isBlank()) {
            Staff existing = staffRepository.findById(request.staffId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown staff id: " + request.staffId()));
            if (!tenantId.equals(existing.getTenantId())) {
                throw new IllegalArgumentException("Staff does not belong to the current tenant");
            }
        }
        String previewId = request.staffId() == null || request.staffId().isBlank() ? "__preview__" : request.staffId();
        List<StaffDependant> previewDependants = request.dependants() == null ? null : request.dependants().stream()
                .map(input -> toPreviewDependant(input, previewId, tenantId))
                .toList();
        return Staff.builder()
                .id(previewId)
                .name("Entitlement preview")
                .joinDate(request.joinDate())
                .termDate(request.termDate())
                .jurisdictionId(jurisdictionId)
                .employmentType(request.employmentType())
                .previewDependants(previewDependants)
                .tenantId(tenantId)
                .build();
    }

    private StaffDependant toPreviewDependant(StaffDependantWriteRequest input, String staffId, String tenantId) {
        return StaffDependant.builder()
                .tenantId(tenantId)
                .staffId(staffId)
                .name(input.name())
                .relationshipCode(input.relationshipCode())
                .dateOfBirth(input.dateOfBirth())
                .citizenshipCode(input.citizenshipCode())
                .residencyCode(input.residencyCode())
                .adoptionDate(input.adoptionDate())
                .effectiveFrom(input.effectiveFrom())
                .effectiveTo(input.effectiveTo())
                .active(input.active() == null || input.active())
                .build();
    }

    private BigDecimal calculateBase(
            LeaveEntitlementPolicy policy, LocalDate eligibleStart,
            LocalDate periodStart, LocalDate periodEnd) {
        if (eligibleStart == null) eligibleStart = periodStart;
        if (eligibleStart.isAfter(periodEnd)) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (policy.getAccrualMethod() == AccrualMethod.MONTHLY) {
            long months = ChronoUnit.MONTHS.between(YearMonth.from(eligibleStart), YearMonth.from(periodEnd)) + 1;
            BigDecimal monthlyRate = policy.getEntitlementAmount().divide(MONTHS_PER_YEAR, 8, RoundingMode.HALF_UP);
            return monthlyRate.multiply(BigDecimal.valueOf(months))
                    .min(policy.getEntitlementAmount())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal amount = policy.getEntitlementAmount();
        if (policy.getProrationMethod() == ProrationMethod.NONE || !eligibleStart.isAfter(periodStart)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal rawProratedAmount = switch (policy.getProrationMethod()) {
            case CALENDAR_DAYS -> {
                long totalDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
                long eligibleDays = ChronoUnit.DAYS.between(eligibleStart, periodEnd) + 1;
                yield amount.multiply(BigDecimal.valueOf(eligibleDays))
                        .divide(BigDecimal.valueOf(totalDays), 8, RoundingMode.HALF_UP);
            }
            case MONTHS -> {
                long totalMonths = ChronoUnit.MONTHS.between(YearMonth.from(periodStart), YearMonth.from(periodEnd)) + 1;
                long eligibleMonths = ChronoUnit.MONTHS.between(YearMonth.from(eligibleStart), YearMonth.from(periodEnd)) + 1;
                yield amount.multiply(BigDecimal.valueOf(eligibleMonths))
                        .divide(BigDecimal.valueOf(totalMonths), 8, RoundingMode.HALF_UP);
            }
            case NONE -> amount;
        };
        return LeaveProrationRounding.toNearestHalfDay(rawProratedAmount);
    }

    private LocalDate laterOf(LocalDate first, LocalDate second) {
        if (second == null || first.isAfter(second)) return first;
        return second;
    }

    static LocalDate calendarLookupDate(LocalDate joinDate, LocalDate today) {
        return joinDate.getYear() < today.getYear() ? today : joinDate;
    }

    static LocalDate evaluationDate(LocalDate periodStart, LocalDate periodEnd, LocalDate today) {
        if (today.isBefore(periodStart)) return periodStart;
        if (today.isAfter(periodEnd)) return periodEnd;
        return today;
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

    private record ResolvedProposalPolicy(String policyId, LocalDate eligibleFrom, boolean futureEligibility) {}
    private record ResolutionAttempt(ResolvedProposalPolicy resolved, boolean templatesFound) {}
}

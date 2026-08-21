package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.Jurisdiction;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeaveEntitlementPolicyResolutionService {
    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";

    private final StaffRepository staffRepository;
    private final LeaveEntitlementPolicyRepository policyRepository;
    private final LeaveEntitlementPolicyEligibilityRepository ruleRepository;
    private final JurisdictionRepository jurisdictionRepository;
    private final AppUserRepository appUserRepository;

    public PolicyResolutionResult resolve(String staffId, String leaveTypeId, LocalDate effectiveDate) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown staff id: " + staffId));
        return resolve(staff, leaveTypeId, effectiveDate);
    }

    public PolicyResolutionResult resolve(Staff staff, String leaveTypeId, LocalDate effectiveDate) {
        validateStaff(staff);
        assertTenantAccess(staff.getTenantId());
        LocalDate date = effectiveDate == null ? LocalDate.now() : effectiveDate;
        List<LeaveEntitlementPolicy> policies = policyRepository
                .findAllByTenantIdAndLeaveTypeIdAndActiveTrue(staff.getTenantId(), leaveTypeId);
        return evaluatePolicies(staff, leaveTypeId, date, policies);
    }

    /**
     * Resolves platform template policies for a preview staff profile. Template policies are
     * intentionally tenantless and are selected from the staff jurisdiction, walking up the
     * jurisdiction hierarchy so inherited templates continue to work.
     */
    public PolicyResolutionResult resolveTemplate(
            Staff staff, String jurisdictionLeaveTypeId, LocalDate effectiveDate) {
        validateStaff(staff);
        if (jurisdictionLeaveTypeId == null || jurisdictionLeaveTypeId.isBlank()) {
            throw new IllegalArgumentException("jurisdictionLeaveTypeId is required");
        }
        assertTenantAccess(staff.getTenantId());
        LocalDate date = effectiveDate == null ? LocalDate.now() : effectiveDate;
        return evaluatePolicies(
                staff,
                jurisdictionLeaveTypeId,
                date,
                effectiveTemplatePolicies(staff.getJurisdictionId(), jurisdictionLeaveTypeId));
    }

    private PolicyResolutionResult evaluatePolicies(
            Staff staff,
            String leaveTypeId,
            LocalDate date,
            List<LeaveEntitlementPolicy> policies) {
        List<PolicyResolutionResult.PolicyEvaluation> evaluations = new ArrayList<>();
        List<LeaveEntitlementPolicy> matching = new ArrayList<>();
        for (LeaveEntitlementPolicy policy : policies) {
            boolean effective = !date.isBefore(policy.getEffectiveFrom())
                    && (policy.getEffectiveTo() == null || !date.isAfter(policy.getEffectiveTo()));
            List<PolicyResolutionResult.RuleEvaluation> ruleResults = new ArrayList<>();
            boolean matched = effective;
            if (effective) {
                for (LeaveEntitlementPolicyEligibilityRule rule : ruleRepository
                        .findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc(policy.getId())) {
                    PolicyResolutionResult.RuleEvaluation result = evaluateRule(rule, staff, date);
                    ruleResults.add(result);
                    if (!result.matched()) {
                        matched = false;
                    }
                }
            }
            if (matched) {
                matching.add(policy);
            }
            evaluations.add(new PolicyResolutionResult.PolicyEvaluation(
                    policy.getId(), policy.getName(), policy.getPriority(), effective, matched, ruleResults,
                    !effective ? "Policy is outside its effective date range" : matched ? "All active rules matched" : "One or more active rules did not match"));
        }

        matching.sort(Comparator.comparingInt(LeaveEntitlementPolicy::getPriority).reversed()
                .thenComparing(LeaveEntitlementPolicy::getId));
        String staffId = staff.getId();
        if (matching.isEmpty()) {
            return new PolicyResolutionResult(staffId, leaveTypeId, null, false,
                    "No matching policy", evaluations);
        }
        LeaveEntitlementPolicy winner = matching.getFirst();
        if (matching.size() > 1 && matching.get(1).getPriority() == winner.getPriority()) {
            return new PolicyResolutionResult(staffId, leaveTypeId, null, true,
                    "Multiple matching policies have the same highest priority", evaluations);
        }
        return new PolicyResolutionResult(staffId, leaveTypeId, winner.getId(), false,
                "Highest-priority matching policy selected", evaluations);
    }

    private List<LeaveEntitlementPolicy> effectiveTemplatePolicies(
            String jurisdictionId, String jurisdictionLeaveTypeId) {
        if (jurisdictionId == null || jurisdictionId.isBlank()) {
            return List.of();
        }
        Map<String, LeaveEntitlementPolicy> effective = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();
        String currentId = jurisdictionId;
        while (currentId != null && !currentId.isBlank() && visited.add(currentId)) {
            for (LeaveEntitlementPolicy policy : policyRepository
                    .findAllByScopeAndJurisdictionIdAndTenantIdIsNullAndActiveTrue(
                            ConfigurationScope.PLATFORM_TEMPLATE, currentId)) {
                if (jurisdictionLeaveTypeId.equals(policy.getJurisdictionLeaveTypeId())) {
                    effective.putIfAbsent(policy.getName(), policy);
                }
            }
            String lookupId = currentId;
            Jurisdiction jurisdiction = jurisdictionRepository.findById(lookupId)
                    .orElseThrow(() -> new IllegalArgumentException("Jurisdiction not found: " + lookupId));
            currentId = jurisdiction.getParentId();
        }
        return List.copyOf(effective.values());
    }

    private void validateStaff(Staff staff) {
        if (staff == null) {
            throw new IllegalArgumentException("staff is required");
        }
        if (staff.getTenantId() == null || staff.getTenantId().isBlank()) {
            throw new IllegalStateException("Staff does not have a tenant id");
        }
    }

    private PolicyResolutionResult.RuleEvaluation evaluateRule(
            LeaveEntitlementPolicyEligibilityRule rule, Staff staff, LocalDate date) {
        boolean matched = switch (rule.getCriterionType()) {
            case JURISDICTION_CODE -> evaluateStringSet(jurisdictionCodes(staff.getJurisdictionId()), rule);
            case SERVICE_MONTHS -> evaluateNumber(serviceMonths(staff, date), rule);
        };
        return new PolicyResolutionResult.RuleEvaluation(rule.getId(), rule.getCriterionType(), rule.getOperator(), matched,
                matched ? "Criterion matched" : "Criterion did not match");
    }

    private boolean evaluateStringSet(Set<String> actualValues, LeaveEntitlementPolicyEligibilityRule rule) {
        Set<String> expected = new HashSet<>(LeaveEntitlementPolicyEligibilityService.values(rule.getValue()));
        return switch (rule.getOperator()) {
            case EQUALS -> expected.size() == 1 && actualValues.contains(expected.iterator().next());
            case NOT_EQUALS -> expected.size() == 1 && !actualValues.contains(expected.iterator().next());
            case IN -> actualValues.stream().anyMatch(expected::contains);
            case NOT_IN -> actualValues.stream().noneMatch(expected::contains);
            default -> false;
        };
    }

    private boolean evaluateNumber(long actual, LeaveEntitlementPolicyEligibilityRule rule) {
        List<Long> expected = LeaveEntitlementPolicyEligibilityService.values(rule.getValue()).stream()
                .map(Long::parseLong)
                .toList();
        long first = expected.getFirst();
        return switch (rule.getOperator()) {
            case EQUALS -> actual == first;
            case NOT_EQUALS -> actual != first;
            case IN -> expected.contains(actual);
            case NOT_IN -> !expected.contains(actual);
            case GREATER_THAN -> actual > first;
            case GREATER_THAN_OR_EQUAL -> actual >= first;
            case LESS_THAN -> actual < first;
            case LESS_THAN_OR_EQUAL -> actual <= first;
        };
    }

    private long serviceMonths(Staff staff, LocalDate date) {
        if (staff.getJoinDate() == null || date.isBefore(staff.getJoinDate())) {
            return 0;
        }
        return ChronoUnit.MONTHS.between(staff.getJoinDate(), date);
    }

    private Set<String> jurisdictionCodes(String jurisdictionId) {
        if (jurisdictionId == null || jurisdictionId.isBlank()) {
            return Set.of();
        }
        Set<String> codes = new HashSet<>();
        Set<String> visited = new HashSet<>();
        String currentId = jurisdictionId;
        while (currentId != null && !currentId.isBlank() && visited.add(currentId)) {
            Optional<Jurisdiction> current = jurisdictionRepository.findById(currentId);
            if (current.isEmpty()) {
                break;
            }
            Jurisdiction jurisdiction = current.get();
            if (jurisdiction.isActive()) {
                codes.add(jurisdiction.getCode());
            }
            currentId = jurisdiction.getParentId();
        }
        return codes;
    }

    private void assertTenantAccess(String tenantId) {
        Optional<AppUser> user = currentUser();
        if (user.isEmpty() || isPlatformAdmin(user.get())) {
            return;
        }
        if (!tenantId.equals(user.get().getTenantId())) {
            throw new org.springframework.security.access.AccessDeniedException("Cannot resolve policies for another tenant");
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
        return user != null && user.isActive() && user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role != null && role.isActive() && PLATFORM_ADMIN_ROLE_ID.equalsIgnoreCase(role.getId()));
    }
}

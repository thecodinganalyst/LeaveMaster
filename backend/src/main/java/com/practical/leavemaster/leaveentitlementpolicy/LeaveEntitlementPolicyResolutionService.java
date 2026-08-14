package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.jurisdiction.Jurisdiction;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.location.Location;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeaveEntitlementPolicyResolutionService {
    private final StaffRepository staffRepository;
    private final LeaveEntitlementPolicyRepository policyRepository;
    private final LeaveEntitlementPolicyEligibilityRepository ruleRepository;
    private final JurisdictionRepository jurisdictionRepository;

    public PolicyResolutionResult resolve(String staffId, String leaveTypeId, LocalDate effectiveDate) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown staff id: " + staffId));
        if (staff.getTenantId() == null || staff.getTenantId().isBlank()) {
            throw new IllegalStateException("Staff does not have a tenant id");
        }
        LocalDate date = effectiveDate == null ? LocalDate.now() : effectiveDate;
        List<LeaveEntitlementPolicy> policies = policyRepository
                .findAllByTenantIdAndLeaveTypeIdAndActiveTrue(staff.getTenantId(), leaveTypeId);

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

    private PolicyResolutionResult.RuleEvaluation evaluateRule(
            LeaveEntitlementPolicyEligibilityRule rule, Staff staff, LocalDate date) {
        boolean matched = switch (rule.getCriterionType()) {
            case LOCATION_ID -> evaluateStringSet(staff.getLocation() == null ? Set.of() : Set.of(staff.getLocation().getId()), rule);
            case JURISDICTION_CODE -> evaluateStringSet(jurisdictionCodes(staff.getLocation()), rule);
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

    private Set<String> jurisdictionCodes(Location location) {
        if (location == null) {
            return Set.of();
        }
        String country = normalized(location.getCountry());
        String state = normalized(location.getState());
        Set<String> matches = new HashSet<>();
        for (Jurisdiction jurisdiction : jurisdictionRepository.findAll()) {
            if (!jurisdiction.isActive()) {
                continue;
            }
            boolean countryMatches = country.equals(normalized(jurisdiction.getCountryCode()))
                    || country.equals(normalized(jurisdiction.getName()));
            if (!countryMatches) {
                continue;
            }
            if (jurisdiction.getSubdivisionCode() == null || jurisdiction.getSubdivisionCode().isBlank()) {
                matches.add(jurisdiction.getCode());
            } else if (state.equals(normalized(jurisdiction.getSubdivisionCode()))
                    || state.equals(normalized(jurisdiction.getName()))) {
                matches.add(jurisdiction.getCode());
            }
        }
        return matches;
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}

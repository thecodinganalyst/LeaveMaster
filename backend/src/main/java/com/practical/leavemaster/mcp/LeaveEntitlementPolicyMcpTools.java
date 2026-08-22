package com.practical.leavemaster.mcp;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeRepository;
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.EligibilityCriterionType;
import com.practical.leavemaster.leaveentitlementpolicy.EligibilityOperator;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRule;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityService;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyService;
import com.practical.leavemaster.leaveentitlementpolicy.ProrationMethod;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.rbac.RbacPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LeaveEntitlementPolicyMcpTools {

    private final LeaveEntitlementPolicyService policyService;
    private final LeaveEntitlementPolicyEligibilityService eligibilityService;
    private final LeaveEntitlementPolicyRepository policyRepository;
    private final JurisdictionLeaveTypeRepository jurisdictionLeaveTypeRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    @Tool(description = "Get the raw leave entitlement policy configuration for a jurisdiction. Use only when the user explicitly asks for policy IDs, technical configuration, or raw details.")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ + "')")
    public List<LeaveEntitlementPolicy> getEntitlementPoliciesByJurisdiction(String jurisdictionId) {
        String jurisdiction = requireJurisdiction(jurisdictionId);
        Set<String> templateIds = templateIdsForJurisdiction(jurisdiction);
        return policyService.findAll().stream()
                .filter(policy -> belongsToJurisdiction(policy, jurisdiction, templateIds))
                .toList();
    }

    @Tool(description = "Get the raw eligibility rules for an accessible leave entitlement policy by policy ID. Use for explicit technical/detail requests.")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ + "')")
    public List<LeaveEntitlementPolicyEligibilityRule> getEligibilityRulesByEntitlementPolicyId(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        return eligibilityService.findAll(policyId.trim());
    }

    @Tool(description = "Get a concise, human-readable summary of leave entitlement policies and eligibility for a jurisdiction in one call. Prefer this for normal user questions. Use each servicePeriod label exactly as returned; it is derived deterministically from the configured operators. Common accrual, proration and carry-forward settings are returned once per leave type.")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ + "')")
    public List<LeaveTypeEntitlementSummary> getLeaveEntitlementConfigurationByJurisdiction(String jurisdictionId) {
        Map<String, List<PolicyDetails>> grouped = new LinkedHashMap<>();
        for (LeaveEntitlementPolicy policy : getEntitlementPoliciesByJurisdiction(jurisdictionId)) {
            String leaveType = leaveTypeName(policy);
            List<LeaveEntitlementPolicyEligibilityRule> rules = eligibilityService.findAll(policy.getId());
            grouped.computeIfAbsent(leaveType, ignored -> new ArrayList<>())
                    .add(toPolicyDetails(policy, rules));
        }

        return grouped.entrySet().stream()
                .map(entry -> toLeaveTypeSummary(entry.getKey(), entry.getValue()))
                .toList();
    }

    private LeaveTypeEntitlementSummary toLeaveTypeSummary(String leaveType, List<PolicyDetails> details) {
        String commonAccrual = commonValue(details.stream().map(PolicyDetails::accrual).toList());
        String commonProration = commonValue(details.stream().map(PolicyDetails::proration).toList());
        String commonCarryForward = commonValue(details.stream().map(PolicyDetails::carryForward).toList());

        List<EntitlementPolicySummary> policies = details.stream()
                .sorted(Comparator.comparingInt(this::serviceRangeSortKey))
                .map(detail -> new EntitlementPolicySummary(
                        detail.policyName(),
                        detail.servicePeriod(),
                        detail.eligibility(),
                        detail.entitlement(),
                        Objects.equals(detail.accrual(), commonAccrual) ? null : detail.accrual(),
                        Objects.equals(detail.proration(), commonProration) ? null : detail.proration(),
                        Objects.equals(detail.carryForward(), commonCarryForward) ? null : detail.carryForward()))
                .toList();

        return new LeaveTypeEntitlementSummary(
                leaveType,
                commonAccrual,
                commonProration,
                commonCarryForward,
                policies);
    }

    private int serviceRangeSortKey(PolicyDetails details) {
        return details.serviceRange().lowerBound().orElse(Integer.MIN_VALUE);
    }

    private String commonValue(List<String> values) {
        if (values.isEmpty()) return null;
        String first = values.getFirst();
        return values.stream().allMatch(value -> Objects.equals(first, value)) ? first : null;
    }

    private PolicyDetails toPolicyDetails(LeaveEntitlementPolicy policy,
                                          List<LeaveEntitlementPolicyEligibilityRule> rules) {
        List<LeaveEntitlementPolicyEligibilityRule> activeRules = rules.stream()
                .filter(LeaveEntitlementPolicyEligibilityRule::isActive)
                .toList();
        ServiceRange serviceRange = serviceRange(activeRules);
        String eligibility = activeRules.stream()
                .filter(rule -> rule.getCriterionType() != EligibilityCriterionType.SERVICE_MONTHS)
                .map(this::eligibilityDescription)
                .collect(Collectors.joining("; "));
        if (eligibility.isBlank()) {
            eligibility = null;
        }
        return new PolicyDetails(
                policy.getName(),
                serviceRange.label(),
                serviceRange,
                eligibility,
                entitlementDescription(policy.getEntitlementAmount(), policy.getEntitlementUnit()),
                accrualDescription(policy.getAccrualMethod()),
                prorationDescription(policy.getProrationMethod()),
                carryForwardDescription(policy));
    }

    private ServiceRange serviceRange(List<LeaveEntitlementPolicyEligibilityRule> rules) {
        Integer lower = null;
        Integer upper = null;
        boolean hasServiceRule = false;

        for (LeaveEntitlementPolicyEligibilityRule rule : rules) {
            if (rule.getCriterionType() != EligibilityCriterionType.SERVICE_MONTHS) continue;
            Integer value = parseInteger(rule.getValue());
            if (value == null) continue;
            hasServiceRule = true;
            switch (rule.getOperator()) {
                case EQUALS -> {
                    lower = strongerLower(lower, value);
                    upper = strongerUpper(upper, value);
                }
                case GREATER_THAN -> lower = strongerLower(lower, value + 1);
                case GREATER_THAN_OR_EQUAL -> lower = strongerLower(lower, value);
                case LESS_THAN -> upper = strongerUpper(upper, value - 1);
                case LESS_THAN_OR_EQUAL -> upper = strongerUpper(upper, value);
                case IN, NOT_EQUALS, NOT_IN -> {
                    // These operators do not form a safe contiguous display range. Keep descriptive eligibility instead.
                }
            }
        }

        if (!hasServiceRule || (lower == null && upper == null)) {
            return new ServiceRange(null, null, "All service periods");
        }
        if (lower != null && upper != null && Objects.equals(lower, upper)) {
            return new ServiceRange(lower, upper, lower + " months");
        }
        if (lower != null && upper != null) {
            return new ServiceRange(lower, upper, lower + "–" + upper + " months");
        }
        if (lower != null) {
            return new ServiceRange(lower, null, lower + "+ months");
        }
        return new ServiceRange(null, upper, "Up to " + upper + " months");
    }

    private Integer strongerLower(Integer current, int candidate) {
        return current == null ? candidate : Math.max(current, candidate);
    }

    private Integer strongerUpper(Integer current, int candidate) {
        return current == null ? candidate : Math.min(current, candidate);
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String leaveTypeName(LeaveEntitlementPolicy policy) {
        if (policy.getScope() == ConfigurationScope.PLATFORM_TEMPLATE && policy.getJurisdictionLeaveTypeId() != null) {
            return jurisdictionLeaveTypeRepository.findById(policy.getJurisdictionLeaveTypeId())
                    .map(type -> type.getName())
                    .orElse(policy.getName());
        }
        if (policy.getLeaveTypeId() != null) {
            return leaveTypeRepository.findById(policy.getLeaveTypeId())
                    .map(type -> type.getName())
                    .orElse(policy.getName());
        }
        return policy.getName();
    }

    private String eligibilityDescription(LeaveEntitlementPolicyEligibilityRule rule) {
        if (rule.getCriterionType() == EligibilityCriterionType.SERVICE_MONTHS) {
            return serviceMonthsDescription(rule.getOperator(), rule.getValue());
        }
        if (rule.getCriterionType() == EligibilityCriterionType.JURISDICTION_CODE) {
            return setDescription("Jurisdiction", rule.getOperator(), rule.getValue());
        }
        return "Eligibility condition: " + rule.getValue();
    }

    private String serviceMonthsDescription(EligibilityOperator operator, String value) {
        String months = value == null ? "" : value.trim();
        return switch (operator) {
            case EQUALS -> months + " months of service";
            case NOT_EQUALS -> "Service period other than " + months + " months";
            case GREATER_THAN -> "More than " + months + " months of service";
            case GREATER_THAN_OR_EQUAL -> "At least " + months + " months of service";
            case LESS_THAN -> "Less than " + months + " months of service";
            case LESS_THAN_OR_EQUAL -> "Up to " + months + " months of service";
            case IN -> "Service months are one of: " + months;
            case NOT_IN -> "Service months are not one of: " + months;
        };
    }

    private String setDescription(String label, EligibilityOperator operator, String value) {
        String values = value == null ? "" : value.trim();
        return switch (operator) {
            case EQUALS -> label + " is " + values;
            case NOT_EQUALS -> label + " is not " + values;
            case IN -> label + " is one of: " + values;
            case NOT_IN -> label + " is not one of: " + values;
            default -> label + " condition: " + values;
        };
    }

    private String entitlementDescription(BigDecimal amount, EntitlementUnit unit) {
        if (amount == null) return "Not configured";
        String value = amount.stripTrailingZeros().toPlainString();
        if (unit == null) return value;
        String label = unit.name().toLowerCase().replace('_', ' ');
        if (BigDecimal.ONE.compareTo(amount.stripTrailingZeros()) == 0 && label.endsWith("s")) {
            label = label.substring(0, label.length() - 1);
        }
        return value + " " + label;
    }

    private String accrualDescription(AccrualMethod method) {
        if (method == null) return "Accrual not configured";
        return switch (method) {
            case NONE -> "Granted upfront";
            case ANNUAL -> "Granted annually";
            case MONTHLY -> "Accrued monthly";
            case PER_PAY_PERIOD -> "Accrued each pay period";
        };
    }

    private String prorationDescription(ProrationMethod method) {
        if (method == null) return "Proration not configured";
        return switch (method) {
            case NONE -> "Not prorated";
            case CALENDAR_DAYS -> "Prorated by calendar days";
            case MONTHS -> "Prorated by completed months";
        };
    }

    private String carryForwardDescription(LeaveEntitlementPolicy policy) {
        if (!policy.isCarryForwardAllowed()) return "Unused leave cannot be carried forward";
        StringBuilder description = new StringBuilder("Carry forward allowed");
        if (policy.getCarryForwardLimit() != null) {
            description.append(" up to ")
                    .append(policy.getCarryForwardLimit().stripTrailingZeros().toPlainString())
                    .append(' ')
                    .append(policy.getEntitlementUnit() == null ? "units" : policy.getEntitlementUnit().name().toLowerCase().replace('_', ' '));
        }
        if (policy.getCarryForwardExpiryMonths() != null) {
            description.append("; expires after ").append(policy.getCarryForwardExpiryMonths()).append(" months");
        }
        return description.toString();
    }

    private Set<String> templateIdsForJurisdiction(String jurisdictionId) {
        return policyRepository.findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE).stream()
                .filter(template -> Objects.equals(jurisdictionId, template.getJurisdictionId()))
                .map(LeaveEntitlementPolicy::getId)
                .collect(Collectors.toSet());
    }

    private boolean belongsToJurisdiction(LeaveEntitlementPolicy policy, String jurisdictionId, Set<String> templateIds) {
        if (policy.getScope() == ConfigurationScope.PLATFORM_TEMPLATE) {
            return Objects.equals(jurisdictionId, policy.getJurisdictionId());
        }
        return policy.getScope() == ConfigurationScope.TENANT
                && policy.getSourceTemplateId() != null
                && templateIds.contains(policy.getSourceTemplateId());
    }

    private String requireJurisdiction(String jurisdictionId) {
        if (jurisdictionId == null || jurisdictionId.isBlank()) {
            throw new IllegalArgumentException("jurisdictionId is required");
        }
        return jurisdictionId.trim();
    }

    private record PolicyDetails(
            String policyName,
            String servicePeriod,
            ServiceRange serviceRange,
            String eligibility,
            String entitlement,
            String accrual,
            String proration,
            String carryForward
    ) {
    }

    private record ServiceRange(Integer lower, Integer upper, String label) {
        OptionalInt lowerBound() {
            return lower == null ? OptionalInt.empty() : OptionalInt.of(lower);
        }
    }

    public record LeaveTypeEntitlementSummary(
            String leaveType,
            String accrual,
            String proration,
            String carryForward,
            List<EntitlementPolicySummary> policies
    ) {
    }

    public record EntitlementPolicySummary(
            String policyName,
            String servicePeriod,
            String eligibility,
            String entitlement,
            String accrual,
            String proration,
            String carryForward
    ) {
    }
}

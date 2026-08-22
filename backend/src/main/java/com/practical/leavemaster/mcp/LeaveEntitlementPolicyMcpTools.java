package com.practical.leavemaster.mcp;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRule;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityService;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyService;
import com.practical.leavemaster.rbac.RbacPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LeaveEntitlementPolicyMcpTools {

    private final LeaveEntitlementPolicyService policyService;
    private final LeaveEntitlementPolicyEligibilityService eligibilityService;
    private final LeaveEntitlementPolicyRepository policyRepository;

    @Tool(description = "Get leave entitlement policies configured for a jurisdiction. Use this instead of tenant tools for entitlement-policy questions.")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ + "')")
    public List<LeaveEntitlementPolicy> getEntitlementPoliciesByJurisdiction(String jurisdictionId) {
        String jurisdiction = requireJurisdiction(jurisdictionId);
        Set<String> templateIds = templateIdsForJurisdiction(jurisdiction);
        return policyService.findAll().stream()
                .filter(policy -> belongsToJurisdiction(policy, jurisdiction, templateIds))
                .toList();
    }

    @Tool(description = "Get eligibility rules for an accessible leave entitlement policy by policy ID.")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ + "')")
    public List<LeaveEntitlementPolicyEligibilityRule> getEligibilityRulesByEntitlementPolicyId(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        return eligibilityService.findAll(policyId.trim());
    }

    @Tool(description = "Get leave entitlement policies and their eligibility rules for a jurisdiction in one call. Prefer this tool when the user asks about entitlement policies, eligibility, or both for a jurisdiction such as Singapore (SG).")
    @PreAuthorize("hasAuthority('" + RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ + "')")
    public List<EntitlementConfiguration> getLeaveEntitlementConfigurationByJurisdiction(String jurisdictionId) {
        return getEntitlementPoliciesByJurisdiction(jurisdictionId).stream()
                .map(policy -> new EntitlementConfiguration(policy, eligibilityService.findAll(policy.getId())))
                .toList();
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

    public record EntitlementConfiguration(
            LeaveEntitlementPolicy policy,
            List<LeaveEntitlementPolicyEligibilityRule> eligibilityRules
    ) {
    }
}

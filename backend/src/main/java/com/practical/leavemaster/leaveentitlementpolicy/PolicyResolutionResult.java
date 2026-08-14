package com.practical.leavemaster.leaveentitlementpolicy;

import java.util.List;

public record PolicyResolutionResult(
        String staffId,
        String leaveTypeId,
        String selectedPolicyId,
        boolean ambiguous,
        String reason,
        List<PolicyEvaluation> consideredPolicies
) {
    public record PolicyEvaluation(
            String policyId,
            String policyName,
            int priority,
            boolean effective,
            boolean matched,
            List<RuleEvaluation> rules,
            String reason
    ) {}

    public record RuleEvaluation(
            String ruleId,
            EligibilityCriterionType criterionType,
            EligibilityOperator operator,
            boolean matched,
            String reason
    ) {}
}

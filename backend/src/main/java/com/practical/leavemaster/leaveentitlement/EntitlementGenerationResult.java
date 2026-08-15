package com.practical.leavemaster.leaveentitlement;

import java.math.BigDecimal;

public record EntitlementGenerationResult(
        String staffId,
        String leaveTypeId,
        String entitlementId,
        String policyId,
        Status status,
        BigDecimal baseAmount,
        BigDecimal carriedForwardAmount,
        BigDecimal adjustmentAmount,
        BigDecimal usedAmount,
        BigDecimal reservedAmount,
        BigDecimal entitlementAmount,
        String reason
) {
    public enum Status {
        CREATED,
        UPDATED,
        NO_MATCHING_POLICY,
        AMBIGUOUS_POLICY,
        LEGACY_PROTECTED,
        HISTORICAL_PROTECTED
    }
}

package com.practical.leavemaster.leaveentitlementpolicy;

/**
 * Describes how a leave policy produces or validates entitlement.
 *
 * <p>The model is intentionally jurisdiction-agnostic. Jurisdiction-specific rules belong in
 * templates and eligibility/event configuration rather than in this enum.</p>
 */
public enum LeavePolicyModel {
    /** A conventional recurring balance for a leave year. */
    ANNUAL_ENTITLEMENT,

    /** A recurring leave-year balance that is generated only when eligibility conditions match. */
    CONDITIONAL_ANNUAL_ENTITLEMENT,

    /** Entitlement is associated with a qualifying event and is not a normal annual balance. */
    EVENT_BASED,

    /** Leave can be requested without a generated standard balance or qualifying event entitlement. */
    REQUEST_BASED
}

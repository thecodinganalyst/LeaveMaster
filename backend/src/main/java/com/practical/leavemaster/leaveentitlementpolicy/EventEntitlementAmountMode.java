package com.practical.leavemaster.leaveentitlementpolicy;

/**
 * Describes how an event-based policy determines the grant associated with one qualifying event.
 * The modes are jurisdiction-neutral so statutory schemes can be configured without country branches.
 */
public enum EventEntitlementAmountMode {
    FIXED,
    APPROVED_EVENT_AMOUNT,
    EVENT_PERIOD_WORKING_DAYS
}

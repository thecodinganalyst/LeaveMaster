package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;

import java.util.List;

public record StaffEntitlementProposalAnalysis(
        List<LeaveEntitlement> proposals,
        Status status) {

    public enum Status {
        AVAILABLE,
        NO_TEMPLATE,
        NOT_ELIGIBLE_IN_PERIOD
    }
}

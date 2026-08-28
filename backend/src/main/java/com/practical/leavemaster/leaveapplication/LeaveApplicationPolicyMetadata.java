package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.leaveentitlementpolicy.LeavePolicyModel;

public record LeaveApplicationPolicyMetadata(
        LeavePolicyModel policyModel,
        boolean eventBased,
        boolean eventRequiresVerification
) {
}

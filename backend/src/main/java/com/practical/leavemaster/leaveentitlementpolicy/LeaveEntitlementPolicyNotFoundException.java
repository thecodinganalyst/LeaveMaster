package com.practical.leavemaster.leaveentitlementpolicy;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LeaveEntitlementPolicyNotFoundException extends RuntimeException {
    public LeaveEntitlementPolicyNotFoundException(String id) {
        super("Leave entitlement policy not found: " + id);
    }
}

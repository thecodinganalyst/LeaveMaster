package com.practical.leavemaster.leaveentitlementpolicy;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LeaveEntitlementPolicyValidationException extends RuntimeException {
    public LeaveEntitlementPolicyValidationException(String message) {
        super(message);
    }
}

package com.practical.leavemaster.tenant;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class DemoTenantOperationException extends IllegalStateException {

    public DemoTenantOperationException(String operation) {
        super("Operation is not available in a DEMO tenant: " + operation);
    }
}

package com.practical.leavemaster.tenant;

public class DemoTenantOperationException extends IllegalStateException {

    public DemoTenantOperationException(String operation) {
        super("Operation is not available in a DEMO tenant: " + operation);
    }
}

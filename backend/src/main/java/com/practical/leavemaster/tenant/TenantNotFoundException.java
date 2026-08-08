package com.practical.leavemaster.tenant;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String id) {
        super("Tenant not found: " + id);
    }
}

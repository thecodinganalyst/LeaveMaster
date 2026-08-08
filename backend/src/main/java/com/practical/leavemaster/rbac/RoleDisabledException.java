package com.practical.leavemaster.rbac;

public class RoleDisabledException extends RuntimeException {

    public RoleDisabledException(String roleId) {
        super("Role is disabled: " + roleId);
    }
}

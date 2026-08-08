package com.practical.leavemaster.rbac;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(String roleId) {
        super("Role not found: " + roleId);
    }
}

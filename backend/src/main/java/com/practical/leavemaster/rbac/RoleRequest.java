package com.practical.leavemaster.rbac;

import lombok.Data;

import java.util.Set;

@Data
public class RoleRequest {
    private String id;
    private String description;
    private boolean active = true;
    private Set<String> permissionCodes;
}

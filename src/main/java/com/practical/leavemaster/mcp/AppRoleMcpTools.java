package com.practical.leavemaster.mcp;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleService;
import com.practical.leavemaster.rbac.RoleRequest;
import com.practical.leavemaster.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AppRoleMcpTools {

    private final AppRoleService appRoleService;

    @Tool(description = "Get all application roles")
    public List<AppRole> getAllRoles() {
        return appRoleService.findAll();
    }

    @Tool(description = "Get all application roles for a tenant by tenant ID")
    public List<AppRole> getRolesByTenantId(String tenantId) {
        return appRoleService.findByTenantId(tenantId);
    }

    @Tool(description = "Get all available permissions")
    public List<AppPermission> getAllPermissions() {
        return appRoleService.findAllPermissions();
    }

    @Tool(description = "Get an application role by role ID")
    public Optional<AppRole> getRoleById(String roleId) {
        return appRoleService.findById(roleId);
    }

    @Tool(description = "Create a new application role")
    public AppRole createRole(RoleRequest request) {
        return appRoleService.create(request);
    }

    @Tool(description = "Update an existing application role by role ID")
    public AppRole updateRole(String roleId, RoleRequest request) {
        return appRoleService.update(roleId, request);
    }

    @Tool(description = "Disable an application role by role ID")
    public AppRole disableRole(String roleId) {
        return appRoleService.disable(roleId);
    }

    @Tool(description = "Enable an application role by role ID")
    public AppRole enableRole(String roleId) {
        return appRoleService.enable(roleId);
    }

    @Tool(description = "Add a user to a role by role ID and login name")
    public AppUser addUserToRole(String roleId, String loginName) {
        return appRoleService.addUserToRole(roleId, loginName);
    }

    @Tool(description = "Remove a user from a role by role ID and login name")
    public AppUser removeUserFromRole(String roleId, String loginName) {
        return appRoleService.removeUserFromRole(roleId, loginName);
    }
}

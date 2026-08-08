package com.practical.leavemaster.mcp;

import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.tenant.Tenant;
import com.practical.leavemaster.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantMcpTools {

    private final TenantService tenantService;

    @Tool(description = "Get all tenants")
    @PreAuthorize("hasAuthority('" + RbacPermissions.TENANT_READ + "')")
    public List<Tenant> getAllTenants() {
        return tenantService.findAll();
    }

    @Tool(description = "Get a tenant by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.TENANT_READ + "')")
    public Optional<Tenant> getTenantById(String id) {
        return tenantService.findById(id);
    }

    @Tool(description = "Create a new tenant")
    @PreAuthorize("hasAuthority('" + RbacPermissions.TENANT_WRITE + "')")
    public Tenant createTenant(Tenant tenant) {
        return tenantService.save(tenant);
    }

    @Tool(description = "Update an existing tenant")
    @PreAuthorize("hasAuthority('" + RbacPermissions.TENANT_WRITE + "')")
    public Tenant updateTenant(String id, Tenant tenant) {
        return tenantService.update(id, tenant);
    }

    @Tool(description = "Delete a tenant by ID")
    @PreAuthorize("hasAuthority('" + RbacPermissions.TENANT_WRITE + "')")
    public void deleteTenant(String id) {
        tenantService.delete(id);
    }
}

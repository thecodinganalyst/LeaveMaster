package com.practical.leavemaster.mcp;

import com.practical.leavemaster.tenant.Tenant;
import com.practical.leavemaster.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantMcpTools {

    private final TenantService tenantService;

    @Tool(description = "Get all tenants")
    public List<Tenant> getAllTenants() {
        return tenantService.findAll();
    }

    @Tool(description = "Get a tenant by ID")
    public Optional<Tenant> getTenantById(String id) {
        return tenantService.findById(id);
    }

    @Tool(description = "Create a new tenant")
    public Tenant createTenant(Tenant tenant) {
        return tenantService.save(tenant);
    }

    @Tool(description = "Update an existing tenant")
    public Tenant updateTenant(String id, Tenant tenant) {
        return tenantService.update(id, tenant);
    }

    @Tool(description = "Delete a tenant by ID")
    public void deleteTenant(String id) {
        tenantService.delete(id);
    }
}

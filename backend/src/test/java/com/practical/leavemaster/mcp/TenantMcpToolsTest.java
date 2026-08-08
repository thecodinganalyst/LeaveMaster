package com.practical.leavemaster.mcp;

import com.practical.leavemaster.tenant.Tenant;
import com.practical.leavemaster.tenant.TenantService;
import com.practical.leavemaster.tenant.TenantStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantMcpToolsTest {

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private TenantMcpTools tenantMcpTools;

    @Test
    void shouldGetAllTenants() {
        List<Tenant> tenants = List.of(Tenant.builder().id("t1").name("Tenant One").status(TenantStatus.ACTIVE).build());
        when(tenantService.findAll()).thenReturn(tenants);

        List<Tenant> result = tenantMcpTools.getAllTenants();

        assertThat(result).hasSize(1);
        verify(tenantService).findAll();
    }

    @Test
    void shouldGetTenantById() {
        Tenant tenant = Tenant.builder().id("t1").name("Tenant One").status(TenantStatus.ACTIVE).build();
        when(tenantService.findById("t1")).thenReturn(Optional.of(tenant));

        Optional<Tenant> result = tenantMcpTools.getTenantById("t1");

        assertThat(result).isPresent();
        verify(tenantService).findById("t1");
    }

    @Test
    void shouldCreateTenant() {
        Tenant tenant = Tenant.builder().id("t1").name("Tenant One").status(TenantStatus.ACTIVE).build();
        when(tenantService.save(tenant)).thenReturn(tenant);

        Tenant result = tenantMcpTools.createTenant(tenant);

        assertThat(result.getId()).isEqualTo("t1");
        verify(tenantService).save(tenant);
    }

    @Test
    void shouldUpdateTenant() {
        Tenant tenant = Tenant.builder().id("t1").name("Updated Tenant").status(TenantStatus.ACTIVE).build();
        when(tenantService.update("t1", tenant)).thenReturn(tenant);

        Tenant result = tenantMcpTools.updateTenant("t1", tenant);

        assertThat(result.getName()).isEqualTo("Updated Tenant");
        verify(tenantService).update("t1", tenant);
    }

    @Test
    void shouldDeleteTenant() {
        doNothing().when(tenantService).delete("t1");

        tenantMcpTools.deleteTenant("t1");

        verify(tenantService).delete("t1");
    }
}

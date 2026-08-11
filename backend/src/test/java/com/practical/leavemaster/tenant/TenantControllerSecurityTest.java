package com.practical.leavemaster.tenant;

import com.practical.leavemaster.config.SecurityConfig;
import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TenantController.class)
@Import(SecurityConfig.class)
class TenantControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    void platformAdminAuthoritiesAllowTenantCrud() throws Exception {
        Tenant tenant = tenant("T1", "Tenant One");
        Tenant updated = tenant("T1", "Tenant One Updated");
        when(tenantService.findAll()).thenReturn(List.of(tenant));
        when(tenantService.findById("T1")).thenReturn(java.util.Optional.of(tenant));
        when(tenantService.save(any(Tenant.class))).thenReturn(tenant);
        when(tenantService.update(eq("T1"), any(Tenant.class))).thenReturn(updated);
        doNothing().when(tenantService).delete("T1");

        mockMvc.perform(get("/tenants").with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("T1"));

        mockMvc.perform(get("/tenants/T1").with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tenant One"));

        mockMvc.perform(post("/tenants")
                        .with(platformAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tenant)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/tenants/T1")
                        .with(platformAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tenant One Updated"));

        mockMvc.perform(delete("/tenants/T1").with(platformAdmin()).with(csrf()))
                .andExpect(status().isNoContent());

        verify(tenantService).delete("T1");
    }

    @Test
    void tenantReadAllowsReadsButDeniesMutations() throws Exception {
        when(tenantService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/tenants").with(tenantReader()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tenants")
                        .with(tenantReader())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tenant("T1", "Tenant One"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/tenants/T1").with(tenantReader()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void userWithoutTenantPermissionsIsDenied() throws Exception {
        mockMvc.perform(get("/tenants").with(ordinaryUser()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/tenants")
                        .with(ordinaryUser())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tenant("T1", "Tenant One"))))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor platformAdmin() {
        return user("PlatformAdmin").authorities(
                new SimpleGrantedAuthority(RbacPermissions.TENANT_READ),
                new SimpleGrantedAuthority(RbacPermissions.TENANT_WRITE));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor tenantReader() {
        return user("tenant-reader").authorities(new SimpleGrantedAuthority(RbacPermissions.TENANT_READ));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor ordinaryUser() {
        return user("ordinary-user").authorities(new SimpleGrantedAuthority(RbacPermissions.STAFF_READ));
    }

    private Tenant tenant(String id, String name) {
        return Tenant.builder()
                .id(id)
                .name(name)
                .startDate(LocalDate.of(2026, 1, 1))
                .status(TenantStatus.ACTIVE)
                .build();
    }
}

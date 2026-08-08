package com.practical.leavemaster.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantController.class)
@WithMockUser
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private SecurityFilterChain securityFilterChain;

    @Test
    void shouldReturnAllTenants() throws Exception {
        List<Tenant> tenants = List.of(
                Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.of(2024, 1, 1)).status(TenantStatus.ACTIVE).build(),
                Tenant.builder().id("t2").name("Tenant 2").startDate(LocalDate.of(2024, 6, 1)).status(TenantStatus.DORMANT).build()
        );
        when(tenantService.findAll()).thenReturn(tenants);

        mockMvc.perform(get("/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("t1"))
                .andExpect(jsonPath("$[1].id").value("t2"));
    }

    @Test
    void shouldReturnTenantById() throws Exception {
        Tenant tenant = Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.of(2024, 1, 1)).status(TenantStatus.ACTIVE).build();
        when(tenantService.findById("t1")).thenReturn(Optional.of(tenant));

        mockMvc.perform(get("/tenants/t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("t1"))
                .andExpect(jsonPath("$.name").value("Tenant 1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn404WhenTenantNotFound() throws Exception {
        when(tenantService.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/tenants/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateTenant() throws Exception {
        Tenant tenant = Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.of(2024, 1, 1)).status(TenantStatus.ACTIVE).build();
        when(tenantService.save(any(Tenant.class))).thenReturn(tenant);

        mockMvc.perform(post("/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tenant)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("t1"))
                .andExpect(jsonPath("$.name").value("Tenant 1"));
    }

    @Test
    void shouldUpdateTenant() throws Exception {
        Tenant updated = Tenant.builder().id("t1").name("Updated Name").startDate(LocalDate.of(2024, 1, 1)).status(TenantStatus.DORMANT).build();
        when(tenantService.update(eq("t1"), any(Tenant.class))).thenReturn(updated);

        mockMvc.perform(put("/tenants/t1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.status").value("DORMANT"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentTenant() throws Exception {
        when(tenantService.update(eq("nonexistent"), any(Tenant.class)))
                .thenThrow(new TenantNotFoundException("nonexistent"));

        mockMvc.perform(put("/tenants/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Tenant())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteTenant() throws Exception {
        doNothing().when(tenantService).delete("t1");

        mockMvc.perform(delete("/tenants/t1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentTenant() throws Exception {
        doThrow(new TenantNotFoundException("nonexistent")).when(tenantService).delete("nonexistent");

        mockMvc.perform(delete("/tenants/nonexistent"))
                .andExpect(status().isNotFound());
    }
}

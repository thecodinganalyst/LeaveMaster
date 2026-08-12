package com.practical.leavemaster.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigRbacAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/tenants"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthorizedPermission() throws Exception {
        mockMvc.perform(get("/tenants").with(user("alice").authorities(() -> "TENANT_READ")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tenants").with(user("alice").authorities(() -> "TENANT_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldForbidWhenPermissionMissing() throws Exception {
        mockMvc.perform(get("/tenants").with(user("bob").authorities(() -> "STAFF_READ")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tenants").with(user("bob").authorities(() -> "STAFF_READ")))
                .andExpect(status().isForbidden());
    }
}

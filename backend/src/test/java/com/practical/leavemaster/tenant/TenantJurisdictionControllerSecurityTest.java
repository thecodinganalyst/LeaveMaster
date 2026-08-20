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

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TenantJurisdictionController.class)
@Import(SecurityConfig.class)
class TenantJurisdictionControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private TenantService tenantService;
    @MockitoBean private AppUserRepository appUserRepository;

    @Test
    void tenantCalendarPermissionsAllowViewingAndAddingJurisdictions() throws Exception {
        TenantJurisdiction association = TenantJurisdiction.builder()
                .id("ACME:SG")
                .tenantId("ACME")
                .jurisdictionId("SG")
                .build();
        TenantJurisdictionProvisionRequest request = new TenantJurisdictionProvisionRequest("SG", true, true, null, null);
        when(tenantService.findJurisdictionsForUser("ACME_Admin")).thenReturn(List.of(association));
        when(tenantService.addJurisdictionForUser(eq("ACME_Admin"), eq(request))).thenReturn(association);

        mockMvc.perform(get("/api/tenant-jurisdictions").with(user("ACME_Admin").authorities(
                        new SimpleGrantedAuthority(RbacPermissions.LEAVE_CALENDAR_READ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jurisdictionId").value("SG"));

        mockMvc.perform(post("/api/tenant-jurisdictions")
                        .with(user("ACME_Admin").authorities(new SimpleGrantedAuthority(RbacPermissions.LEAVE_CALENDAR_WRITE)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value("ACME"));

        verify(tenantService).findJurisdictionsForUser("ACME_Admin");
        verify(tenantService).addJurisdictionForUser("ACME_Admin", request);
    }

    @Test
    void userWithoutLeaveCalendarPermissionIsDenied() throws Exception {
        mockMvc.perform(get("/api/tenant-jurisdictions")
                        .with(user("staff").authorities(new SimpleGrantedAuthority(RbacPermissions.LEAVE_TYPE_READ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/tenant-jurisdictions")
                        .with(user("staff").authorities(new SimpleGrantedAuthority(RbacPermissions.LEAVE_TYPE_READ)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TenantJurisdictionProvisionRequest("SG", false, false, null, null))))
                .andExpect(status().isForbidden());
    }
}

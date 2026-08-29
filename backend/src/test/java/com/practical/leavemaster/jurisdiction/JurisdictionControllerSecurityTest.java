package com.practical.leavemaster.jurisdiction;

import com.practical.leavemaster.config.PlatformAdminAccess;
import com.practical.leavemaster.config.SecurityConfig;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JurisdictionController.class)
@Import(SecurityConfig.class)
class JurisdictionControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private JurisdictionService jurisdictionService;
    @MockitoBean private JurisdictionLeaveTypeService leaveTypeService;
    @MockitoBean private PlatformAdminAccess platformAdminAccess;
    @MockitoBean private AppUserRepository appUserRepository;

    private static final SimpleGrantedAuthority JURISDICTION_WRITE =
            new SimpleGrantedAuthority("JURISDICTION_WRITE");

    @Test
    void tenantAdminWithJurisdictionWritePermissionCannotMutateGlobalCatalog() throws Exception {
        when(platformAdminAccess.isPlatformAdmin(any(Authentication.class))).thenReturn(false);

        mockMvc.perform(post("/api/jurisdictions")
                        .with(user("Bravo_Admin").authorities(JURISDICTION_WRITE))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/jurisdictions/SG")
                        .with(user("Bravo_Admin").authorities(JURISDICTION_WRITE))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/jurisdictions/SG")
                        .with(user("Bravo_Admin").authorities(JURISDICTION_WRITE))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void platformAdminWithJurisdictionWritePermissionCanMutateGlobalCatalog() throws Exception {
        when(platformAdminAccess.isPlatformAdmin(any(Authentication.class))).thenReturn(true);

        mockMvc.perform(post("/api/jurisdictions")
                        .with(user("PlatformAdmin").authorities(JURISDICTION_WRITE))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/jurisdictions/SG")
                        .with(user("PlatformAdmin").authorities(JURISDICTION_WRITE))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/jurisdictions/SG")
                        .with(user("PlatformAdmin").authorities(JURISDICTION_WRITE))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}

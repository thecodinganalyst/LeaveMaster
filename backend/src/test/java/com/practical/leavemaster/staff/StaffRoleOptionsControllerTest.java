package com.practical.leavemaster.staff;

import com.practical.leavemaster.rbac.AppRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StaffController.class)
@WithMockUser
class StaffRoleOptionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StaffService staffService;

    @MockitoBean
    private StaffRoleAssignmentPolicy staffRoleAssignmentPolicy;

    @MockitoBean
    private SecurityFilterChain securityFilterChain;

    @Test
    void returnsAssignableRoleOptions() throws Exception {
        when(staffRoleAssignmentPolicy.findAssignableRoles()).thenReturn(List.of(
                AppRole.builder().id("ACME_Staff").description("Acme Staff").active(true).tenantId("ACME").build(),
                AppRole.builder().id("ACME_Manager").description("Acme Manager").active(true).tenantId("ACME").build()));

        mockMvc.perform(get("/api/staff/role-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("ACME_Staff"))
                .andExpect(jsonPath("$[1].id").value("ACME_Manager"));
    }
}

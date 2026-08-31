package com.practical.leavemaster.staff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StaffController.class)
@WithMockUser
class StaffControllerRoleAssignmentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StaffService staffService;

    @MockitoBean
    private StaffRoleAssignmentPolicy staffRoleAssignmentPolicy;

    @MockitoBean
    private SecurityFilterChain securityFilterChain;

    @Test
    void rejectsRoleAssignmentOutsideCurrentUsersPermissionCeiling() throws Exception {
        StaffWriteRequest request = new StaffWriteRequest(
                "S001", "Alice", "alice@example.com", null, null, null,
                "SG", null, "alice", Set.of("ACME_Admin"));
        doThrow(new IllegalArgumentException("Role is not assignable by the current user: ACME_Admin"))
                .when(staffRoleAssignmentPolicy).validateAssignableRoleIds(eq(Set.of("ACME_Admin")));

        mockMvc.perform(post("/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Role is not assignable by the current user: ACME_Admin"));

        verify(staffRoleAssignmentPolicy).validateAssignableRoleIds(Set.of("ACME_Admin"));
    }
}

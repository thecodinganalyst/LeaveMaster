package com.practical.leavemaster.config;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.staff.StaffController;
import com.practical.leavemaster.staff.StaffRoleAssignmentPolicy;
import com.practical.leavemaster.staff.StaffService;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StaffController.class)
@Import(SecurityConfig.class)
class StaffRoleLookupSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StaffService staffService;

    @MockitoBean
    private StaffRoleAssignmentPolicy staffRoleAssignmentPolicy;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(authorities = {RbacPermissions.STAFF_READ, RbacPermissions.STAFF_WRITE})
    void hrCanLoadAssignableRoleOptions() throws Exception {
        when(staffRoleAssignmentPolicy.findAssignableRoles()).thenReturn(List.of(
                role("ACME_Staff"), role("ACME_Manager")));

        mockMvc.perform(get("/api/staff/role-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("ACME_Staff"));
    }

    @Test
    @WithMockUser(authorities = {RbacPermissions.LEAVE_APPLICATION_READ, RbacPermissions.LEAVE_APPLICATION_APPROVE})
    void managerCannotLoadStaffRoleOptions() throws Exception {
        mockMvc.perform(get("/api/staff/role-options"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {RbacPermissions.STAFF_READ, RbacPermissions.STAFF_WRITE})
    void hrStillCannotAccessRoleAdministrationEndpoint() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isForbidden());
    }

    private static AppRole role(String id) {
        return AppRole.builder()
                .id(id)
                .description(id)
                .tenantId("ACME")
                .active(true)
                .permissions(Set.of(AppPermission.builder()
                        .code(RbacPermissions.LEAVE_APPLICATION_READ)
                        .description("Read leave applications")
                        .build()))
                .build();
    }
}

package com.practical.leavemaster.rbac;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppRoleController.class)
@WithMockUser
class AppRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppRoleService appRoleService;

    @MockitoBean
    private SecurityFilterChain securityFilterChain;

    @Test
    void shouldReturnAllRoles() throws Exception {
        when(appRoleService.findAll()).thenReturn(List.of(role("ADMIN"), role("MANAGER")));

        mockMvc.perform(get("/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("ADMIN"))
                .andExpect(jsonPath("$[1].id").value("MANAGER"));
    }

    @Test
    void shouldReturnAllPermissions() throws Exception {
        when(appRoleService.findAllPermissions()).thenReturn(List.of(
                AppPermission.builder().code("ROLE_MANAGE").description("Manage roles").build(),
                AppPermission.builder().code("USER_READ").description("Read users").build()
        ));

        mockMvc.perform(get("/roles/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("ROLE_MANAGE"));
    }

    @Test
    void shouldReturnRoleById() throws Exception {
        when(appRoleService.findById("ADMIN")).thenReturn(Optional.of(role("ADMIN")));

        mockMvc.perform(get("/roles/ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ADMIN"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturn404WhenRoleNotFound() throws Exception {
        when(appRoleService.findById("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/roles/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateRole() throws Exception {
        when(appRoleService.create(any(RoleRequest.class))).thenReturn(role("ADMIN"));

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ADMIN"))
                .andExpect(jsonPath("$.permissions[0].code").value("ROLE_MANAGE"));
    }

    @Test
    void shouldReturn400WhenCreateRequestIsInvalid() throws Exception {
        when(appRoleService.create(any(RoleRequest.class)))
                .thenThrow(new IllegalArgumentException("Role description must not be blank"));

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Role description must not be blank"));
    }

    @Test
    void shouldUpdateRole() throws Exception {
        AppRole updated = role("ADMIN");
        updated.setDescription("Updated admin role");
        when(appRoleService.update(eq("ADMIN"), any(RoleRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/roles/ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated admin role"));
    }

    @Test
    void shouldReturn404WhenUpdatingUnknownRole() throws Exception {
        when(appRoleService.update(eq("UNKNOWN"), any(RoleRequest.class)))
                .thenThrow(new RoleNotFoundException("UNKNOWN"));

        mockMvc.perform(put("/roles/UNKNOWN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenUpdateRequestIsInvalid() throws Exception {
        when(appRoleService.update(eq("ADMIN"), any(RoleRequest.class)))
                .thenThrow(new IllegalArgumentException("Role description must not be blank"));

        mockMvc.perform(put("/roles/ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Role description must not be blank"));
    }

    @Test
    void shouldDisableRole() throws Exception {
        AppRole disabled = role("ADMIN");
        disabled.setActive(false);
        when(appRoleService.disable("ADMIN")).thenReturn(disabled);

        mockMvc.perform(put("/roles/ADMIN/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldReturn404WhenDisablingUnknownRole() throws Exception {
        when(appRoleService.disable("UNKNOWN")).thenThrow(new RoleNotFoundException("UNKNOWN"));

        mockMvc.perform(put("/roles/UNKNOWN/disable"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldEnableRole() throws Exception {
        when(appRoleService.enable("ADMIN")).thenReturn(role("ADMIN"));

        mockMvc.perform(put("/roles/ADMIN/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturn404WhenEnablingUnknownRole() throws Exception {
        when(appRoleService.enable("UNKNOWN")).thenThrow(new RoleNotFoundException("UNKNOWN"));

        mockMvc.perform(put("/roles/UNKNOWN/enable"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAddUserToRole() throws Exception {
        when(appRoleService.addUserToRole("ADMIN", "alice")).thenReturn(user("alice", Set.of(role("ADMIN"))));

        mockMvc.perform(put("/roles/ADMIN/users/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginName").value("alice"))
                .andExpect(jsonPath("$.roles[0].id").value("ADMIN"));
    }

    @Test
    void shouldReturn404WhenAddingUnknownUserOrRole() throws Exception {
        when(appRoleService.addUserToRole("ADMIN", "missing"))
                .thenThrow(new AppUserNotFoundException("missing"));

        mockMvc.perform(put("/roles/ADMIN/users/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenAddingUserToDisabledRole() throws Exception {
        when(appRoleService.addUserToRole("DISABLED", "alice"))
                .thenThrow(new RoleDisabledException("DISABLED"));

        mockMvc.perform(put("/roles/DISABLED/users/alice"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Role is disabled: DISABLED"));
    }

    @Test
    void shouldRemoveUserFromRole() throws Exception {
        when(appRoleService.removeUserFromRole("ADMIN", "alice")).thenReturn(user("alice", Set.of()));

        mockMvc.perform(delete("/roles/ADMIN/users/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginName").value("alice"))
                .andExpect(jsonPath("$.roles").isEmpty());
    }

    @Test
    void shouldReturn404WhenRemovingUnknownUserOrRole() throws Exception {
        when(appRoleService.removeUserFromRole("ADMIN", "missing"))
                .thenThrow(new AppUserNotFoundException("missing"));

        mockMvc.perform(delete("/roles/ADMIN/users/missing"))
                .andExpect(status().isNotFound());
    }

    private static AppRole role(String id) {
        return AppRole.builder()
                .id(id)
                .description(id + " role")
                .active(true)
                .permissions(Set.of(AppPermission.builder().code("ROLE_MANAGE").description("Manage roles").build()))
                .build();
    }

    private static RoleRequest roleRequest() {
        RoleRequest request = new RoleRequest();
        request.setId("ADMIN");
        request.setDescription("Admin role");
        request.setActive(true);
        request.setPermissionCodes(Set.of("ROLE_MANAGE"));
        return request;
    }

    private static AppUser user(String loginName, Set<AppRole> roles) {
        return AppUser.builder()
                .loginName(loginName)
                .password("secret")
                .active(true)
                .roles(roles)
                .build();
    }
}

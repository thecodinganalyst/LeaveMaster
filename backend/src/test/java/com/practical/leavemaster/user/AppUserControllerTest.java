package com.practical.leavemaster.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppUserController.class)
@WithMockUser
class AppUserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AppUserService appUserService;
    @MockitoBean private AppUserRepository appUserRepository;
    @MockitoBean private TenantAuthenticationProvider tenantAuthenticationProvider;
    @MockitoBean private SecurityFilterChain securityFilterChain;

    @Test
    void shouldReturnAllUsers() throws Exception {
        List<AppUser> users = List.of(
                AppUser.builder().loginName("alice").password("pass").active(true).build(),
                AppUser.builder().loginName("bob").password("pass").active(false).build());
        when(appUserService.findAll()).thenReturn(users);
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].loginName").value("alice"))
                .andExpect(jsonPath("$[1].loginName").value("bob"));
    }

    @Test
    void shouldReturnUserByLoginName() throws Exception {
        AppUser user = AppUser.builder().loginName("alice").password("pass").active(true).build();
        when(appUserService.findByLoginName("alice")).thenReturn(Optional.of(user));
        mockMvc.perform(get("/users/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginName").value("alice"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(appUserService.findByLoginName("nonexistent")).thenReturn(Optional.empty());
        mockMvc.perform(get("/users/nonexistent")).andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateUser() throws Exception {
        AppUser user = AppUser.builder().loginName("alice").password("pass").active(true).build();
        when(appUserService.save(any(AppUser.class))).thenReturn(user);
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.loginName").value("alice"));
    }

    @Test
    void shouldReturn409WhenCreatingUserWithDuplicateLoginName() throws Exception {
        AppUser user = AppUser.builder().loginName("alice").password("pass").active(true).build();
        when(appUserService.save(any(AppUser.class))).thenThrow(new DuplicateLoginNameException("alice"));
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("Login name already exists: alice"));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        AppUser updated = AppUser.builder().loginName("alice").password("pass").active(false).build();
        when(appUserService.update(eq("alice"), any(AppUser.class))).thenReturn(updated);
        mockMvc.perform(put("/users/alice").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
        when(appUserService.update(eq("nonexistent"), any(AppUser.class))).thenThrow(new AppUserNotFoundException("nonexistent"));
        mockMvc.perform(put("/users/nonexistent").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new AppUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenUpdatingUserWithInvalidOidcCredentials() throws Exception {
        when(appUserService.update(eq("alice"), any(AppUser.class)))
                .thenThrow(new IllegalArgumentException("Both oidcProvider and oidcSubject must be provided together"));
        mockMvc.perform(put("/users/alice").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AppUser.builder().active(true).oidcProvider("github").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Both oidcProvider and oidcSubject must be provided together"));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        doNothing().when(appUserService).delete("alice");
        mockMvc.perform(delete("/users/alice")).andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
        doThrow(new AppUserNotFoundException("nonexistent")).when(appUserService).delete("nonexistent");
        mockMvc.perform(delete("/users/nonexistent")).andExpect(status().isNotFound());
    }

    @Test
    void shouldChangePassword() throws Exception {
        AppUser user = AppUser.builder().loginName("alice").password("newPass").active(true).build();
        when(appUserService.changePassword("alice", "newPass")).thenReturn(user);
        mockMvc.perform(put("/users/alice/change-password").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "newPass"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.loginName").value("alice"));
    }

    @Test
    void shouldReturn404WhenChangingPasswordOfNonExistentUser() throws Exception {
        when(appUserService.changePassword(eq("nonexistent"), any())).thenThrow(new AppUserNotFoundException("nonexistent"));
        mockMvc.perform(put("/users/nonexistent/change-password").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "newPass"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenChangingPasswordToBlank() throws Exception {
        when(appUserService.changePassword("alice", "")).thenThrow(new IllegalArgumentException("New password must not be blank"));
        mockMvc.perform(put("/users/alice/change-password").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", ""))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("New password must not be blank"));
    }

    @Test
    void shouldActivateUser() throws Exception {
        AppUser user = AppUser.builder().loginName("alice").password("pass").active(true).build();
        when(appUserService.activate("alice")).thenReturn(user);
        mockMvc.perform(put("/users/alice/activate")).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturn404WhenActivatingNonExistentUser() throws Exception {
        when(appUserService.activate("nonexistent")).thenThrow(new AppUserNotFoundException("nonexistent"));
        mockMvc.perform(put("/users/nonexistent/activate")).andExpect(status().isNotFound());
    }

    @Test
    void shouldDeactivateUser() throws Exception {
        AppUser user = AppUser.builder().loginName("alice").password("pass").active(false).build();
        when(appUserService.deactivate("alice")).thenReturn(user);
        mockMvc.perform(put("/users/alice/deactivate")).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldReturn404WhenDeactivatingNonExistentUser() throws Exception {
        when(appUserService.deactivate("nonexistent")).thenThrow(new AppUserNotFoundException("nonexistent"));
        mockMvc.perform(put("/users/nonexistent/deactivate")).andExpect(status().isNotFound());
    }

    @Test
    void shouldLoginSuccessfullyWithTenantScopedCredentials() throws Exception {
        AppUser user = AppUser.builder().userId("user-a").tenantId("tenant-a").loginName("alice").password("hash").active(true).build();
        TenantAuthenticationToken authenticated = new TenantAuthenticationToken("tenant-a", "alice", "user-a", List.of());
        when(tenantAuthenticationProvider.authenticate(any(TenantAuthenticationToken.class))).thenReturn(authenticated);
        when(appUserRepository.findById("user-a")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("tenantId", "tenant-a", "loginName", "alice", "password", "pass"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.loginName").value("alice"));
    }

    @Test
    void shouldReturnGeneric401WhenTenantAwareLoginFails() throws Exception {
        when(tenantAuthenticationProvider.authenticate(any(TenantAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        mockMvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("tenantId", "wrong-tenant", "loginName", "alice", "password", "wrong"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void shouldReturnGeneric401WhenAuthenticatedPrincipalCannotBeResolved() throws Exception {
        TenantAuthenticationToken authenticated = new TenantAuthenticationToken("tenant-a", "alice", "missing-user-id", List.of());
        when(tenantAuthenticationProvider.authenticate(any(TenantAuthenticationToken.class))).thenReturn(authenticated);
        when(appUserRepository.findById("missing-user-id")).thenReturn(Optional.empty());
        mockMvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("tenantId", "tenant-a", "loginName", "alice", "password", "pass"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("Invalid credentials"));
    }
}

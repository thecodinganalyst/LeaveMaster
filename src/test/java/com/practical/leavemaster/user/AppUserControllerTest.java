package com.practical.leavemaster.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
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
class AppUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppUserService appUserService;

    @Test
    void shouldReturnAllUsers() throws Exception {
        List<AppUser> users = List.of(
                AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build(),
                AppUser.builder().id("U002").loginName("bob").password("pass").active(false).build()
        );
        when(appUserService.findAll()).thenReturn(users);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("U001"))
                .andExpect(jsonPath("$[1].id").value("U002"));
    }

    @Test
    void shouldReturnUserById() throws Exception {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        when(appUserService.findById("U001")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/users/U001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("U001"))
                .andExpect(jsonPath("$.loginName").value("alice"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(appUserService.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateUser() throws Exception {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        when(appUserService.save(any(AppUser.class))).thenReturn(user);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("U001"))
                .andExpect(jsonPath("$.loginName").value("alice"));
    }

    @Test
    void shouldReturn409WhenCreatingUserWithDuplicateLoginName() throws Exception {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        when(appUserService.save(any(AppUser.class))).thenThrow(new DuplicateLoginNameException("alice"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Login name already exists: alice"));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        AppUser updated = AppUser.builder().id("U001").loginName("alice-updated").password("pass").active(false).build();
        when(appUserService.update(eq("U001"), any(AppUser.class))).thenReturn(updated);

        mockMvc.perform(put("/users/U001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginName").value("alice-updated"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
        when(appUserService.update(eq("nonexistent"), any(AppUser.class)))
                .thenThrow(new AppUserNotFoundException("nonexistent"));

        mockMvc.perform(put("/users/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AppUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenUpdatingWithDuplicateLoginName() throws Exception {
        when(appUserService.update(eq("U001"), any(AppUser.class)))
                .thenThrow(new DuplicateLoginNameException("bob"));

        mockMvc.perform(put("/users/U001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AppUser())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Login name already exists: bob"));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        doNothing().when(appUserService).delete("U001");

        mockMvc.perform(delete("/users/U001"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
        doThrow(new AppUserNotFoundException("nonexistent")).when(appUserService).delete("nonexistent");

        mockMvc.perform(delete("/users/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldChangePassword() throws Exception {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("newPass").active(true).build();
        when(appUserService.changePassword(eq("U001"), eq("newPass"))).thenReturn(user);

        mockMvc.perform(put("/users/U001/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "newPass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("U001"));
    }

    @Test
    void shouldReturn404WhenChangingPasswordOfNonExistentUser() throws Exception {
        when(appUserService.changePassword(eq("nonexistent"), any()))
                .thenThrow(new AppUserNotFoundException("nonexistent"));

        mockMvc.perform(put("/users/nonexistent/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "newPass"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenChangingPasswordToBlank() throws Exception {
        when(appUserService.changePassword(eq("U001"), eq("")))
                .thenThrow(new IllegalArgumentException("New password must not be blank"));

        mockMvc.perform(put("/users/U001/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("New password must not be blank"));
    }

    @Test
    void shouldActivateUser() throws Exception {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        when(appUserService.activate("U001")).thenReturn(user);

        mockMvc.perform(put("/users/U001/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturn404WhenActivatingNonExistentUser() throws Exception {
        when(appUserService.activate("nonexistent")).thenThrow(new AppUserNotFoundException("nonexistent"));

        mockMvc.perform(put("/users/nonexistent/activate"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeactivateUser() throws Exception {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(false).build();
        when(appUserService.deactivate("U001")).thenReturn(user);

        mockMvc.perform(put("/users/U001/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldReturn404WhenDeactivatingNonExistentUser() throws Exception {
        when(appUserService.deactivate("nonexistent")).thenThrow(new AppUserNotFoundException("nonexistent"));

        mockMvc.perform(put("/users/nonexistent/deactivate"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        when(appUserService.login("alice", "pass")).thenReturn(user);

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("loginName", "alice", "password", "pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("U001"));
    }

    @Test
    void shouldReturn401WhenLoginWithInvalidCredentials() throws Exception {
        when(appUserService.login("alice", "wrong"))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("loginName", "alice", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void shouldReturn401WhenLoginWithNonExistentUser() throws Exception {
        when(appUserService.login("nonexistent", "pass"))
                .thenThrow(new AppUserNotFoundException("nonexistent"));

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("loginName", "nonexistent", "password", "pass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void shouldReturn403WhenLoginWithInactiveUser() throws Exception {
        when(appUserService.login("alice", "pass"))
                .thenThrow(new IllegalStateException("User account is not active"));

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("loginName", "alice", "password", "pass"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("User account is not active"));
    }
}

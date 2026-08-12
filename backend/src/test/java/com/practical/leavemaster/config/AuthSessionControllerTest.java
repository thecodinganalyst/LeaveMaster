package com.practical.leavemaster.config;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthSessionControllerTest {

    private AppUserRepository appUserRepository;
    private AppUserService appUserService;
    private AuthSessionController controller;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        appUserService = mock(AppUserService.class);
        controller = new AuthSessionController(appUserRepository, appUserService);
    }

    @Test
    void shouldReturnCsrfTokenMetadata() {
        DefaultCsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-value");

        AuthSessionController.CsrfResponse response = controller.csrf(token);

        assertThat(response.token()).isEqualTo("token-value");
        assertThat(response.headerName()).isEqualTo("X-CSRF-TOKEN");
        assertThat(response.parameterName()).isEqualTo("_csrf");
    }

    @Test
    void shouldReturnCurrentUserWithSortedAuthorities() {
        AppUser user = AppUser.builder()
            .loginName("admin@example.com")
            .staffId("S001")
            .tenantId("tenant-1")
            .active(true)
            .build();
        when(appUserRepository.findById("admin@example.com")).thenReturn(Optional.of(user));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "admin@example.com",
            "n/a",
            List.of(
                new SimpleGrantedAuthority("STAFF_WRITE"),
                new SimpleGrantedAuthority("STAFF_READ")
            )
        );

        var response = controller.currentUser(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().loginName()).isEqualTo("admin@example.com");
        assertThat(response.getBody().staffId()).isEqualTo("S001");
        assertThat(response.getBody().tenantId()).isEqualTo("tenant-1");
        assertThat(response.getBody().active()).isTrue();
        assertThat(response.getBody().authorities()).containsExactly("STAFF_READ", "STAFF_WRITE");
    }

    @Test
    void shouldReturnNotFoundWhenAuthenticatedUserNoLongerExists() {
        when(appUserRepository.findById("missing@example.com")).thenReturn(Optional.empty());
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken("missing@example.com", "n/a", List.of());

        var response = controller.currentUser(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }
}

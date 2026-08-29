package com.practical.leavemaster.user;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantAuthenticationFilterTest {

    @Test
    void buildsTrimmedTenantAuthenticationRequestAndDelegatesToManager() {
        AtomicReference<Authentication> captured = new AtomicReference<>();
        AuthenticationManager manager = authentication -> {
            captured.set(authentication);
            return new TenantAuthenticationToken("tenant-a", "001", "user-a", List.of());
        };
        TenantAuthenticationFilter filter = new TenantAuthenticationFilter(manager);
        MockHttpServletRequest request = loginRequest(" tenant-a ", " 001 ", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Authentication result = filter.attemptAuthentication(request, response);

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getName()).isEqualTo("user-a");
        assertThat(captured.get()).isInstanceOf(TenantAuthenticationToken.class);
        TenantAuthenticationToken token = (TenantAuthenticationToken) captured.get();
        assertThat(token.getTenantId()).isEqualTo("tenant-a");
        assertThat(token.getLoginName()).isEqualTo("001");
        assertThat(token.getCredentials()).isEqualTo("secret");
        assertThat(token.getDetails()).isNotNull();
    }

    @Test
    void successfulTenantLoginPersistsSecurityContextInHttpSession() throws Exception {
        AuthenticationManager manager = authentication ->
                new TenantAuthenticationToken("tenant-a", "001", "tenant-user-id", List.of());
        TenantAuthenticationFilter filter = new TenantAuthenticationFilter(manager);
        MockHttpServletRequest request = loginRequest("tenant-a", "001", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertPersistedAuthentication(request, "tenant-user-id", "tenant-a", "001");
    }

    @Test
    void successfulPlatformLoginPersistsSecurityContextInHttpSession() throws Exception {
        AuthenticationManager manager = authentication ->
                new TenantAuthenticationToken(
                        AuthenticationRealm.PLATFORM_REALM_ID,
                        "PlatformAdmin",
                        "platform-user-id",
                        List.of(new SimpleGrantedAuthority("TENANT_READ")));
        TenantAuthenticationFilter filter = new TenantAuthenticationFilter(manager);
        MockHttpServletRequest request = loginRequest("PLATFORM", "PlatformAdmin", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertPersistedAuthentication(request, "platform-user-id", "PLATFORM", "PlatformAdmin");
    }

    @Test
    void preservesMissingTenantForProviderToRejectGenerically() {
        AtomicReference<TenantAuthenticationToken> captured = new AtomicReference<>();
        AuthenticationManager manager = authentication -> {
            captured.set((TenantAuthenticationToken) authentication);
            throw new BadCredentialsException("Invalid credentials");
        };
        TenantAuthenticationFilter filter = new TenantAuthenticationFilter(manager);
        MockHttpServletRequest request = loginRequest(null, "001", "secret");

        assertThatThrownBy(() -> filter.attemptAuthentication(request, new MockHttpServletResponse()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
        assertThat(captured.get().getTenantId()).isNull();
    }

    @Test
    void rejectsNonPostAuthenticationRequests() {
        TenantAuthenticationFilter filter = new TenantAuthenticationFilter(authentication -> authentication);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/login");

        assertThatThrownBy(() -> filter.attemptAuthentication(request, new MockHttpServletResponse()))
                .isInstanceOf(AuthenticationServiceException.class)
                .hasMessageContaining("GET");
    }

    @Test
    void tokenExposesRealmLoginPrincipalAuthoritiesAndErasesCredentials() {
        TenantAuthenticationToken request = new TenantAuthenticationToken("tenant-a", "001", "secret");
        assertThat(request.isAuthenticated()).isFalse();
        assertThat(request.getPrincipal()).isEqualTo("001");
        assertThat(request.getTenantId()).isEqualTo("tenant-a");
        assertThat(request.getLoginName()).isEqualTo("001");
        assertThat(request.getCredentials()).isEqualTo("secret");
        request.eraseCredentials();
        assertThat(request.getCredentials()).isNull();

        TenantAuthenticationToken authenticated = new TenantAuthenticationToken(
                AuthenticationRealm.PLATFORM_REALM_ID,
                "PlatformAdmin",
                "immutable-user-id",
                List.of(new SimpleGrantedAuthority("TENANT_READ")));
        assertThat(authenticated.isAuthenticated()).isTrue();
        assertThat(authenticated.getName()).isEqualTo("immutable-user-id");
        assertThat(authenticated.getTenantId()).isEqualTo("PLATFORM");
        assertThat(authenticated.getLoginName()).isEqualTo("PlatformAdmin");
        assertThat(authenticated.getAuthorities()).extracting("authority").containsExactly("TENANT_READ");
    }

    private void assertPersistedAuthentication(
            MockHttpServletRequest request,
            String expectedPrincipal,
            String expectedTenantId,
            String expectedLoginName) {
        MockHttpSession session = (MockHttpSession) request.getSession(false);
        assertThat(session).isNotNull();
        Object stored = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(stored).isInstanceOf(SecurityContext.class);
        Authentication authentication = ((SecurityContext) stored).getAuthentication();
        assertThat(authentication).isInstanceOf(TenantAuthenticationToken.class);
        assertThat(authentication.getName()).isEqualTo(expectedPrincipal);
        TenantAuthenticationToken token = (TenantAuthenticationToken) authentication;
        assertThat(token.getTenantId()).isEqualTo(expectedTenantId);
        assertThat(token.getLoginName()).isEqualTo(expectedLoginName);
    }

    private MockHttpServletRequest loginRequest(String tenantId, String loginName, String password) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        if (tenantId != null) {
            request.addParameter(TenantAuthenticationFilter.TENANT_PARAMETER, tenantId);
        }
        request.addParameter("username", loginName);
        request.addParameter("password", password);
        return request;
    }
}

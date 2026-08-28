package com.practical.leavemaster.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class TenantAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public static final String TENANT_PARAMETER = "tenantId";

    public TenantAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
        setFilterProcessesUrl("/auth/login");
        setAuthenticationSuccessHandler((request, response, authentication) ->
                response.setStatus(HttpServletResponse.SC_OK));
        setAuthenticationFailureHandler((request, response, exception) ->
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new org.springframework.security.authentication.AuthenticationServiceException(
                    "Authentication method not supported: " + request.getMethod());
        }

        String tenantId = request.getParameter(TENANT_PARAMETER);
        String loginName = obtainUsername(request);
        String password = obtainPassword(request);
        TenantAuthenticationToken token = new TenantAuthenticationToken(
                tenantId == null ? null : tenantId.trim(),
                loginName == null ? null : loginName.trim(),
                password);
        setDetails(request, token);
        return this.getAuthenticationManager().authenticate(token);
    }
}

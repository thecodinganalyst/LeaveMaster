package com.practical.leavemaster.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TenantAuthenticationProvider implements AuthenticationProvider {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) {
        TenantAuthenticationToken request = (TenantAuthenticationToken) authentication;
        String tenantId = normalize(request.getTenantId());
        String loginName = normalize(request.getLoginName());
        String password = request.getCredentials() == null ? null : request.getCredentials().toString();

        if (tenantId == null || loginName == null || password == null || password.isBlank()) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        AppUser user = AuthenticationRealm.isPlatformRealm(tenantId)
                ? appUserRepository.findByTenantIdIsNullAndLoginName(loginName).orElse(null)
                : appUserRepository.findByTenantIdAndLoginName(tenantId, loginName).orElse(null);

        if (user == null || !user.isActive() || user.getPassword() == null
                || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .filter(role -> role != null && role.isActive())
                .flatMap(role -> role.getPermissions().stream())
                .filter(permission -> permission != null && permission.getCode() != null)
                .map(permission -> (GrantedAuthority) new SimpleGrantedAuthority(permission.getCode()))
                .collect(Collectors.toSet());

        String authenticatedRealm = user.getTenantId() == null
                ? AuthenticationRealm.PLATFORM_REALM_ID
                : user.getTenantId();
        return new TenantAuthenticationToken(
                authenticatedRealm,
                user.getLoginName(),
                user.getUserId(),
                authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TenantAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

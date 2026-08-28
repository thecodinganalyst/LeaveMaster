package com.practical.leavemaster.user;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public class TenantAuthenticationToken extends AbstractAuthenticationToken {

    private final String tenantId;
    private final String loginName;
    private final Object principal;
    private Object credentials;

    public TenantAuthenticationToken(String tenantId, String loginName, String password) {
        super(List.of());
        this.tenantId = tenantId;
        this.loginName = loginName;
        this.principal = loginName;
        this.credentials = password;
        setAuthenticated(false);
    }

    public TenantAuthenticationToken(
            String tenantId,
            String loginName,
            String userId,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.tenantId = tenantId;
        this.loginName = loginName;
        this.principal = userId;
        this.credentials = null;
        setAuthenticated(true);
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getLoginName() {
        return loginName;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        credentials = null;
    }
}

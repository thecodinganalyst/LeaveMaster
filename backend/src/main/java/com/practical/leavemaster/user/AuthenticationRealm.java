package com.practical.leavemaster.user;

public final class AuthenticationRealm {

    public static final String PLATFORM_REALM_ID = "PLATFORM";

    private AuthenticationRealm() {
    }

    public static boolean isPlatformRealm(String tenantId) {
        return tenantId != null && PLATFORM_REALM_ID.equalsIgnoreCase(tenantId.trim());
    }
}

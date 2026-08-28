package com.practical.leavemaster.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationRealmTest {

    @Test
    void recognizesPlatformRealmCaseInsensitivelyAfterTrimming() {
        assertThat(AuthenticationRealm.isPlatformRealm("PLATFORM")).isTrue();
        assertThat(AuthenticationRealm.isPlatformRealm(" platform ")).isTrue();
        assertThat(AuthenticationRealm.isPlatformRealm("tenant-a")).isFalse();
        assertThat(AuthenticationRealm.isPlatformRealm("")).isFalse();
        assertThat(AuthenticationRealm.isPlatformRealm(null)).isFalse();
    }
}

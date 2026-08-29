package com.practical.leavemaster.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void shouldSaveAndFindUserByImmutableUserId() {
        AppUser user = appUserRepository.save(AppUser.builder()
                .loginName("alice")
                .tenantId("tenant-a")
                .password("secret")
                .active(true)
                .build());

        assertThat(user.getUserId()).isNotBlank();
        Optional<AppUser> found = appUserRepository.findById(user.getUserId());
        assertThat(found).isPresent();
        assertThat(found.get().getLoginName()).isEqualTo("alice");
        assertThat(found.get().getTenantId()).isEqualTo("tenant-a");
    }

    @Test
    void shouldFindLoginNameWithinTenant() {
        appUserRepository.save(AppUser.builder()
                .loginName("001").tenantId("tenant-a").password("secret").active(true).build());
        appUserRepository.save(AppUser.builder()
                .loginName("001").tenantId("tenant-b").password("secret").active(true).build());

        assertThat(appUserRepository.existsByTenantIdAndLoginName("tenant-a", "001")).isTrue();
        assertThat(appUserRepository.existsByTenantIdAndLoginName("tenant-b", "001")).isTrue();
        assertThat(appUserRepository.findByTenantIdAndLoginName("tenant-a", "001"))
                .get().extracting(AppUser::getTenantId).isEqualTo("tenant-a");
        assertThat(appUserRepository.findByTenantIdAndLoginName("tenant-b", "001"))
                .get().extracting(AppUser::getTenantId).isEqualTo("tenant-b");
        assertThat(appUserRepository.findUniqueByLoginName("001")).isEmpty();
    }

    @Test
    void shouldFindByTenantAndStaffId() {
        AppUser user = AppUser.builder()
                .loginName("alice")
                .tenantId("tenant-a")
                .password("secret")
                .active(true)
                .staffId("S001")
                .build();
        appUserRepository.save(user);

        Optional<AppUser> found = appUserRepository.findByTenantIdAndStaffId("tenant-a", "S001");
        assertThat(found).isPresent();
        assertThat(found.get().getLoginName()).isEqualTo("alice");
    }

    @Test
    void shouldFindByOidcProviderAndOidcSubject() {
        AppUser user = AppUser.builder()
                .loginName("alice")
                .tenantId("tenant-a")
                .password("secret")
                .active(true)
                .oidcProvider("github")
                .oidcSubject("12345")
                .build();
        appUserRepository.save(user);

        Optional<AppUser> found = appUserRepository.findByOidcProviderAndOidcSubject("github", "12345");
        assertThat(found).isPresent();
        assertThat(found.get().getLoginName()).isEqualTo("alice");
    }

    @Test
    void shouldRejectSameOauthIdentityAcrossDifferentTenants() {
        appUserRepository.saveAndFlush(AppUser.builder()
                .loginName("alice")
                .tenantId("tenant-a")
                .password("secret")
                .active(true)
                .oidcProvider("github")
                .oidcSubject("same-account")
                .build());

        AppUser secondUser = AppUser.builder()
                .loginName("bob")
                .tenantId("tenant-b")
                .password("secret")
                .active(true)
                .oidcProvider("github")
                .oidcSubject("same-account")
                .build();

        assertThatThrownBy(() -> appUserRepository.saveAndFlush(secondUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowMultipleUnlinkedUsers() {
        appUserRepository.saveAndFlush(AppUser.builder()
                .loginName("alice").tenantId("tenant-a").password("secret").active(true).build());
        appUserRepository.saveAndFlush(AppUser.builder()
                .loginName("bob").tenantId("tenant-b").password("secret").active(true).build());

        assertThat(appUserRepository.findAll()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldDeleteUserByImmutableUserId() {
        AppUser user = appUserRepository.save(AppUser.builder()
                .loginName("alice").tenantId("tenant-a").password("secret").active(true).build());
        appUserRepository.deleteById(user.getUserId());
        assertThat(appUserRepository.findById(user.getUserId())).isEmpty();
    }
}

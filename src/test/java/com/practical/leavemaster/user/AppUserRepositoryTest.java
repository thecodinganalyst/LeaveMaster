package com.practical.leavemaster.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void shouldSaveAndFindUser() {
        AppUser user = AppUser.builder()
                .id("U001")
                .loginName("alice")
                .password("secret")
                .active(true)
                .build();

        appUserRepository.save(user);

        Optional<AppUser> found = appUserRepository.findById("U001");
        assertThat(found).isPresent();
        assertThat(found.get().getLoginName()).isEqualTo("alice");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void shouldFindByLoginName() {
        AppUser user = AppUser.builder()
                .id("U001")
                .loginName("alice")
                .password("secret")
                .active(true)
                .build();
        appUserRepository.save(user);

        Optional<AppUser> found = appUserRepository.findByLoginName("alice");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("U001");
    }

    @Test
    void shouldReturnTrueWhenLoginNameExists() {
        appUserRepository.save(AppUser.builder().id("U001").loginName("alice").password("secret").active(true).build());

        assertThat(appUserRepository.existsByLoginName("alice")).isTrue();
        assertThat(appUserRepository.existsByLoginName("bob")).isFalse();
    }

    @Test
    void shouldFindByStaffId() {
        AppUser user = AppUser.builder()
                .id("U001")
                .loginName("alice")
                .password("secret")
                .active(true)
                .staffId("S001")
                .build();
        appUserRepository.save(user);

        Optional<AppUser> found = appUserRepository.findByStaffId("S001");
        assertThat(found).isPresent();
        assertThat(found.get().getLoginName()).isEqualTo("alice");
    }

    @Test
    void shouldDeleteUser() {
        appUserRepository.save(AppUser.builder().id("U001").loginName("alice").password("secret").active(true).build());
        appUserRepository.deleteById("U001");
        assertThat(appUserRepository.findById("U001")).isEmpty();
    }
}

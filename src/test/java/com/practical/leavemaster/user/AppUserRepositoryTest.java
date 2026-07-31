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
                .loginName("alice")
                .password("secret")
                .active(true)
                .build();

        appUserRepository.save(user);

        Optional<AppUser> found = appUserRepository.findById("alice");
        assertThat(found).isPresent();
        assertThat(found.get().getLoginName()).isEqualTo("alice");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void shouldReturnTrueWhenLoginNameExists() {
        appUserRepository.save(AppUser.builder().loginName("alice").password("secret").active(true).build());

        assertThat(appUserRepository.existsById("alice")).isTrue();
        assertThat(appUserRepository.existsById("bob")).isFalse();
    }

    @Test
    void shouldFindByStaffId() {
        AppUser user = AppUser.builder()
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
        appUserRepository.save(AppUser.builder().loginName("alice").password("secret").active(true).build());
        appUserRepository.deleteById("alice");
        assertThat(appUserRepository.findById("alice")).isEmpty();
    }
}

package com.practical.leavemaster.user;

import com.practical.leavemaster.tenant.TenantActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserSelfServicePasswordTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TenantActivityService tenantActivityService;

    @InjectMocks
    private AppUserService appUserService;

    @Test
    void shouldChangeOwnPasswordWhenCurrentPasswordMatches() {
        AppUser user = AppUser.builder()
            .loginName("alice")
            .password("encoded-old")
            .tenantId("tenant-1")
            .active(true)
            .build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "encoded-old")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "encoded-old")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
        when(appUserRepository.save(user)).thenReturn(user);

        appUserService.changeOwnPassword("alice", "old-password", "new-password");

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        verify(appUserRepository).save(user);
        verify(tenantActivityService).touch("tenant-1");
    }

    @Test
    void shouldRejectIncorrectCurrentPassword() {
        AppUser user = AppUser.builder().loginName("alice").password("encoded-old").active(true).build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-old")).thenReturn(false);

        assertThatThrownBy(() -> appUserService.changeOwnPassword("alice", "wrong-password", "new-password"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Current password is incorrect");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldRejectPasswordShorterThanEightCharacters() {
        assertThatThrownBy(() -> appUserService.changeOwnPassword("alice", "old-password", "short"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 8 characters");

        verifyNoInteractions(appUserRepository);
    }

    @Test
    void shouldRejectReusingCurrentPassword() {
        AppUser user = AppUser.builder().loginName("alice").password("encoded-old").active(true).build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("same-password", "encoded-old")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.changeOwnPassword("alice", "same-password", "same-password"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("New password must be different from the current password");

        verify(appUserRepository, never()).save(any());
    }
}

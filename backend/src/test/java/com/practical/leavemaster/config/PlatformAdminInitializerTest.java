package com.practical.leavemaster.config;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppPermissionRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatformAdminInitializerTest {

    @Mock private AppRoleRepository appRoleRepository;
    @Mock private AppPermissionRepository appPermissionRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationArguments applicationArguments;
    @InjectMocks private PlatformAdminInitializer initializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(initializer, "platformAdminPassword", "test-password");
        ReflectionTestUtils.setField(initializer, "resetPlatformAdminPassword", false);
    }

    private AppRole platformAdminRole() {
        return AppRole.builder().id(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)
                .description("Platform administrator – manages tenants").active(true).build();
    }

    private AppPermission permission(String code) {
        return AppPermission.builder().code(code).description(code).build();
    }

    @Test
    void shouldCreatePlatformAdminRoleWhenItDoesNotExist() throws Exception {
        AppPermission tenantRead = permission(RbacPermissions.TENANT_READ);
        AppPermission tenantWrite = permission(RbacPermissions.TENANT_WRITE);
        AppRole savedRole = AppRole.builder().id(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)
                .description("Platform administrator – manages tenants").active(true)
                .permissions(Set.of(tenantRead, tenantWrite)).build();
        when(appRoleRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)).thenReturn(Optional.empty());
        when(appPermissionRepository.findAllById(anyCollection())).thenReturn(List.of(tenantRead, tenantWrite));
        when(appRoleRepository.save(any(AppRole.class))).thenReturn(savedRole);
        when(appUserRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_LOGIN_NAME)).thenReturn(Optional.empty());
        when(appUserRepository.findAll()).thenReturn(List.of());
        when(passwordEncoder.encode("test-password")).thenReturn("$2a$encoded");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        initializer.run(applicationArguments);

        ArgumentCaptor<AppRole> roleCaptor = ArgumentCaptor.forClass(AppRole.class);
        verify(appRoleRepository).save(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getId()).isEqualTo(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID);
        assertThat(roleCaptor.getValue().isActive()).isTrue();
        assertThat(roleCaptor.getValue().getPermissions())
                .extracting(AppPermission::getCode)
                .containsExactlyInAnyOrder(RbacPermissions.TENANT_READ, RbacPermissions.TENANT_WRITE);
    }

    @Test
    void shouldReconcileExistingPlatformAdminRoleWithoutRemovingExtraPermissions() throws Exception {
        AppPermission tenantRead = permission(RbacPermissions.TENANT_READ);
        AppPermission tenantWrite = permission(RbacPermissions.TENANT_WRITE);
        AppPermission extra = permission(RbacPermissions.USER_READ);
        AppRole staleRole = AppRole.builder()
                .id(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)
                .description("Platform administrator – manages tenants")
                .active(false)
                .permissions(Set.of(extra))
                .build();
        AppUser admin = AppUser.builder().loginName(PlatformAdminInitializer.PLATFORM_ADMIN_LOGIN_NAME)
                .password("old-hash").active(true).roles(Set.of(staleRole)).build();
        when(appRoleRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)).thenReturn(Optional.of(staleRole));
        when(appPermissionRepository.findAllById(anyCollection())).thenReturn(List.of(tenantRead, tenantWrite));
        when(appRoleRepository.save(staleRole)).thenReturn(staleRole);
        when(appUserRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_LOGIN_NAME)).thenReturn(Optional.of(admin));
        when(appUserRepository.findAll()).thenReturn(List.of(admin));

        initializer.run(applicationArguments);

        assertThat(staleRole.isActive()).isTrue();
        assertThat(staleRole.getPermissions())
                .extracting(AppPermission::getCode)
                .containsExactlyInAnyOrder(RbacPermissions.TENANT_READ, RbacPermissions.TENANT_WRITE, RbacPermissions.USER_READ);
        verify(appRoleRepository).save(staleRole);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldNotChangeExistingAdminPasswordByDefault() throws Exception {
        AppRole role = platformAdminRole();
        AppUser admin = AppUser.builder().loginName(PlatformAdminInitializer.PLATFORM_ADMIN_LOGIN_NAME)
                .password("old-hash").active(true).roles(Set.of(role)).build();
        when(appRoleRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)).thenReturn(Optional.of(role));
        when(appUserRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_LOGIN_NAME)).thenReturn(Optional.of(admin));
        when(appUserRepository.findAll()).thenReturn(List.of(admin));

        initializer.run(applicationArguments);

        assertThat(admin.getPassword()).isEqualTo("old-hash");
        verify(passwordEncoder, never()).encode(anyString());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldResetExistingDefaultAdminPasswordOnlyWhenExplicitlyEnabled() throws Exception {
        AppRole role = platformAdminRole();
        AppUser admin = AppUser.builder().loginName(PlatformAdminInitializer.PLATFORM_ADMIN_LOGIN_NAME)
                .password("old-hash").active(true).roles(Set.of(role)).build();
        ReflectionTestUtils.setField(initializer, "resetPlatformAdminPassword", true);
        when(appRoleRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)).thenReturn(Optional.of(role));
        when(appUserRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_LOGIN_NAME)).thenReturn(Optional.of(admin));
        when(appUserRepository.findAll()).thenReturn(List.of(admin));
        when(passwordEncoder.encode("test-password")).thenReturn("new-hash");
        when(appUserRepository.save(admin)).thenReturn(admin);

        initializer.run(applicationArguments);

        assertThat(admin.getPassword()).isEqualTo("new-hash");
        verify(passwordEncoder).encode("test-password");
        verify(appUserRepository).save(admin);
    }

    @Test
    void shouldCreatePlatformAdminUserWhenNoUsersInRole() throws Exception {
        AppRole role = platformAdminRole();
        AppUser userWithoutRole = AppUser.builder().loginName("someUser").password("$2a$encoded")
                .active(true).roles(Set.of()).build();
        when(appRoleRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)).thenReturn(Optional.of(role));
        when(appUserRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_LOGIN_NAME)).thenReturn(Optional.empty());
        when(appUserRepository.findAll()).thenReturn(List.of(userWithoutRole));
        when(passwordEncoder.encode("test-password")).thenReturn("$2a$encoded");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        initializer.run(applicationArguments);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getLoginName()).isEqualTo(PlatformAdminInitializer.PLATFORM_ADMIN_LOGIN_NAME);
        assertThat(userCaptor.getValue().getRoles()).contains(role);
    }
}

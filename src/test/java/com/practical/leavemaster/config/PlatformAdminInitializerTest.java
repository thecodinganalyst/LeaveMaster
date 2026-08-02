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

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private AppPermissionRepository appPermissionRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private PlatformAdminInitializer initializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(initializer, "platformAdminPassword", "test-password");
    }

    @Test
    void shouldCreatePlatformAdminRoleWhenItDoesNotExist() throws Exception {
        AppPermission tenantRead = AppPermission.builder().code(RbacPermissions.TENANT_READ).description("Read tenants").build();
        AppPermission tenantWrite = AppPermission.builder().code(RbacPermissions.TENANT_WRITE).description("Write tenants").build();
        AppRole savedRole = AppRole.builder()
                .id(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)
                .description("Platform administrator – manages tenants")
                .active(true)
                .permissions(Set.of(tenantRead, tenantWrite))
                .build();

        when(appRoleRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)).thenReturn(Optional.empty());
        when(appPermissionRepository.findAllById(anyCollection())).thenReturn(List.of(tenantRead, tenantWrite));
        when(appRoleRepository.save(any(AppRole.class))).thenReturn(savedRole);
        when(appUserRepository.findAll()).thenReturn(List.of());
        when(passwordEncoder.encode("test-password")).thenReturn("$2a$encoded");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        initializer.run(applicationArguments);

        ArgumentCaptor<AppRole> roleCaptor = ArgumentCaptor.forClass(AppRole.class);
        verify(appRoleRepository).save(roleCaptor.capture());
        AppRole createdRole = roleCaptor.getValue();
        assertThat(createdRole.getId()).isEqualTo(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID);
        assertThat(createdRole.isActive()).isTrue();
    }

    @Test
    void shouldNotCreatePlatformAdminRoleWhenItAlreadyExists() throws Exception {
        AppRole existingRole = AppRole.builder()
                .id(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)
                .description("Platform administrator – manages tenants")
                .active(true)
                .build();
        AppUser existingAdmin = AppUser.builder()
                .loginName(PlatformAdminInitializer.PLATFORM_ADMIN_LOGIN_NAME)
                .password("$2a$encoded")
                .active(true)
                .roles(Set.of(existingRole))
                .build();

        when(appRoleRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)).thenReturn(Optional.of(existingRole));
        when(appUserRepository.findAll()).thenReturn(List.of(existingAdmin));

        initializer.run(applicationArguments);

        verify(appRoleRepository, never()).save(any());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldCreatePlatformAdminUserWhenNoUsersInRole() throws Exception {
        AppRole existingRole = AppRole.builder()
                .id(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)
                .description("Platform administrator – manages tenants")
                .active(true)
                .build();
        AppUser userWithoutRole = AppUser.builder()
                .loginName("someUser")
                .password("$2a$encoded")
                .active(true)
                .roles(Set.of())
                .build();

        when(appRoleRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)).thenReturn(Optional.of(existingRole));
        when(appUserRepository.findAll()).thenReturn(List.of(userWithoutRole));
        when(passwordEncoder.encode("test-password")).thenReturn("$2a$encoded");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        initializer.run(applicationArguments);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        AppUser createdUser = userCaptor.getValue();
        assertThat(createdUser.getLoginName()).isEqualTo(PlatformAdminInitializer.PLATFORM_ADMIN_LOGIN_NAME);
        assertThat(createdUser.isActive()).isTrue();
        assertThat(createdUser.getRoles()).contains(existingRole);
    }

    @Test
    void shouldNotCreatePlatformAdminUserWhenUserAlreadyInRole() throws Exception {
        AppRole existingRole = AppRole.builder()
                .id(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)
                .description("Platform administrator – manages tenants")
                .active(true)
                .build();
        AppUser adminUser = AppUser.builder()
                .loginName(PlatformAdminInitializer.PLATFORM_ADMIN_LOGIN_NAME)
                .password("$2a$encoded")
                .active(true)
                .roles(Set.of(existingRole))
                .build();

        when(appRoleRepository.findById(PlatformAdminInitializer.PLATFORM_ADMIN_ROLE_ID)).thenReturn(Optional.of(existingRole));
        when(appUserRepository.findAll()).thenReturn(List.of(adminUser));

        initializer.run(applicationArguments);

        verify(appUserRepository, never()).save(any());
    }
}

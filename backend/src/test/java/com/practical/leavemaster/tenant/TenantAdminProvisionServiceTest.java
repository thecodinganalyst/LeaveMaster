package com.practical.leavemaster.tenant;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppPermissionRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class TenantAdminProvisionServiceTest {

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private AppPermissionRepository appPermissionRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TenantAdminProvisionService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tenantAdminDefaultPassword", "test-password");
    }

    @Test
    void shouldCreateTenantAdminRoleAndUserWhenNeitherExists() {
        String tenantId = "tenant1";
        String roleId = TenantAdminProvisionService.TENANT_ADMIN_ROLE_ID + "_" + tenantId;
        List<AppPermission> permissions = TenantAdminProvisionService.TENANT_ADMIN_PERMISSION_CODES.stream()
                .map(code -> AppPermission.builder().code(code).description(code).build())
                .toList();
        AppRole savedRole = AppRole.builder().id(roleId).description("desc").active(true).tenantId(tenantId).build();

        when(appRoleRepository.findById(roleId)).thenReturn(Optional.empty());
        when(appPermissionRepository.findAllById(anyCollection())).thenReturn(permissions);
        when(appRoleRepository.save(any(AppRole.class))).thenReturn(savedRole);
        when(appUserRepository.existsById(roleId)).thenReturn(false);
        when(passwordEncoder.encode("test-password")).thenReturn("$2a$encoded");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        service.provision(tenantId);

        ArgumentCaptor<AppRole> roleCaptor = ArgumentCaptor.forClass(AppRole.class);
        verify(appRoleRepository).save(roleCaptor.capture());
        AppRole createdRole = roleCaptor.getValue();
        assertThat(createdRole.getId()).isEqualTo(roleId);
        assertThat(createdRole.getTenantId()).isEqualTo(tenantId);
        assertThat(createdRole.isActive()).isTrue();
        assertThat(createdRole.getPermissions()).hasSize(TenantAdminProvisionService.TENANT_ADMIN_PERMISSION_CODES.size());

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        AppUser createdUser = userCaptor.getValue();
        assertThat(createdUser.getLoginName()).isEqualTo(roleId);
        assertThat(createdUser.getTenantId()).isEqualTo(tenantId);
        assertThat(createdUser.isActive()).isTrue();
        assertThat(createdUser.getRoles()).contains(savedRole);
    }

    @Test
    void shouldNotCreateRoleWhenItAlreadyExists() {
        String tenantId = "tenant1";
        String roleId = TenantAdminProvisionService.TENANT_ADMIN_ROLE_ID + "_" + tenantId;
        AppRole existingRole = AppRole.builder().id(roleId).description("desc").active(true).tenantId(tenantId).build();

        when(appRoleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(appUserRepository.existsById(roleId)).thenReturn(false);
        when(passwordEncoder.encode("test-password")).thenReturn("$2a$encoded");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        service.provision(tenantId);

        verify(appRoleRepository, never()).save(any());
    }

    @Test
    void shouldNotCreateUserWhenItAlreadyExists() {
        String tenantId = "tenant1";
        String roleId = TenantAdminProvisionService.TENANT_ADMIN_ROLE_ID + "_" + tenantId;
        AppRole existingRole = AppRole.builder().id(roleId).description("desc").active(true).tenantId(tenantId).build();

        when(appRoleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(appUserRepository.existsById(roleId)).thenReturn(true);

        service.provision(tenantId);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldNotIncludeTenantPermissionsInTenantAdminRole() {
        assertThat(TenantAdminProvisionService.TENANT_ADMIN_PERMISSION_CODES)
                .doesNotContain("TENANT_READ", "TENANT_WRITE");
    }

    @Test
    void shouldIncludeAllExpectedPermissionsInTenantAdminRole() {
        assertThat(TenantAdminProvisionService.TENANT_ADMIN_PERMISSION_CODES)
                .contains(
                        "USER_READ", "USER_WRITE",
                        "ROLE_MANAGE",
                        "STAFF_READ", "STAFF_WRITE",
                        "LEAVE_TYPE_READ", "LEAVE_TYPE_WRITE",
                        "LEAVE_APPROVER_READ", "LEAVE_APPROVER_WRITE",
                        "LEAVE_CALENDAR_READ", "LEAVE_CALENDAR_WRITE",
                        "LOCATION_READ", "LOCATION_WRITE",
                        "LEAVE_APPLICATION_READ", "LEAVE_APPLICATION_WRITE", "LEAVE_APPLICATION_APPROVE"
                );
    }

    @Test
    void shouldDeprovisionTenantRoles() {
        String tenantId = "tenant1";

        service.deprovision(tenantId);

        verify(appRoleRepository).deleteAllByTenantId(tenantId);
    }
}

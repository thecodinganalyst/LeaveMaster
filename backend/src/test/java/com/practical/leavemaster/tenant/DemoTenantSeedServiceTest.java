package com.practical.leavemaster.tenant;

import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoTenantSeedServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantService tenantService;
    @Mock private StaffRepository staffRepository;
    @Mock private AppRoleRepository appRoleRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveApproverRepository leaveApproverRepository;
    @Mock private LeaveApplicationRepository leaveApplicationRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private DemoTenantSeedService service;

    @BeforeEach
    void setUp() {
        service = new DemoTenantSeedService(
                tenantRepository,
                tenantService,
                staffRepository,
                appRoleRepository,
                appUserRepository,
                leaveTypeRepository,
                leaveApproverRepository,
                leaveApplicationRepository,
                passwordEncoder);
        ReflectionTestUtils.setField(service, "configuredTenantId", "DEMO");
        ReflectionTestUtils.setField(service, "demoPassword", "Demo123!");
    }

    @Test
    void configuredDemoTenantCanBeSeededFromCleanState() {
        when(tenantRepository.findById("DEMO")).thenReturn(Optional.empty());
        when(appRoleRepository.findById("DEMO_HR")).thenReturn(Optional.of(role("DEMO_HR")));
        when(appRoleRepository.findById("DEMO_Manager")).thenReturn(Optional.of(role("DEMO_Manager")));
        when(appRoleRepository.findById("DEMO_Staff")).thenReturn(Optional.of(role("DEMO_Staff")));
        when(leaveTypeRepository.findAllByTenantId("DEMO")).thenReturn(List.of(
                LeaveType.builder()
                        .id("DEMO:SG:ANNUAL_LEAVE")
                        .name("Annual Leave")
                        .tenantId("DEMO")
                        .jurisdictionId("SG")
                        .sourceJurisdictionLeaveTypeId("SG:ANNUAL_LEAVE")
                        .active(true)
                        .used(true)
                        .statutory(true)
                        .build()));
        when(passwordEncoder.encode("Demo123!")).thenReturn("encoded-demo-password");

        DemoTenantSeedService.DemoSeedResult result = service.resetConfiguredDemoTenant();

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantService).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getType()).isEqualTo(TenantType.DEMO);
        assertThat(tenantCaptor.getValue().getId()).isEqualTo("DEMO");
        assertThat(result.tenantType()).isEqualTo(TenantType.DEMO);
        assertThat(result.staffCount()).isEqualTo(4);
        assertThat(result.userCount()).isEqualTo(4);
        assertThat(result.leaveApplicationCount()).isEqualTo(4);
        verify(staffRepository).saveAll(any());
        verify(leaveApproverRepository).saveAll(any());
        verify(appUserRepository).saveAll(any());
        verify(leaveApplicationRepository).saveAll(any());
    }

    @Test
    void resetExistingDemoTenantDeletesAndRecreatesOnlyDemoTenant() {
        Tenant existing = Tenant.builder().id("DEMO").type(TenantType.DEMO).build();
        when(tenantRepository.findById("DEMO")).thenReturn(Optional.of(existing));
        when(appRoleRepository.findById("DEMO_HR")).thenReturn(Optional.of(role("DEMO_HR")));
        when(appRoleRepository.findById("DEMO_Manager")).thenReturn(Optional.of(role("DEMO_Manager")));
        when(appRoleRepository.findById("DEMO_Staff")).thenReturn(Optional.of(role("DEMO_Staff")));
        when(leaveTypeRepository.findAllByTenantId("DEMO")).thenReturn(List.of(
                LeaveType.builder().id("annual").sourceJurisdictionLeaveTypeId("SG:ANNUAL_LEAVE").build()));
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        DemoTenantSeedService.DemoSeedResult result = service.resetExistingDemoTenant("DEMO");

        verify(tenantService).delete("DEMO");
        verify(tenantService).save(any(Tenant.class));
        assertThat(result.tenantId()).isEqualTo("DEMO");
        assertThat(result.tenantType()).isEqualTo(TenantType.DEMO);
    }

    @Test
    void standardTenantCanNeverBeResetByDemoResetService() {
        Tenant standard = Tenant.builder().id("ACME").type(TenantType.STANDARD).build();
        when(tenantRepository.findById("ACME")).thenReturn(Optional.of(standard));

        assertThatThrownBy(() -> service.resetExistingDemoTenant("ACME"))
                .isInstanceOf(DemoTenantOperationException.class)
                .hasMessageContaining("non-DEMO tenant");

        verify(tenantService, never()).delete("ACME");
        verify(tenantService, never()).save(any());
    }

    @Test
    void arbitraryMissingTenantCannotBeCreatedThroughExistingResetPath() {
        when(tenantRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetExistingDemoTenant("UNKNOWN"))
                .isInstanceOf(TenantNotFoundException.class);

        verify(tenantService, never()).save(any());
    }

    private AppRole role(String id) {
        return AppRole.builder().id(id).tenantId("DEMO").active(true).build();
    }
}

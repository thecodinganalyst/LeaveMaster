package com.practical.leavemaster.tenant;

import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendarRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private LeaveApplicationRepository leaveApplicationRepository;
    @Mock private LeaveApproverRepository leaveApproverRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private LeaveEntitlementPolicyRepository leaveEntitlementPolicyRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveCalendarRepository leaveCalendarRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private TenantAdminProvisionService tenantAdminProvisionService;
    @Mock private TenantLeaveConfigurationProvisionService tenantLeaveConfigurationProvisionService;
    @Mock private JurisdictionRepository jurisdictionRepository;

    @InjectMocks private TenantService tenantService;

    @Test
    void shouldReturnAllTenants() {
        List<Tenant> tenants = List.of(
                Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).build(),
                Tenant.builder().id("t2").name("Tenant 2").startDate(LocalDate.now()).status(TenantStatus.DORMANT).build()
        );
        when(tenantRepository.findAll()).thenReturn(tenants);
        assertThat(tenantService.findAll()).hasSize(2);
    }

    @Test
    void shouldReturnTenantById() {
        Tenant tenant = Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).build();
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant));
        Optional<Tenant> result = tenantService.findById("t1");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Tenant 1");
    }

    @Test
    void shouldSaveTenantAndProvisionJurisdictionDefaults() {
        Tenant tenant = Tenant.builder().id("t1").name("Tenant 1").jurisdictionId("SG").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).build();
        when(jurisdictionRepository.existsById("SG")).thenReturn(true);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant result = tenantService.save(tenant);

        assertThat(result.getId()).isEqualTo("t1");
        assertThat(result.getLastModified()).isNotNull();
        verify(tenantLeaveConfigurationProvisionService).provision(result);
        verify(tenantAdminProvisionService).provision("t1");
    }

    @Test
    void shouldRejectTenantWithoutValidJurisdiction() {
        Tenant tenant = Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).build();
        assertThatThrownBy(() -> tenantService.save(tenant)).isInstanceOf(IllegalArgumentException.class);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void shouldUpdateTenant() {
        Tenant existing = Tenant.builder().id("t1").name("Old Name").jurisdictionId("SG").startDate(LocalDate.of(2024, 1, 1)).status(TenantStatus.ACTIVE).build();
        Tenant updated = Tenant.builder().id("t1").name("New Name").jurisdictionId("AU").startDate(LocalDate.of(2024, 1, 1)).endDate(LocalDate.of(2025, 12, 31)).status(TenantStatus.DORMANT).build();
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(existing));
        when(tenantRepository.save(existing)).thenReturn(existing);

        Tenant result = tenantService.update("t1", updated);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getStatus()).isEqualTo(TenantStatus.DORMANT);
        assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(result.getJurisdictionId()).isEqualTo("SG");
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentTenant() {
        when(tenantRepository.findById("nonexistent")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> tenantService.update("nonexistent", new Tenant())).isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void shouldMarkInactiveActiveTenantsDormant() {
        Tenant inactive = Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).lastModified(LocalDateTime.now().minusMonths(2)).build();
        when(tenantRepository.findAllByStatusAndLastModifiedBefore(eq(TenantStatus.ACTIVE), any(LocalDateTime.class))).thenReturn(List.of(inactive));
        int updatedCount = tenantService.markDormantTenants(LocalDateTime.now().minusMonths(1));
        assertThat(updatedCount).isEqualTo(1);
        assertThat(inactive.getStatus()).isEqualTo(TenantStatus.DORMANT);
        verify(tenantRepository).saveAll(List.of(inactive));
    }

    @Test
    void shouldDeleteTenantAndAllRelatedData() {
        Tenant tenant = Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).build();
        Staff staff = Staff.builder().id("s1").name("Alice").joinDate(LocalDate.now()).tenantId("t1").build();
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant));
        when(staffRepository.findAllByTenantId("t1")).thenReturn(List.of(staff));
        tenantService.delete("t1");
        verify(leaveApplicationRepository).deleteAllByTenantId("t1");
        verify(leaveApproverRepository).deleteAllByTenantId("t1");
        verify(staffRepository).deleteAll(List.of(staff));
        verify(leaveEntitlementPolicyRepository).deleteAllByTenantId("t1");
        verify(leaveTypeRepository).deleteAllByTenantId("t1");
        verify(leaveCalendarRepository).deleteAllByTenantId("t1");
        verify(appUserRepository).deleteAllByTenantId("t1");
        verify(tenantAdminProvisionService).deprovision("t1");
        verify(tenantRepository).deleteById("t1");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentTenant() {
        when(tenantRepository.findById("nonexistent")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> tenantService.delete("nonexistent")).isInstanceOf(TenantNotFoundException.class);
        verify(tenantRepository, never()).deleteById(any());
    }
}

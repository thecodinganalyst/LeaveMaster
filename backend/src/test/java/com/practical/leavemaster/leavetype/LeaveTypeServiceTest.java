package com.practical.leavemaster.leavetype;

import com.practical.leavemaster.jurisdiction.JurisdictionLeaveType;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveTypeServiceTest {

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private JurisdictionLeaveTypeRepository jurisdictionLeaveTypeRepository;

    @Mock
    private TenantActivityService tenantActivityService;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private LeaveTypeService leaveTypeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnAllLeaveTypes() {
        List<LeaveType> leaveTypes = List.of(
                LeaveType.builder().id("annual").name("Annual Leave").used(false).build(),
                LeaveType.builder().id("medical").name("Medical Leave").used(true).build()
        );
        when(leaveTypeRepository.findAll()).thenReturn(leaveTypes);

        List<LeaveType> result = leaveTypeService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldReturnOnlyCurrentTenantLeaveTypesForTenantUser() {
        authenticate("tenant-admin");
        AppUser user = AppUser.builder().loginName("tenant-admin").active(true).tenantId("TENANT_A").build();
        List<LeaveType> leaveTypes = List.of(
                LeaveType.builder().id("annual").name("Annual Leave").tenantId("TENANT_A").build()
        );
        when(appUserRepository.findById("tenant-admin")).thenReturn(Optional.of(user));
        when(leaveTypeRepository.findAllByTenantId("TENANT_A")).thenReturn(leaveTypes);

        List<LeaveType> result = leaveTypeService.findAll();

        assertThat(result).containsExactlyElementsOf(leaveTypes);
        verify(leaveTypeRepository).findAllByTenantId("TENANT_A");
        verify(leaveTypeRepository, never()).findAll();
    }

    @Test
    void shouldExposeDerivedJurisdictionForTenantLeaveTypesWithoutChangingCatalogueAuthorization() {
        authenticate("tenant-admin");
        AppUser user = AppUser.builder().loginName("tenant-admin").active(true).tenantId("TENANT_A").build();
        LeaveType leaveType = LeaveType.builder()
                .id("annual")
                .name("Annual Leave")
                .tenantId("TENANT_A")
                .sourceJurisdictionLeaveTypeId("SG:ANNUAL_LEAVE")
                .build();
        JurisdictionLeaveType source = JurisdictionLeaveType.builder()
                .id("SG:ANNUAL_LEAVE")
                .jurisdictionId("SG")
                .build();
        when(appUserRepository.findById("tenant-admin")).thenReturn(Optional.of(user));
        when(leaveTypeRepository.findAllByTenantId("TENANT_A")).thenReturn(List.of(leaveType));
        when(jurisdictionLeaveTypeRepository.findAllById(Set.of("SG:ANNUAL_LEAVE"))).thenReturn(List.of(source));

        List<LeaveType> result = leaveTypeService.findAll();

        assertThat(result).singleElement().extracting(LeaveType::getJurisdictionId).isEqualTo("SG");
        verify(jurisdictionLeaveTypeRepository).findAllById(Set.of("SG:ANNUAL_LEAVE"));
    }

    @Test
    void shouldReturnNoLeaveTypesWhenTenantUserHasNoTenantId() {
        authenticate("tenant-admin");
        AppUser user = AppUser.builder().loginName("tenant-admin").active(true).tenantId(" ").build();
        when(appUserRepository.findById("tenant-admin")).thenReturn(Optional.of(user));

        assertThat(leaveTypeService.findAll()).isEmpty();
        verifyNoInteractions(leaveTypeRepository);
        verifyNoInteractions(jurisdictionLeaveTypeRepository);
    }

    @Test
    void shouldAllowPlatformAdminToReturnAllLeaveTypes() {
        authenticate("platform-admin");
        AppRole platformAdminRole = AppRole.builder().id("PLATFORM_ADMIN").active(true).build();
        AppUser user = AppUser.builder()
                .loginName("platform-admin")
                .active(true)
                .roles(Set.of(platformAdminRole))
                .build();
        when(appUserRepository.findById("platform-admin")).thenReturn(Optional.of(user));
        when(leaveTypeRepository.findAll()).thenReturn(List.of(
                LeaveType.builder().id("tenant-a-annual").tenantId("TENANT_A").build(),
                LeaveType.builder().id("tenant-b-annual").tenantId("TENANT_B").build()
        ));

        assertThat(leaveTypeService.findAll()).hasSize(2);
        verify(leaveTypeRepository).findAll();
        verify(leaveTypeRepository, never()).findAllByTenantId(anyString());
    }

    @Test
    void shouldReturnLeaveTypeById() {
        LeaveType leaveType = LeaveType.builder().id("annual").name("Annual Leave").used(false).build();
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType));

        Optional<LeaveType> result = leaveTypeService.findById("annual");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Annual Leave");
    }

    @Test
    void shouldHideOtherTenantLeaveTypeById() {
        authenticate("tenant-admin");
        AppUser user = AppUser.builder().loginName("tenant-admin").active(true).tenantId("TENANT_A").build();
        LeaveType otherTenant = LeaveType.builder().id("annual").name("Annual Leave").tenantId("TENANT_B").build();
        when(appUserRepository.findById("tenant-admin")).thenReturn(Optional.of(user));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(otherTenant));

        assertThat(leaveTypeService.findById("annual")).isEmpty();
    }

    @Test
    void shouldSaveLeaveType() {
        LeaveType leaveType = LeaveType.builder().id("annual").name("Annual Leave").used(true).build();
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveType result = leaveTypeService.save(leaveType);

        assertThat(result.getId()).isEqualTo("annual");
        assertThat(result.isUsed()).isFalse();
    }

    @Test
    void shouldApplyCurrentTenantWhenTenantUserSavesLeaveType() {
        authenticate("tenant-admin");
        AppUser user = AppUser.builder().loginName("tenant-admin").active(true).tenantId("TENANT_A").build();
        when(appUserRepository.findById("tenant-admin")).thenReturn(Optional.of(user));
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveType result = leaveTypeService.save(
                LeaveType.builder().id("annual").name("Annual Leave").tenantId("TENANT_B").used(true).build());

        assertThat(result.getTenantId()).isEqualTo("TENANT_A");
        assertThat(result.isUsed()).isFalse();
        verify(tenantActivityService).touch("TENANT_A");
    }

    @Test
    void shouldRejectSaveWhenTenantUserHasNoTenantId() {
        authenticate("tenant-admin");
        AppUser user = AppUser.builder().loginName("tenant-admin").active(true).tenantId(null).build();
        when(appUserRepository.findById("tenant-admin")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> leaveTypeService.save(LeaveType.builder().id("annual").build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenant id");
        verify(leaveTypeRepository, never()).save(any());
    }

    @Test
    void shouldUpdateTenantOwnedLeaveTypeMetadataWithoutChangingLineage() {
        LeaveType existing = LeaveType.builder()
                .id("annual").name("Annual Leave").used(false).tenantId("TENANT_A")
                .sourceJurisdictionLeaveTypeId("SG:ANNUAL_LEAVE")
                .active(true).statutory(true).paid(true).sourceName("MOM")
                .sourceUrl("https://old.example").effectiveFrom(LocalDate.of(2026, 1, 1)).build();
        LeaveType updated = LeaveType.builder()
                .id("different-id").name("Annual Leave Updated").used(true).tenantId("TENANT_B")
                .sourceJurisdictionLeaveTypeId("OTHER")
                .active(false).statutory(false).paid(false).sourceName("Tenant handbook")
                .sourceUrl("https://tenant.example/leave")
                .effectiveFrom(LocalDate.of(2026, 2, 1)).effectiveTo(LocalDate.of(2026, 12, 31)).build();
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(existing));
        when(leaveTypeRepository.save(existing)).thenReturn(existing);

        LeaveType result = leaveTypeService.update("annual", updated);

        assertThat(result.getName()).isEqualTo("Annual Leave Updated");
        assertThat(result.isActive()).isFalse();
        assertThat(result.isStatutory()).isFalse();
        assertThat(result.getPaid()).isFalse();
        assertThat(result.getSourceName()).isEqualTo("Tenant handbook");
        assertThat(result.getSourceUrl()).isEqualTo("https://tenant.example/leave");
        assertThat(result.getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(result.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(result.isUsed()).isFalse();
        assertThat(result.getTenantId()).isEqualTo("TENANT_A");
        assertThat(result.getSourceJurisdictionLeaveTypeId()).isEqualTo("SG:ANNUAL_LEAVE");
        verify(tenantActivityService).touch("TENANT_A");
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentLeaveType() {
        when(leaveTypeRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveTypeService.update("nonexistent", new LeaveType()))
                .isInstanceOf(LeaveTypeNotFoundException.class);
    }

    @Test
    void shouldDeleteLeaveTypeWhenNotInUse() {
        LeaveType leaveType = LeaveType.builder().id("annual").name("Annual Leave").used(false).build();
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType));

        leaveTypeService.delete("annual");

        verify(leaveTypeRepository).deleteById("annual");
    }

    @Test
    void shouldThrowWhenDeletingLeaveTypeInUse() {
        LeaveType leaveType = LeaveType.builder().id("medical").name("Medical Leave").used(true).build();
        when(leaveTypeRepository.findById("medical")).thenReturn(Optional.of(leaveType));

        assertThatThrownBy(() -> leaveTypeService.delete("medical"))
                .isInstanceOf(LeaveTypeInUseException.class);

        verify(leaveTypeRepository, never()).deleteById("medical");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentLeaveType() {
        when(leaveTypeRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveTypeService.delete("nonexistent"))
                .isInstanceOf(LeaveTypeNotFoundException.class);
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "n/a", List.of()));
    }
}

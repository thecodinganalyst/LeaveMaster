package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceUpdateRegressionTest {

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private LeaveCalendarService leaveCalendarService;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private LeaveApproverRepository leaveApproverRepository;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private AppUserService appUserService;

    @Mock
    private TenantActivityService tenantActivityService;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private StaffService staffService;

    @Test
    void shouldReuseManagedEntitlementWhenUpdatingEmailAndRoles() {
        LeaveType managedLeaveType = annualLeaveType();
        LeaveEntitlement managedEntitlement = entitlement("E1", managedLeaveType, "5.79");
        Staff existing = staffWithEntitlements(managedEntitlement);
        managedEntitlement.setStaff(existing);
        existing.setEmail("old@example.com");

        LeaveEntitlement submittedEntitlement = entitlement(
                "E1", LeaveType.builder().id("annual").build(), "5.79");
        Staff updated = updateRequest("new@example.com", List.of(submittedEntitlement));
        updated.setRoleIds(Set.of("Bravo_Staff"));

        when(staffRepository.findById("S001")).thenReturn(Optional.of(existing));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(managedLeaveType));
        when(staffRepository.save(existing)).thenReturn(existing);
        when(appUserService.findRoleIdsByStaffId("S001")).thenReturn(Set.of("Bravo_Staff"));

        Staff result = staffService.update("S001", updated);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getLeaveEntitlements()).containsExactly(managedEntitlement);
        assertThat(result.getLeaveEntitlements().getFirst()).isSameAs(managedEntitlement);
        assertThat(result.getLeaveEntitlements().getFirst()).isNotSameAs(submittedEntitlement);
        verify(appUserService).updateRolesByStaffId("S001", Set.of("Bravo_Staff"), null);
    }

    @Test
    void shouldUpdateFieldsOnManagedExistingEntitlementInsteadOfReplacingIt() {
        LeaveType managedLeaveType = annualLeaveType();
        LeaveEntitlement managedEntitlement = entitlement("E1", managedLeaveType, "5.79");
        Staff existing = staffWithEntitlements(managedEntitlement);
        managedEntitlement.setStaff(existing);

        LeaveEntitlement submittedEntitlement = entitlement(
                "E1", LeaveType.builder().id("annual").build(), "6.25");
        submittedEntitlement.setAdjustmentAmount(new BigDecimal("0.46"));
        Staff updated = updateRequest("alice@example.com", List.of(submittedEntitlement));

        when(staffRepository.findById("S001")).thenReturn(Optional.of(existing));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(managedLeaveType));
        when(staffRepository.save(existing)).thenReturn(existing);

        Staff result = staffService.update("S001", updated);

        LeaveEntitlement resultEntitlement = result.getLeaveEntitlements().getFirst();
        assertThat(resultEntitlement).isSameAs(managedEntitlement);
        assertThat(resultEntitlement.getId()).isEqualTo("E1");
        assertThat(resultEntitlement.getEntitlement()).isEqualByComparingTo("6.25");
        assertThat(resultEntitlement.getAdjustmentAmount()).isEqualByComparingTo("0.46");
        assertThat(resultEntitlement.getLeaveType()).isSameAs(managedLeaveType);
    }

    @Test
    void shouldCreateFreshEntityForNewEntitlementWhileKeepingManagedExistingEntitlement() {
        LeaveType managedLeaveType = annualLeaveType();
        LeaveEntitlement managedEntitlement = entitlement("E1", managedLeaveType, "5.79");
        Staff existing = staffWithEntitlements(managedEntitlement);
        managedEntitlement.setStaff(existing);

        LeaveEntitlement submittedExisting = entitlement(
                "E1", LeaveType.builder().id("annual").build(), "5.79");
        LeaveEntitlement submittedNew = entitlement(
                null, LeaveType.builder().id("annual").build(), "2.00");
        Staff updated = updateRequest("alice@example.com", List.of(submittedExisting, submittedNew));

        when(staffRepository.findById("S001")).thenReturn(Optional.of(existing));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(managedLeaveType));
        when(staffRepository.save(existing)).thenReturn(existing);

        Staff result = staffService.update("S001", updated);

        assertThat(result.getLeaveEntitlements()).hasSize(2);
        assertThat(result.getLeaveEntitlements().getFirst()).isSameAs(managedEntitlement);
        LeaveEntitlement created = result.getLeaveEntitlements().get(1);
        assertThat(created).isNotSameAs(submittedNew);
        assertThat(created.getId()).isNull();
        assertThat(created.getStaff()).isSameAs(existing);
        assertThat(created.getLeaveType()).isSameAs(managedLeaveType);
    }

    @Test
    void shouldRemoveOnlyManagedEntitlementsOmittedFromSubmittedCollection() {
        LeaveType managedLeaveType = annualLeaveType();
        LeaveEntitlement kept = entitlement("E1", managedLeaveType, "5.79");
        LeaveEntitlement removed = entitlement("E2", managedLeaveType, "3.00");
        Staff existing = staffWithEntitlements(kept, removed);
        kept.setStaff(existing);
        removed.setStaff(existing);

        LeaveEntitlement submittedKept = entitlement(
                "E1", LeaveType.builder().id("annual").build(), "5.79");
        Staff updated = updateRequest("alice@example.com", List.of(submittedKept));

        when(staffRepository.findById("S001")).thenReturn(Optional.of(existing));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(managedLeaveType));
        when(staffRepository.save(existing)).thenReturn(existing);

        Staff result = staffService.update("S001", updated);

        assertThat(result.getLeaveEntitlements()).containsExactly(kept);
        assertThat(result.getLeaveEntitlements().getFirst()).isSameAs(kept);
    }

    @Test
    void shouldRejectEntitlementIdThatDoesNotBelongToStaff() {
        LeaveType managedLeaveType = annualLeaveType();
        LeaveEntitlement managedEntitlement = entitlement("E1", managedLeaveType, "5.79");
        Staff existing = staffWithEntitlements(managedEntitlement);
        managedEntitlement.setStaff(existing);

        LeaveEntitlement foreignEntitlement = entitlement(
                "FOREIGN", LeaveType.builder().id("annual").build(), "5.79");
        Staff updated = updateRequest("alice@example.com", List.of(foreignEntitlement));

        when(staffRepository.findById("S001")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> staffService.update("S001", updated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Leave entitlement does not belong to staff S001")
                .hasMessageContaining("FOREIGN");
    }

    @Test
    void shouldRejectDuplicateSubmittedEntitlementIds() {
        LeaveType managedLeaveType = annualLeaveType();
        LeaveEntitlement managedEntitlement = entitlement("E1", managedLeaveType, "5.79");
        Staff existing = staffWithEntitlements(managedEntitlement);
        managedEntitlement.setStaff(existing);

        LeaveEntitlement first = entitlement("E1", LeaveType.builder().id("annual").build(), "5.79");
        LeaveEntitlement duplicate = entitlement("E1", LeaveType.builder().id("annual").build(), "5.79");
        Staff updated = updateRequest("alice@example.com", List.of(first, duplicate));

        when(staffRepository.findById("S001")).thenReturn(Optional.of(existing));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(managedLeaveType));

        assertThatThrownBy(() -> staffService.update("S001", updated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate leave entitlement ID: E1");
    }

    private Staff staffWithEntitlements(LeaveEntitlement... entitlements) {
        return Staff.builder()
                .id("S001")
                .name("Alice")
                .email("alice@example.com")
                .joinDate(LocalDate.of(2026, 8, 3))
                .leaveEntitlements(new ArrayList<>(List.of(entitlements)))
                .build();
    }

    private Staff updateRequest(String email, List<LeaveEntitlement> entitlements) {
        return Staff.builder()
                .id("S001")
                .name("Alice")
                .email(email)
                .joinDate(LocalDate.of(2026, 8, 3))
                .leaveEntitlements(entitlements)
                .build();
    }

    private LeaveType annualLeaveType() {
        return LeaveType.builder()
                .id("annual")
                .name("Annual Leave")
                .used(true)
                .build();
    }

    private LeaveEntitlement entitlement(String id, LeaveType leaveType, String amount) {
        return LeaveEntitlement.builder()
                .id(id)
                .leaveType(leaveType)
                .from(LocalDate.of(2026, 1, 1))
                .to(LocalDate.of(2026, 12, 31))
                .entitlement(new BigDecimal(amount))
                .carriedForwardAmount(BigDecimal.ZERO)
                .adjustmentAmount(BigDecimal.ZERO)
                .build();
    }
}

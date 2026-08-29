package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveapprover.LeaveApprover;
import com.practical.leavemaster.leaveapprover.LeaveApproverRequest;
import com.practical.leavemaster.leaveapprover.LeaveApproverService;
import com.practical.leavemaster.leaveeligibility.LeaveEligibilityFactService;
import com.practical.leavemaster.leaveeligibility.StaffDependantWriteRequest;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffWriteServiceTest {

    @Mock private StaffService staffService;
    @Mock private LeaveApproverService leaveApproverService;
    @Mock private LeaveEligibilityFactService leaveEligibilityFactService;
    @Mock private StaffEntitlementProposalService entitlementProposalService;

    @InjectMocks private StaffWriteService staffWriteService;

    @Test
    void shouldCreateMultipleLeaveApproversWithStaff() {
        Staff saved = Staff.builder().id("S001").name("Alice").build();
        stubEmptyProposal();
        when(staffService.save(any(Staff.class))).thenReturn(saved);
        when(leaveApproverService.findByStaffId("S001")).thenReturn(List.of());
        when(leaveApproverService.create(any(LeaveApproverRequest.class)))
                .thenReturn(LeaveApprover.builder().id("LA1").build(), LeaveApprover.builder().id("LA2").build());

        StaffWriteRequest request = request(List.of(
                input(null, "M001", "2026-08-27", null),
                input(null, "M002", "2027-01-01", null)));

        Staff result = staffWriteService.create(request);

        assertThat(result.getId()).isEqualTo("S001");
        ArgumentCaptor<LeaveApproverRequest> captor = ArgumentCaptor.forClass(LeaveApproverRequest.class);
        verify(leaveApproverService, times(2)).create(captor.capture());
        assertThat(captor.getAllValues()).extracting(LeaveApproverRequest::getStaffId).containsOnly("S001");
        assertThat(captor.getAllValues()).extracting(LeaveApproverRequest::getApproverId).containsExactly("M001", "M002");
    }

    @Test
    void shouldReplaceClientEntitlementsWithAuthoritativeProposalAndCreateDependants() {
        LeaveEntitlement authoritative = LeaveEntitlement.builder().entitlement(BigDecimal.valueOf(14)).build();
        when(entitlementProposalService.analyze(any())).thenReturn(new StaffEntitlementProposalAnalysis(
                List.of(authoritative), StaffEntitlementProposalAnalysis.Status.AVAILABLE));
        Staff saved = Staff.builder().id("S001").name("Alice").build();
        when(staffService.save(any(Staff.class))).thenReturn(saved);

        StaffDependantWriteRequest dependant = new StaffDependantWriteRequest(
                "Child", "CHILD", LocalDate.of(2022, 1, 1), "SG", null, null, null, null, true);
        StaffWriteRequest request = new StaffWriteRequest(
                "S001", "Alice", "alice@example.com", LocalDate.of(2026, 8, 27), List.of(), null,
                "SG", List.of(), "alice", null, EmploymentType.FULL_TIME, null, List.of(dependant));

        staffWriteService.create(request);

        ArgumentCaptor<Staff> staffCaptor = ArgumentCaptor.forClass(Staff.class);
        verify(staffService).save(staffCaptor.capture());
        assertThat(staffCaptor.getValue().getLeaveEntitlements()).containsExactly(authoritative);
        verify(leaveEligibilityFactService).createDependant("S001", dependant);
    }

    @Test
    void shouldSynchronizeExistingNewAndRemovedApproversOnUpdate() {
        Staff saved = Staff.builder().id("S001").name("Alice").build();
        LeaveApprover retained = LeaveApprover.builder().id("LA1").build();
        LeaveApprover removed = LeaveApprover.builder().id("LA2").build();
        when(staffService.update(eq("S001"), any(Staff.class))).thenReturn(saved);
        when(leaveApproverService.findByStaffId("S001")).thenReturn(List.of(retained, removed));
        when(leaveApproverService.create(any())).thenReturn(LeaveApprover.builder().id("LA3").build());

        StaffWriteRequest request = request(List.of(
                input("LA1", "M001", "2026-01-01", "2026-12-31"),
                input(null, "M002", "2027-01-01", null)));

        staffWriteService.update("S001", request);

        verify(leaveApproverService).update(eq("LA1"), any(LeaveApproverRequest.class));
        verify(leaveApproverService).create(any(LeaveApproverRequest.class));
        verify(leaveApproverService).delete("LA2");
        verify(leaveApproverService, never()).delete("LA1");
    }

    @Test
    void shouldRejectApproverRecordOwnedByAnotherStaffMember() {
        Staff saved = Staff.builder().id("S001").name("Alice").build();
        when(staffService.update(eq("S001"), any(Staff.class))).thenReturn(saved);
        when(leaveApproverService.findByStaffId("S001")).thenReturn(List.of(LeaveApprover.builder().id("LA1").build()));

        StaffWriteRequest request = request(List.of(input("OTHER", "M001", "2026-08-27", null)));

        assertThatThrownBy(() -> staffWriteService.update("S001", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to the staff member");
        verify(leaveApproverService, never()).update(anyString(), any());
    }

    @Test
    void shouldLeaveApproversUnchangedWhenFieldIsOmitted() {
        Staff saved = Staff.builder().id("S001").name("Alice").build();
        when(staffService.update(eq("S001"), any(Staff.class))).thenReturn(saved);

        staffWriteService.update("S001", request(null));

        verifyNoInteractions(leaveApproverService);
    }

    private void stubEmptyProposal() {
        when(entitlementProposalService.analyze(any())).thenReturn(new StaffEntitlementProposalAnalysis(
                List.of(), StaffEntitlementProposalAnalysis.Status.NO_TEMPLATE));
    }

    private static StaffWriteRequest request(List<StaffWriteRequest.LeaveApproverInput> approvers) {
        return new StaffWriteRequest(
                "S001", "Alice", "alice@example.com", LocalDate.of(2026, 8, 27), List.of(), null,
                "SG", null, "alice", null, null, approvers);
    }

    private static StaffWriteRequest.LeaveApproverInput input(String id, String approverId, String from, String to) {
        return new StaffWriteRequest.LeaveApproverInput(id, approverId, LocalDate.parse(from), to == null ? null : LocalDate.parse(to));
    }
}

package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyResolutionService;
import com.practical.leavemaster.leaveentitlementpolicy.LeavePolicyModel;
import com.practical.leavemaster.leaveentitlementpolicy.PolicyResolutionResult;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveApplicationPolicyMetadataServiceTest {

    @Mock StaffRepository staffRepository;
    @Mock LeaveTypeRepository leaveTypeRepository;
    @Mock LeaveEntitlementPolicyRepository policyRepository;
    @Mock LeaveEntitlementPolicyResolutionService resolutionService;

    @InjectMocks LeaveApplicationPolicyMetadataService service;

    @Test
    void resolvesVerificationRequiredEventPolicy() {
        Staff staff = Staff.builder().id("S1").tenantId("T1").build();
        LeaveType leaveType = LeaveType.builder().id("MAT").tenantId("T1").build();
        PolicyResolutionResult resolution = mock(PolicyResolutionResult.class);
        when(staffRepository.findById("S1")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("MAT")).thenReturn(Optional.of(leaveType));
        when(resolutionService.resolve(staff, "MAT", LocalDate.of(2026, 8, 28))).thenReturn(resolution);
        when(resolution.selectedPolicyId()).thenReturn("P1");
        when(policyRepository.findById("P1")).thenReturn(Optional.of(LeaveEntitlementPolicy.builder()
                .id("P1")
                .policyModel(LeavePolicyModel.EVENT_BASED)
                .eventRequiresVerification(true)
                .build()));

        LeaveApplicationPolicyMetadata metadata = service.resolve("S1", "MAT", LocalDate.of(2026, 8, 28));

        assertThat(metadata.policyModel()).isEqualTo(LeavePolicyModel.EVENT_BASED);
        assertThat(metadata.eventBased()).isTrue();
        assertThat(metadata.eventRequiresVerification()).isTrue();
    }

    @Test
    void reportsNonEventPolicyWithoutVerification() {
        Staff staff = Staff.builder().id("S1").tenantId("T1").build();
        LeaveType leaveType = LeaveType.builder().id("AL").tenantId("T1").build();
        PolicyResolutionResult resolution = mock(PolicyResolutionResult.class);
        when(staffRepository.findById("S1")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("AL")).thenReturn(Optional.of(leaveType));
        when(resolutionService.resolve(staff, "AL", null)).thenReturn(resolution);
        when(resolution.selectedPolicyId()).thenReturn("P2");
        when(policyRepository.findById("P2")).thenReturn(Optional.of(LeaveEntitlementPolicy.builder()
                .id("P2")
                .policyModel(LeavePolicyModel.ANNUAL_ENTITLEMENT)
                .eventRequiresVerification(true)
                .build()));

        LeaveApplicationPolicyMetadata metadata = service.resolve("S1", "AL", null);

        assertThat(metadata.eventBased()).isFalse();
        assertThat(metadata.eventRequiresVerification()).isFalse();
    }

    @Test
    void requiresAttachmentForVerificationRequiredEventPolicy() {
        Staff staff = Staff.builder().id("S1").tenantId("T1").build();
        LeaveType leaveType = LeaveType.builder().id("MAT").tenantId("T1").build();
        PolicyResolutionResult resolution = mock(PolicyResolutionResult.class);
        LocalDate eventDate = LocalDate.of(2026, 8, 28);
        when(staffRepository.findById("S1")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("MAT")).thenReturn(Optional.of(leaveType));
        when(resolutionService.resolve(staff, "MAT", eventDate)).thenReturn(resolution);
        when(resolution.selectedPolicyId()).thenReturn("P1");
        when(policyRepository.findById("P1")).thenReturn(Optional.of(LeaveEntitlementPolicy.builder()
                .id("P1")
                .policyModel(LeavePolicyModel.EVENT_BASED)
                .eventRequiresVerification(true)
                .build()));
        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .staffId("S1")
                .leaveTypeId("MAT")
                .fromDate(eventDate)
                .eventDate(eventDate)
                .build();

        assertThatThrownBy(() -> service.validateAttachmentRequirement(request, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Attachment is required");
        service.validateAttachmentRequirement(request, true);
    }
}

package com.practical.leavemaster.leaveentitlement;

import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyResolutionService;
import com.practical.leavemaster.leaveentitlementpolicy.LeavePolicyModel;
import com.practical.leavemaster.leaveentitlementpolicy.PolicyResolutionResult;
import com.practical.leavemaster.leaveentitlementpolicy.ProrationMethod;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeavePolicyModelGenerationTest {
    @Mock private StaffRepository staffRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveEntitlementRepository entitlementRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyResolutionService resolutionService;
    @Mock private LeaveApplicationRepository applicationRepository;
    @Mock private AppUserRepository appUserRepository;

    @Test
    void existingPoliciesDefaultToAnnualEntitlementModel() {
        LeaveEntitlementPolicy policy = LeaveEntitlementPolicy.builder().build();

        assertThat(policy.getPolicyModel()).isEqualTo(LeavePolicyModel.ANNUAL_ENTITLEMENT);
    }

    @Test
    void eventBasedPolicyDoesNotGenerateAnnualBalance() {
        LeaveEntitlementGenerationService service = new LeaveEntitlementGenerationService(
                staffRepository, leaveTypeRepository, entitlementRepository, policyRepository,
                resolutionService, applicationRepository, appUserRepository);
        LocalDate start = LocalDate.of(2027, 1, 1);
        LocalDate end = LocalDate.of(2027, 12, 31);
        Staff staff = Staff.builder().id("staff-1").name("Staff One").tenantId("tenant-a")
                .joinDate(LocalDate.of(2026, 1, 1)).leaveEntitlements(new ArrayList<>()).build();
        LeaveType leaveType = LeaveType.builder().id("event-leave").name("Event Leave").tenantId("tenant-a").build();
        LeaveEntitlementPolicy policy = LeaveEntitlementPolicy.builder()
                .id("event-policy")
                .tenantId("tenant-a")
                .leaveTypeId("event-leave")
                .name("Generic event leave")
                .active(true)
                .priority(10)
                .policyModel(LeavePolicyModel.EVENT_BASED)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(BigDecimal.ZERO)
                .accrualMethod(AccrualMethod.NONE)
                .prorationMethod(ProrationMethod.NONE)
                .effectiveFrom(start)
                .build();

        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(leaveType));
        when(resolutionService.resolve("staff-1", "event-leave", start))
                .thenReturn(new PolicyResolutionResult("staff-1", "event-leave", "event-policy", false, "selected", List.of()));
        when(policyRepository.findById("event-policy")).thenReturn(Optional.of(policy));

        EntitlementGenerationResult result = service.generateForStaff("staff-1", start, end).getFirst();

        assertThat(result.status()).isEqualTo(EntitlementGenerationResult.Status.EVENT_BASED_NO_ANNUAL_BALANCE);
        assertThat(result.policyId()).isEqualTo("event-policy");
        assertThat(result.entitlementAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(staff.getLeaveEntitlements()).isEmpty();
        verify(entitlementRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}

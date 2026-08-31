package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.ProrationMethod;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StaffAssistantReadServiceUnitTest {

    @Test
    void shouldExposeEntitlementCalculationContextWithoutJpaEntities() {
        StaffRepository repository = mock(StaffRepository.class);
        LeaveEntitlementPolicyRepository policyRepository = mock(LeaveEntitlementPolicyRepository.class);
        Staff staff = staffWithAnnualEntitlement(LocalDate.of(2026, 8, 15), new BigDecimal("5.21"));
        when(repository.findById("001")).thenReturn(Optional.of(staff));

        StaffAssistantReadService.StaffResult result = new StaffAssistantReadService(repository, policyRepository)
                .findById("001")
                .orElseThrow();

        assertThat(result.leaveEntitlements()).singleElement().satisfies(entitlement -> {
            assertThat(entitlement.leaveTypeId()).isEqualTo("annual");
            assertThat(entitlement.leaveTypeName()).isEqualTo("Annual Leave");
            assertThat(entitlement.entitlement()).isEqualByComparingTo("5.21");
            assertThat(entitlement.baseEntitlementAmount()).isEqualByComparingTo("5.21");
            assertThat(entitlement.policyId()).isEqualTo("annual-policy");
        });
    }

    @Test
    void shouldReturnOnlyRequestedEntitlementEvidenceForYear() {
        StaffRepository repository = mock(StaffRepository.class);
        LeaveEntitlementPolicyRepository policyRepository = mock(LeaveEntitlementPolicyRepository.class);
        Staff staff = staffWithAnnualEntitlement(LocalDate.of(2026, 8, 15), new BigDecimal("5.50"));
        when(repository.findById("001")).thenReturn(Optional.of(staff));
        when(policyRepository.findById("annual-policy")).thenReturn(Optional.of(annualPolicy()));

        StaffAssistantReadService.StaffLeaveEntitlementResult result =
                new StaffAssistantReadService(repository, policyRepository)
                        .findLeaveEntitlement("001", "Annual Leave", 2026)
                        .orElseThrow();

        assertThat(result.staffName()).isEqualTo("Alice");
        assertThat(result.joinDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(result.jurisdictionId()).isEqualTo("SG");
        assertThat(result.leaveTypeName()).isEqualTo("Annual Leave");
        assertThat(result.entitlement()).isEqualByComparingTo("5.50");
        assertThat(result.baseEntitlementAmount()).isEqualByComparingTo("5.50");
        assertThat(result.sourcePolicyName()).isEqualTo("Singapore Annual Leave - less than 2 years service");
        assertThat(result.configuredEntitlementAmount()).isEqualByComparingTo("14.00");
        assertThat(result.entitlementUnit()).isEqualTo("DAYS");
        assertThat(result.accrualMethod()).isEqualTo("NONE");
        assertThat(result.prorationMethod()).isEqualTo("CALENDAR_DAYS");
        assertThat(result.sourcePolicyResolved()).isTrue();
    }

    @Test
    void shouldExposeDeterministicProrationEvidenceForSecondFebruaryJoiner() {
        StaffRepository repository = mock(StaffRepository.class);
        LeaveEntitlementPolicyRepository policyRepository = mock(LeaveEntitlementPolicyRepository.class);
        Staff staff = staffWithAnnualEntitlement(LocalDate.of(2026, 2, 2), new BigDecimal("13.00"));
        when(repository.findById("001")).thenReturn(Optional.of(staff));
        when(policyRepository.findById("annual-policy")).thenReturn(Optional.of(annualPolicy()));

        StaffAssistantReadService.StaffLeaveEntitlementResult result =
                new StaffAssistantReadService(repository, policyRepository)
                        .findLeaveEntitlement("001", "Annual Leave", 2026)
                        .orElseThrow();

        assertThat(result.configuredEntitlementAmount()).isEqualByComparingTo("14.00");
        assertThat(result.prorationEligibleUnits()).isEqualTo(333L);
        assertThat(result.prorationPeriodUnits()).isEqualTo(365L);
        assertThat(result.rawProratedAmount()).isEqualByComparingTo("12.77260274");
        assertThat(result.prorationDenominationDays()).isEqualByComparingTo("0.50");
        assertThat(result.prorationRoundingRule()).isEqualTo("NEAREST_HALF_DAY");
        assertThat(result.entitlement()).isEqualByComparingTo("13.00");
    }

    @Test
    void shouldMarkMissingSourcePolicyWithoutInventingPolicyContext() {
        StaffRepository repository = mock(StaffRepository.class);
        LeaveEntitlementPolicyRepository policyRepository = mock(LeaveEntitlementPolicyRepository.class);
        Staff staff = staffWithAnnualEntitlement(LocalDate.of(2026, 2, 2), new BigDecimal("13.00"));
        when(repository.findById("001")).thenReturn(Optional.of(staff));
        when(policyRepository.findById("annual-policy")).thenReturn(Optional.empty());

        StaffAssistantReadService.StaffLeaveEntitlementResult result =
                new StaffAssistantReadService(repository, policyRepository)
                        .findLeaveEntitlement("001", "Annual Leave", 2026)
                        .orElseThrow();

        assertThat(result.sourcePolicyResolved()).isFalse();
        assertThat(result.sourcePolicyName()).isNull();
        assertThat(result.configuredEntitlementAmount()).isNull();
        assertThat(result.rawProratedAmount()).isNull();
    }

    @Test
    void shouldMatchFocusedEntitlementByLeaveTypeIdAndExcludeOtherYears() {
        StaffRepository repository = mock(StaffRepository.class);
        LeaveEntitlementPolicyRepository policyRepository = mock(LeaveEntitlementPolicyRepository.class);
        Staff staff = staffWithAnnualEntitlement(LocalDate.of(2026, 8, 15), new BigDecimal("5.50"));
        when(repository.findById("001")).thenReturn(Optional.of(staff));
        when(policyRepository.findById("annual-policy")).thenReturn(Optional.of(annualPolicy()));
        StaffAssistantReadService service = new StaffAssistantReadService(repository, policyRepository);

        assertThat(service.findLeaveEntitlement("001", "ANNUAL", 2026)).isPresent();
        assertThat(service.findLeaveEntitlement("001", "annual", 2025)).isEmpty();
    }

    @Test
    void shouldRejectMissingFocusedEntitlementArguments() {
        StaffAssistantReadService service = new StaffAssistantReadService(
                mock(StaffRepository.class), mock(LeaveEntitlementPolicyRepository.class));

        assertThatThrownBy(() -> service.findLeaveEntitlement(" ", "Annual Leave", 2026))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("staffId is required");
        assertThatThrownBy(() -> service.findLeaveEntitlement("001", " ", 2026))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("leaveType is required");
    }

    private Staff staffWithAnnualEntitlement(LocalDate joinDate, BigDecimal entitlementAmount) {
        Staff staff = Staff.builder()
                .id("001")
                .name("Alice")
                .joinDate(joinDate)
                .jurisdictionId("SG")
                .build();
        LeaveType annual = LeaveType.builder().id("annual").name("Annual Leave").build();
        staff.setLeaveEntitlements(List.of(LeaveEntitlement.builder()
                .staff(staff)
                .leaveType(annual)
                .from(LocalDate.of(2026, 1, 1))
                .to(LocalDate.of(2026, 12, 31))
                .entitlement(entitlementAmount)
                .policyId("annual-policy")
                .baseEntitlementAmount(entitlementAmount)
                .carriedForwardAmount(BigDecimal.ZERO)
                .adjustmentAmount(BigDecimal.ZERO)
                .build()));
        return staff;
    }

    private LeaveEntitlementPolicy annualPolicy() {
        return LeaveEntitlementPolicy.builder()
                .id("annual-policy")
                .name("Singapore Annual Leave - less than 2 years service")
                .active(true)
                .entitlementAmount(new BigDecimal("14.00"))
                .entitlementUnit(EntitlementUnit.DAYS)
                .accrualMethod(AccrualMethod.NONE)
                .prorationMethod(ProrationMethod.CALENDAR_DAYS)
                .carryForwardAllowed(false)
                .build();
    }
}

package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
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
        Staff staff = staffWithAnnualEntitlement();
        when(repository.findById("001")).thenReturn(Optional.of(staff));

        StaffAssistantReadService.StaffResult result = new StaffAssistantReadService(repository)
                .findById("001")
                .orElseThrow();

        assertThat(result.leaveEntitlements()).singleElement().satisfies(entitlement -> {
            assertThat(entitlement.leaveTypeId()).isEqualTo("annual");
            assertThat(entitlement.leaveTypeName()).isEqualTo("Annual Leave");
            assertThat(entitlement.entitlement()).isEqualByComparingTo("5.21");
            assertThat(entitlement.baseEntitlementAmount()).isEqualByComparingTo("14.00");
            assertThat(entitlement.policyId()).isEqualTo("annual-policy");
        });
    }

    @Test
    void shouldReturnOnlyRequestedEntitlementEvidenceForYear() {
        StaffRepository repository = mock(StaffRepository.class);
        Staff staff = staffWithAnnualEntitlement();
        when(repository.findById("001")).thenReturn(Optional.of(staff));

        StaffAssistantReadService.StaffLeaveEntitlementResult result = new StaffAssistantReadService(repository)
                .findLeaveEntitlement("001", "Annual Leave", 2026)
                .orElseThrow();

        assertThat(result.staffName()).isEqualTo("Alice");
        assertThat(result.joinDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(result.jurisdictionId()).isEqualTo("SG");
        assertThat(result.leaveTypeName()).isEqualTo("Annual Leave");
        assertThat(result.entitlement()).isEqualByComparingTo("5.21");
        assertThat(result.baseEntitlementAmount()).isEqualByComparingTo("14.00");
    }

    @Test
    void shouldMatchFocusedEntitlementByLeaveTypeIdAndExcludeOtherYears() {
        StaffRepository repository = mock(StaffRepository.class);
        Staff staff = staffWithAnnualEntitlement();
        when(repository.findById("001")).thenReturn(Optional.of(staff));
        StaffAssistantReadService service = new StaffAssistantReadService(repository);

        assertThat(service.findLeaveEntitlement("001", "ANNUAL", 2026)).isPresent();
        assertThat(service.findLeaveEntitlement("001", "annual", 2025)).isEmpty();
    }

    @Test
    void shouldRejectMissingFocusedEntitlementArguments() {
        StaffAssistantReadService service = new StaffAssistantReadService(mock(StaffRepository.class));

        assertThatThrownBy(() -> service.findLeaveEntitlement(" ", "Annual Leave", 2026))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("staffId is required");
        assertThatThrownBy(() -> service.findLeaveEntitlement("001", " ", 2026))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("leaveType is required");
    }

    private Staff staffWithAnnualEntitlement() {
        Staff staff = Staff.builder()
                .id("001")
                .name("Alice")
                .joinDate(LocalDate.of(2026, 8, 15))
                .jurisdictionId("SG")
                .build();
        LeaveType annual = LeaveType.builder().id("annual").name("Annual Leave").build();
        staff.setLeaveEntitlements(List.of(LeaveEntitlement.builder()
                .staff(staff)
                .leaveType(annual)
                .from(LocalDate.of(2026, 8, 15))
                .to(LocalDate.of(2026, 12, 31))
                .entitlement(new BigDecimal("5.21"))
                .policyId("annual-policy")
                .baseEntitlementAmount(new BigDecimal("14.00"))
                .carriedForwardAmount(BigDecimal.ZERO)
                .adjustmentAmount(BigDecimal.ZERO)
                .build()));
        return staff;
    }
}

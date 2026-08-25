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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StaffAssistantReadServiceUnitTest {

    @Test
    void shouldExposeEntitlementCalculationContextWithoutJpaEntities() {
        StaffRepository repository = mock(StaffRepository.class);
        Staff staff = Staff.builder()
                .id("001")
                .name("Alice")
                .joinDate(LocalDate.of(2026, 8, 15))
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
}

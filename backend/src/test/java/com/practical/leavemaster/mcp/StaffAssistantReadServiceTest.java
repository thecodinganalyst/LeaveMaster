package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.staff.DaySchedule;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.staff.WorkScheduleDay;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(StaffAssistantReadService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StaffAssistantReadServiceTest {

    @Autowired
    private StaffAssistantReadService readService;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Test
    void shouldMapLazyStaffAssociationsBeforePersistenceContextCloses() {
        LeaveType annual = leaveTypeRepository.save(LeaveType.builder()
                .id("annual-338")
                .name("Annual Leave")
                .used(true)
                .active(true)
                .build());
        Staff staff = Staff.builder()
                .id("LAZY-338")
                .name("Alice")
                .email("alice-338@example.com")
                .joinDate(LocalDate.of(2026, 8, 15))
                .workSchedule(List.of(
                        WorkScheduleDay.builder()
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .daySchedule(DaySchedule.FULL)
                                .build()))
                .build();
        staff.setLeaveEntitlements(List.of(LeaveEntitlement.builder()
                .staff(staff)
                .leaveType(annual)
                .from(LocalDate.of(2026, 8, 15))
                .to(LocalDate.of(2026, 12, 31))
                .entitlement(new BigDecimal("5.21"))
                .policyId("annual-policy-338")
                .baseEntitlementAmount(new BigDecimal("14.00"))
                .carriedForwardAmount(BigDecimal.ZERO)
                .adjustmentAmount(BigDecimal.ZERO)
                .build()));
        staffRepository.save(staff);

        Staff detached = staffRepository.findById("LAZY-338").orElseThrow();
        assertThat(Hibernate.isInitialized(detached.getWorkSchedule())).isFalse();
        assertThat(Hibernate.isInitialized(detached.getLeaveEntitlements())).isFalse();

        StaffAssistantReadService.StaffResult result = readService.findById("LAZY-338").orElseThrow();

        assertThat(result.id()).isEqualTo("LAZY-338");
        assertThat(result.workSchedule()).singleElement().satisfies(day -> {
            assertThat(day.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(day.daySchedule()).isEqualTo(DaySchedule.FULL);
        });
        assertThat(result.leaveEntitlements()).singleElement().satisfies(entitlement -> {
            assertThat(entitlement.leaveTypeId()).isEqualTo("annual-338");
            assertThat(entitlement.leaveTypeName()).isEqualTo("Annual Leave");
            assertThat(entitlement.entitlement()).isEqualByComparingTo("5.21");
            assertThat(entitlement.baseEntitlementAmount()).isEqualByComparingTo("14.00");
            assertThat(entitlement.policyId()).isEqualTo("annual-policy-338");
        });
    }

    @Test
    void shouldReturnSafeDtosForAllStaffAndEmptyForUnknownId() {
        staffRepository.save(Staff.builder()
                .id("ALL-338")
                .name("Bob")
                .joinDate(LocalDate.of(2025, 1, 1))
                .build());

        assertThat(readService.findAll()).singleElement()
                .extracting(StaffAssistantReadService.StaffResult::id)
                .isEqualTo("ALL-338");
        assertThat(readService.findById("missing-338")).isEmpty();
    }
}

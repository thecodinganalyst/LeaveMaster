package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlementRepository;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(LeaveBalanceAssistantReadService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LeaveBalanceAssistantReadServiceTest {

    @Autowired
    private LeaveBalanceAssistantReadService readService;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private LeaveEntitlementRepository leaveEntitlementRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @AfterEach
    void cleanUp() {
        leaveEntitlementRepository.deleteAll();
        staffRepository.deleteAll();
        leaveTypeRepository.deleteAll();
    }

    @Test
    void shouldReadLazyEntitlementsWithinTransactionAndReturnSafeDto() {
        LeaveType annualLeave = leaveTypeRepository.save(LeaveType.builder()
                .id("annual-366")
                .name("Annual Leave")
                .active(true)
                .used(true)
                .statutory(true)
                .build());
        Staff staff = staffRepository.save(Staff.builder()
                .id("001-366")
                .name("Lazy Staff")
                .joinDate(LocalDate.of(2026, 8, 1))
                .build());
        leaveEntitlementRepository.save(LeaveEntitlement.builder()
                .staff(staff)
                .leaveType(annualLeave)
                .from(LocalDate.of(2026, 1, 1))
                .to(LocalDate.of(2026, 12, 31))
                .entitlement(new BigDecimal("5.79"))
                .baseEntitlementAmount(new BigDecimal("14.00"))
                .carriedForwardAmount(BigDecimal.ZERO)
                .adjustmentAmount(new BigDecimal("-8.21"))
                .build());

        Staff detached = staffRepository.findById("001-366").orElseThrow();
        assertThat(Hibernate.isInitialized(detached.getLeaveEntitlements())).isFalse();

        List<LeaveBalanceAssistantReadService.LeaveBalanceResult> results = readService.findByStaffId("001-366");

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.leaveTypeId()).isEqualTo("annual-366");
            assertThat(result.leaveTypeName()).isEqualTo("Annual Leave");
            assertThat(result.from()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(result.to()).isEqualTo(LocalDate.of(2026, 12, 31));
            assertThat(result.entitlement()).isEqualByComparingTo("5.79");
            assertThat(result.used()).isEqualByComparingTo("0");
            assertThat(result.balance()).isEqualByComparingTo("5.79");
            assertThat(result.policyId()).isNull();
            assertThat(result.baseEntitlementAmount()).isEqualByComparingTo("14.00");
            assertThat(result.adjustmentAmount()).isEqualByComparingTo("-8.21");
        });
    }

    @Test
    void shouldReturnEmptyListForStaffWithoutEntitlements() {
        staffRepository.save(Staff.builder()
                .id("empty-366")
                .name("No Entitlement")
                .joinDate(LocalDate.of(2026, 8, 1))
                .build());

        assertThat(readService.findByStaffId("empty-366")).isEmpty();
    }
}

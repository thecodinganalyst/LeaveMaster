package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LeaveEntitlementPolicyRepositoryTest {

    @Autowired
    private LeaveEntitlementPolicyRepository repository;

    @Test
    void savesNewPolicyWithClientAssignedId() {
        LeaveEntitlementPolicy policy = LeaveEntitlementPolicy.builder()
                .id("SG_STANDARD_LEAVE")
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId("SG")
                .jurisdictionLeaveTypeId("SG:ANNUAL_LEAVE")
                .name("Standard Leave")
                .active(true)
                .priority(10)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(new BigDecimal("14"))
                .accrualMethod(AccrualMethod.NONE)
                .prorationMethod(ProrationMethod.NONE)
                .carryForwardAllowed(false)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();

        LeaveEntitlementPolicy saved = repository.saveAndFlush(policy);

        assertThat(saved.getId()).isEqualTo("SG_STANDARD_LEAVE");
        assertThat(repository.findById("SG_STANDARD_LEAVE")).isPresent();
    }

    @Test
    void generatesIdWhenPolicyIdIsOmitted() {
        LeaveEntitlementPolicy policy = LeaveEntitlementPolicy.builder()
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId("SG")
                .jurisdictionLeaveTypeId("SG:ANNUAL_LEAVE")
                .name("Generated ID Policy")
                .active(true)
                .priority(10)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(BigDecimal.ONE)
                .accrualMethod(AccrualMethod.NONE)
                .prorationMethod(ProrationMethod.NONE)
                .carryForwardAllowed(false)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();

        LeaveEntitlementPolicy saved = repository.saveAndFlush(policy);

        assertThat(saved.getId()).isNotBlank();
    }
}

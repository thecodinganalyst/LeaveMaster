package com.practical.leavemaster.leaveentitlementpolicy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LeaveEntitlementPolicyTest {
    @Test
    void preservesClientAssignedIdBeforeInsert() {
        LeaveEntitlementPolicy policy = LeaveEntitlementPolicy.builder().id("SG_STANDARD_LEAVE").build();

        policy.ensureId();

        assertThat(policy.getId()).isEqualTo("SG_STANDARD_LEAVE");
    }

    @Test
    void generatesUuidWhenIdIsOmitted() {
        LeaveEntitlementPolicy policy = LeaveEntitlementPolicy.builder().build();

        policy.ensureId();

        assertThat(policy.getId()).isNotBlank();
    }
}

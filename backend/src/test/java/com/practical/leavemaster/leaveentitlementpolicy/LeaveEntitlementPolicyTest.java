package com.practical.leavemaster.leaveentitlementpolicy;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Test
    void defaultsCarryForwardAllowedToFalseWhenJsonValueIsNull() throws Exception {
        LeaveEntitlementPolicy policy = new ObjectMapper().readValue(
                "{\"carryForwardAllowed\":null}",
                LeaveEntitlementPolicy.class
        );

        assertThat(policy.isCarryForwardAllowed()).isFalse();
    }

    @Test
    void preservesExplicitCarryForwardAllowedValueFromJson() throws Exception {
        LeaveEntitlementPolicy policy = new ObjectMapper().readValue(
                "{\"carryForwardAllowed\":true}",
                LeaveEntitlementPolicy.class
        );

        assertThat(policy.isCarryForwardAllowed()).isTrue();
    }
}

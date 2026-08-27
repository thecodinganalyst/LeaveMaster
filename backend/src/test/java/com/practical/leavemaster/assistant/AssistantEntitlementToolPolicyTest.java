package com.practical.leavemaster.assistant;

import com.practical.leavemaster.rbac.RbacPermissions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantEntitlementToolPolicyTest {

    @Test
    void shouldAuthorizeAndStructureEntitlementReadTools() {
        for (String tool : new String[]{
                "getEntitlementPoliciesByJurisdiction",
                "getEligibilityRulesByEntitlementPolicyId",
                "getLeaveEntitlementConfigurationByJurisdiction"
        }) {
            assertThat(AssistantToolPolicy.REQUIRED_AUTHORITY)
                    .containsEntry(tool, RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ);
            assertThat(AssistantToolPolicy.WRITE_TOOLS).doesNotContain(tool);
            assertThat(AssistantToolPolicy.STRUCTURED_RESULT_TOOLS).contains(tool);
        }
    }

    @Test
    void shouldAuthorizeAndStructureFocusedStaffEntitlementReadTool() {
        assertThat(AssistantToolPolicy.REQUIRED_AUTHORITY)
                .containsEntry("getStaffLeaveEntitlement", RbacPermissions.STAFF_READ);
        assertThat(AssistantToolPolicy.WRITE_TOOLS).doesNotContain("getStaffLeaveEntitlement");
        assertThat(AssistantToolPolicy.STRUCTURED_RESULT_TOOLS).contains("getStaffLeaveEntitlement");
    }
}

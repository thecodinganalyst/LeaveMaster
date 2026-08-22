package com.practical.leavemaster.mcp;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRule;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityService;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaveEntitlementPolicyMcpToolsTest {

    private final LeaveEntitlementPolicyService policyService = mock(LeaveEntitlementPolicyService.class);
    private final LeaveEntitlementPolicyEligibilityService eligibilityService = mock(LeaveEntitlementPolicyEligibilityService.class);
    private final LeaveEntitlementPolicyRepository policyRepository = mock(LeaveEntitlementPolicyRepository.class);
    private final LeaveEntitlementPolicyMcpTools tools = new LeaveEntitlementPolicyMcpTools(
            policyService, eligibilityService, policyRepository);

    @Test
    void shouldReturnPlatformPoliciesAndEligibilityForRequestedJurisdiction() {
        LeaveEntitlementPolicy sg = template("sg-policy", "SG");
        LeaveEntitlementPolicy au = template("au-policy", "AU");
        LeaveEntitlementPolicyEligibilityRule rule = LeaveEntitlementPolicyEligibilityRule.builder()
                .id("rule-1").policyId("sg-policy").value("3").active(true).sortOrder(1).build();
        when(policyRepository.findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE)).thenReturn(List.of(sg, au));
        when(policyService.findAll()).thenReturn(List.of(sg, au));
        when(eligibilityService.findAll("sg-policy")).thenReturn(List.of(rule));

        var result = tools.getLeaveEntitlementConfigurationByJurisdiction("SG");

        assertThat(result).singleElement().satisfies(configuration -> {
            assertThat(configuration.policy().getId()).isEqualTo("sg-policy");
            assertThat(configuration.eligibilityRules()).containsExactly(rule);
        });
    }

    @Test
    void shouldReturnOnlyTenantPoliciesCopiedFromRequestedJurisdictionTemplates() {
        LeaveEntitlementPolicy sgTemplate = template("sg-template", "SG");
        LeaveEntitlementPolicy auTemplate = template("au-template", "AU");
        LeaveEntitlementPolicy tenantSg = tenant("tenant-sg", "T1", "sg-template");
        LeaveEntitlementPolicy tenantAu = tenant("tenant-au", "T1", "au-template");
        LeaveEntitlementPolicy otherTenant = tenant("other-tenant", "T2", "sg-template");
        when(policyRepository.findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE)).thenReturn(List.of(sgTemplate, auTemplate));
        // LeaveEntitlementPolicyService is the security boundary: it supplies only policies accessible to the current user.
        when(policyService.findAll()).thenReturn(List.of(tenantSg, tenantAu));

        assertThat(tools.getEntitlementPoliciesByJurisdiction("SG"))
                .extracting(LeaveEntitlementPolicy::getId)
                .containsExactly("tenant-sg")
                .doesNotContain(otherTenant.getId());
    }

    @Test
    void shouldRejectBlankIdentifiersAndDelegateEligibilityAccessChecks() {
        assertThatThrownBy(() -> tools.getEntitlementPoliciesByJurisdiction(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jurisdictionId");
        assertThatThrownBy(() -> tools.getEligibilityRulesByEntitlementPolicyId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("policyId");
    }

    private LeaveEntitlementPolicy template(String id, String jurisdictionId) {
        return LeaveEntitlementPolicy.builder()
                .id(id)
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId(jurisdictionId)
                .name(id)
                .active(true)
                .build();
    }

    private LeaveEntitlementPolicy tenant(String id, String tenantId, String sourceTemplateId) {
        return LeaveEntitlementPolicy.builder()
                .id(id)
                .tenantId(tenantId)
                .scope(ConfigurationScope.TENANT)
                .sourceTemplateId(sourceTemplateId)
                .name(id)
                .active(true)
                .build();
    }
}

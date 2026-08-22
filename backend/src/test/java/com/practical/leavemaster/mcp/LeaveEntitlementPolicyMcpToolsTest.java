package com.practical.leavemaster.mcp;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveType;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeRepository;
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.EligibilityCriterionType;
import com.practical.leavemaster.leaveentitlementpolicy.EligibilityOperator;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRule;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityService;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyService;
import com.practical.leavemaster.leaveentitlementpolicy.ProrationMethod;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaveEntitlementPolicyMcpToolsTest {

    private final LeaveEntitlementPolicyService policyService = mock(LeaveEntitlementPolicyService.class);
    private final LeaveEntitlementPolicyEligibilityService eligibilityService = mock(LeaveEntitlementPolicyEligibilityService.class);
    private final LeaveEntitlementPolicyRepository policyRepository = mock(LeaveEntitlementPolicyRepository.class);
    private final JurisdictionLeaveTypeRepository jurisdictionLeaveTypeRepository = mock(JurisdictionLeaveTypeRepository.class);
    private final LeaveTypeRepository leaveTypeRepository = mock(LeaveTypeRepository.class);
    private final LeaveEntitlementPolicyMcpTools tools = new LeaveEntitlementPolicyMcpTools(
            policyService, eligibilityService, policyRepository, jurisdictionLeaveTypeRepository, leaveTypeRepository);

    @Test
    void shouldReturnHumanReadablePlatformPolicyAndEligibility() {
        LeaveEntitlementPolicy sg = template("sg-policy", "SG");
        sg.setName("1st year");
        sg.setJurisdictionLeaveTypeId("SG:ANNUAL_LEAVE");
        sg.setEntitlementAmount(BigDecimal.valueOf(7));
        sg.setEntitlementUnit(EntitlementUnit.DAYS);
        sg.setAccrualMethod(AccrualMethod.NONE);
        sg.setProrationMethod(ProrationMethod.MONTHS);
        sg.setCarryForwardAllowed(false);
        LeaveEntitlementPolicyEligibilityRule rule = LeaveEntitlementPolicyEligibilityRule.builder()
                .id("rule-1")
                .policyId("sg-policy")
                .criterionType(EligibilityCriterionType.SERVICE_MONTHS)
                .operator(EligibilityOperator.GREATER_THAN_OR_EQUAL)
                .value("3")
                .active(true)
                .sortOrder(1)
                .build();
        when(policyRepository.findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE)).thenReturn(List.of(sg));
        when(policyService.findAll()).thenReturn(List.of(sg));
        when(eligibilityService.findAll("sg-policy")).thenReturn(List.of(rule));
        when(jurisdictionLeaveTypeRepository.findById("SG:ANNUAL_LEAVE")).thenReturn(Optional.of(
                JurisdictionLeaveType.builder().id("SG:ANNUAL_LEAVE").jurisdictionId("SG").code("ANNUAL_LEAVE")
                        .name("Annual Leave").active(true).build()));

        var result = tools.getLeaveEntitlementConfigurationByJurisdiction("SG");

        assertThat(result).singleElement().satisfies(group -> {
            assertThat(group.leaveType()).isEqualTo("Annual Leave");
            assertThat(group.policies()).singleElement().satisfies(policy -> {
                assertThat(policy.policyName()).isEqualTo("1st year");
                assertThat(policy.eligibility()).isEqualTo("At least 3 months of service");
                assertThat(policy.entitlement()).isEqualTo("7 days");
                assertThat(policy.accrual()).isEqualTo("Granted upfront");
                assertThat(policy.proration()).isEqualTo("Prorated by completed months");
                assertThat(policy.carryForward()).isEqualTo("Unused leave cannot be carried forward");
            });
        });
    }

    @Test
    void shouldGroupProgressiveTiersByLeaveType() {
        LeaveEntitlementPolicy first = template("first", "SG");
        first.setJurisdictionLeaveTypeId("SG:ANNUAL_LEAVE");
        first.setName("1st year");
        LeaveEntitlementPolicy second = template("second", "SG");
        second.setJurisdictionLeaveTypeId("SG:ANNUAL_LEAVE");
        second.setName("2nd year");
        when(policyRepository.findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE)).thenReturn(List.of(first, second));
        when(policyService.findAll()).thenReturn(List.of(first, second));
        when(eligibilityService.findAll("first")).thenReturn(List.of());
        when(eligibilityService.findAll("second")).thenReturn(List.of());
        when(jurisdictionLeaveTypeRepository.findById("SG:ANNUAL_LEAVE")).thenReturn(Optional.of(
                JurisdictionLeaveType.builder().id("SG:ANNUAL_LEAVE").jurisdictionId("SG").code("ANNUAL_LEAVE")
                        .name("Annual Leave").active(true).build()));

        assertThat(tools.getLeaveEntitlementConfigurationByJurisdiction("SG"))
                .singleElement()
                .satisfies(group -> assertThat(group.policies())
                        .extracting(LeaveEntitlementPolicyMcpTools.EntitlementPolicySummary::policyName)
                        .containsExactly("1st year", "2nd year"));
    }

    @Test
    void shouldUseTenantLeaveTypeNameAndKeepTenantIsolation() {
        LeaveEntitlementPolicy sgTemplate = template("sg-template", "SG");
        LeaveEntitlementPolicy auTemplate = template("au-template", "AU");
        LeaveEntitlementPolicy tenantSg = tenant("tenant-sg", "T1", "sg-template");
        tenantSg.setLeaveTypeId("T1:ANNUAL");
        LeaveEntitlementPolicy tenantAu = tenant("tenant-au", "T1", "au-template");
        when(policyRepository.findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE)).thenReturn(List.of(sgTemplate, auTemplate));
        when(policyService.findAll()).thenReturn(List.of(tenantSg, tenantAu));
        when(eligibilityService.findAll("tenant-sg")).thenReturn(List.of());
        when(leaveTypeRepository.findById("T1:ANNUAL")).thenReturn(Optional.of(
                LeaveType.builder().id("T1:ANNUAL").name("Annual Leave").tenantId("T1").build()));

        assertThat(tools.getEntitlementPoliciesByJurisdiction("SG"))
                .extracting(LeaveEntitlementPolicy::getId)
                .containsExactly("tenant-sg");
        assertThat(tools.getLeaveEntitlementConfigurationByJurisdiction("SG"))
                .extracting(LeaveEntitlementPolicyMcpTools.LeaveTypeEntitlementSummary::leaveType)
                .containsExactly("Annual Leave");
    }

    @Test
    void shouldRenderJurisdictionAndCarryForwardRulesInBusinessLanguage() {
        LeaveEntitlementPolicy policy = template("childcare", "SG");
        policy.setJurisdictionLeaveTypeId("SG:CHILDCARE");
        policy.setEntitlementAmount(BigDecimal.valueOf(6));
        policy.setEntitlementUnit(EntitlementUnit.DAYS);
        policy.setAccrualMethod(AccrualMethod.MONTHLY);
        policy.setProrationMethod(ProrationMethod.CALENDAR_DAYS);
        policy.setCarryForwardAllowed(true);
        policy.setCarryForwardLimit(BigDecimal.valueOf(3));
        policy.setCarryForwardExpiryMonths(12);
        LeaveEntitlementPolicyEligibilityRule jurisdictionRule = LeaveEntitlementPolicyEligibilityRule.builder()
                .criterionType(EligibilityCriterionType.JURISDICTION_CODE)
                .operator(EligibilityOperator.IN)
                .value("SG")
                .active(true)
                .build();
        when(policyRepository.findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE)).thenReturn(List.of(policy));
        when(policyService.findAll()).thenReturn(List.of(policy));
        when(eligibilityService.findAll("childcare")).thenReturn(List.of(jurisdictionRule));
        when(jurisdictionLeaveTypeRepository.findById("SG:CHILDCARE")).thenReturn(Optional.empty());

        var summary = tools.getLeaveEntitlementConfigurationByJurisdiction("SG").getFirst().policies().getFirst();
        assertThat(summary.eligibility()).isEqualTo("Jurisdiction is one of: SG");
        assertThat(summary.accrual()).isEqualTo("Accrued monthly");
        assertThat(summary.proration()).isEqualTo("Prorated by calendar days");
        assertThat(summary.carryForward()).isEqualTo("Carry forward allowed up to 3 days; expires after 12 months");
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

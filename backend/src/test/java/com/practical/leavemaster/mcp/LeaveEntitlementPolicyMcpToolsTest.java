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
    void shouldReturnExactServiceRangeAndCommonBusinessSettings() {
        LeaveEntitlementPolicy sg = template("sg-policy", "SG");
        sg.setName("1st year");
        sg.setJurisdictionLeaveTypeId("SG:ANNUAL_LEAVE");
        sg.setEntitlementAmount(BigDecimal.valueOf(7));
        sg.setEntitlementUnit(EntitlementUnit.DAYS);
        sg.setAccrualMethod(AccrualMethod.NONE);
        sg.setProrationMethod(ProrationMethod.MONTHS);
        sg.setCarryForwardAllowed(false);
        var lower = serviceRule("sg-policy", EligibilityOperator.GREATER_THAN_OR_EQUAL, "3", 1);
        var upper = serviceRule("sg-policy", EligibilityOperator.LESS_THAN, "12", 2);
        when(policyRepository.findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE)).thenReturn(List.of(sg));
        when(policyService.findAll()).thenReturn(List.of(sg));
        when(eligibilityService.findAll("sg-policy")).thenReturn(List.of(lower, upper));
        when(jurisdictionLeaveTypeRepository.findById("SG:ANNUAL_LEAVE")).thenReturn(Optional.of(
                JurisdictionLeaveType.builder().id("SG:ANNUAL_LEAVE").jurisdictionId("SG").code("ANNUAL_LEAVE")
                        .name("Annual Leave").active(true).build()));

        var result = tools.getLeaveEntitlementConfigurationByJurisdiction("SG");

        assertThat(result).singleElement().satisfies(group -> {
            assertThat(group.leaveType()).isEqualTo("Annual Leave");
            assertThat(group.accrual()).isEqualTo("Granted upfront");
            assertThat(group.proration()).isEqualTo("Prorated by completed months");
            assertThat(group.carryForward()).isEqualTo("Unused leave cannot be carried forward");
            assertThat(group.policies()).singleElement().satisfies(policy -> {
                assertThat(policy.policyName()).isEqualTo("1st year");
                assertThat(policy.servicePeriod()).isEqualTo("3–11 months");
                assertThat(policy.eligibility()).isNull();
                assertThat(policy.entitlement()).isEqualTo("7 days");
                assertThat(policy.accrual()).isNull();
                assertThat(policy.proration()).isNull();
                assertThat(policy.carryForward()).isNull();
            });
        });
    }

    @Test
    void shouldDeriveInclusiveExclusiveAndOpenEndedServiceRanges() {
        LeaveEntitlementPolicy first = annualPolicy("first", "1st year", 7);
        LeaveEntitlementPolicy second = annualPolicy("second", "2nd year", 8);
        LeaveEntitlementPolicy eighth = annualPolicy("eighth", "8th year and later", 14);
        when(policyRepository.findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE)).thenReturn(List.of(first, second, eighth));
        when(policyService.findAll()).thenReturn(List.of(eighth, second, first));
        when(eligibilityService.findAll("first")).thenReturn(List.of(
                serviceRule("first", EligibilityOperator.GREATER_THAN_OR_EQUAL, "3", 1),
                serviceRule("first", EligibilityOperator.LESS_THAN, "12", 2)));
        when(eligibilityService.findAll("second")).thenReturn(List.of(
                serviceRule("second", EligibilityOperator.GREATER_THAN_OR_EQUAL, "12", 1),
                serviceRule("second", EligibilityOperator.LESS_THAN_OR_EQUAL, "23", 2)));
        when(eligibilityService.findAll("eighth")).thenReturn(List.of(
                serviceRule("eighth", EligibilityOperator.GREATER_THAN_OR_EQUAL, "84", 1)));
        when(jurisdictionLeaveTypeRepository.findById("SG:ANNUAL_LEAVE")).thenReturn(Optional.of(
                JurisdictionLeaveType.builder().id("SG:ANNUAL_LEAVE").jurisdictionId("SG").code("ANNUAL_LEAVE")
                        .name("Annual Leave").active(true).build()));

        var policies = tools.getLeaveEntitlementConfigurationByJurisdiction("SG").getFirst().policies();

        assertThat(policies)
                .extracting(LeaveEntitlementPolicyMcpTools.EntitlementPolicySummary::servicePeriod)
                .containsExactly("3–11 months", "12–23 months", "84+ months");
    }

    @Test
    void shouldDeriveEqualityAndUpperOnlyServiceRanges() {
        LeaveEntitlementPolicy exact = annualPolicy("exact", "Exact", 1);
        LeaveEntitlementPolicy upper = annualPolicy("upper", "Upper", 2);
        when(policyRepository.findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE)).thenReturn(List.of(exact, upper));
        when(policyService.findAll()).thenReturn(List.of(upper, exact));
        when(eligibilityService.findAll("exact")).thenReturn(List.of(
                serviceRule("exact", EligibilityOperator.EQUALS, "12", 1)));
        when(eligibilityService.findAll("upper")).thenReturn(List.of(
                serviceRule("upper", EligibilityOperator.LESS_THAN_OR_EQUAL, "5", 1)));
        when(jurisdictionLeaveTypeRepository.findById("SG:ANNUAL_LEAVE")).thenReturn(Optional.empty());

        var result = tools.getLeaveEntitlementConfigurationByJurisdiction("SG");

        assertThat(result).hasSize(2);
        assertThat(result.stream().flatMap(group -> group.policies().stream()).map(p -> p.servicePeriod()))
                .containsExactlyInAnyOrder("12 months", "Up to 5 months");
    }

    @Test
    void shouldOnlyShowTierSpecificSettingsWhenTheyDiffer() {
        LeaveEntitlementPolicy first = annualPolicy("first", "1st year", 7);
        LeaveEntitlementPolicy second = annualPolicy("second", "2nd year", 8);
        second.setProrationMethod(ProrationMethod.CALENDAR_DAYS);
        when(policyRepository.findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE)).thenReturn(List.of(first, second));
        when(policyService.findAll()).thenReturn(List.of(first, second));
        when(eligibilityService.findAll("first")).thenReturn(List.of());
        when(eligibilityService.findAll("second")).thenReturn(List.of());
        when(jurisdictionLeaveTypeRepository.findById("SG:ANNUAL_LEAVE")).thenReturn(Optional.of(
                JurisdictionLeaveType.builder().id("SG:ANNUAL_LEAVE").jurisdictionId("SG").code("ANNUAL_LEAVE")
                        .name("Annual Leave").active(true).build()));

        var group = tools.getLeaveEntitlementConfigurationByJurisdiction("SG").getFirst();

        assertThat(group.accrual()).isEqualTo("Granted upfront");
        assertThat(group.proration()).isNull();
        assertThat(group.carryForward()).isEqualTo("Unused leave cannot be carried forward");
        assertThat(group.policies())
                .extracting(LeaveEntitlementPolicyMcpTools.EntitlementPolicySummary::proration)
                .containsExactly("Not prorated", "Prorated by calendar days");
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
    void shouldKeepNonServiceEligibilityAndCarryForwardInBusinessLanguage() {
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

        var group = tools.getLeaveEntitlementConfigurationByJurisdiction("SG").getFirst();
        var summary = group.policies().getFirst();
        assertThat(summary.servicePeriod()).isEqualTo("All service periods");
        assertThat(summary.eligibility()).isEqualTo("Jurisdiction is one of: SG");
        assertThat(group.accrual()).isEqualTo("Accrued monthly");
        assertThat(group.proration()).isEqualTo("Prorated by calendar days");
        assertThat(group.carryForward()).isEqualTo("Carry forward allowed up to 3 days; expires after 12 months");
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

    private LeaveEntitlementPolicy annualPolicy(String id, String name, int days) {
        LeaveEntitlementPolicy policy = template(id, "SG");
        policy.setName(name);
        policy.setJurisdictionLeaveTypeId("SG:ANNUAL_LEAVE");
        policy.setEntitlementAmount(BigDecimal.valueOf(days));
        policy.setEntitlementUnit(EntitlementUnit.DAYS);
        policy.setAccrualMethod(AccrualMethod.NONE);
        policy.setProrationMethod(ProrationMethod.NONE);
        policy.setCarryForwardAllowed(false);
        return policy;
    }

    private LeaveEntitlementPolicyEligibilityRule serviceRule(String policyId, EligibilityOperator operator, String value, int sortOrder) {
        return LeaveEntitlementPolicyEligibilityRule.builder()
                .id(policyId + "-" + sortOrder)
                .policyId(policyId)
                .criterionType(EligibilityCriterionType.SERVICE_MONTHS)
                .operator(operator)
                .value(value)
                .active(true)
                .sortOrder(sortOrder)
                .build();
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

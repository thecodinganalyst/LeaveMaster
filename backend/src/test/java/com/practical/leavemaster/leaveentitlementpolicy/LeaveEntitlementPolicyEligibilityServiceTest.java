package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.Jurisdiction;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveEntitlementPolicyEligibilityServiceTest {
    @Mock private LeaveEntitlementPolicyEligibilityRepository ruleRepository;
    @Mock private LeaveEntitlementPolicyService policyService;
    @Mock private JurisdictionRepository jurisdictionRepository;
    @InjectMocks private LeaveEntitlementPolicyEligibilityService service;

    @Test
    void createsListsUpdatesAndDeletesRulesForAccessiblePolicy() {
        LeaveEntitlementPolicy policy = policy("p1", "tenant-a");
        LeaveEntitlementPolicyEligibilityRule rule = rule(EligibilityCriterionType.JURISDICTION_CODE, EligibilityOperator.EQUALS, "SG");
        when(policyService.findById("p1")).thenReturn(Optional.of(policy));
        when(jurisdictionRepository.findByCode("SG")).thenReturn(Optional.of(Jurisdiction.builder().id("SG").code("SG").build()));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveEntitlementPolicyEligibilityRule created = service.create("p1", rule);
        assertThat(created.getPolicyId()).isEqualTo("p1");
        assertThat(created.getId()).isNull();

        created.setId("r1");
        when(ruleRepository.findAllByPolicyIdOrderBySortOrderAsc("p1")).thenReturn(List.of(created));
        assertThat(service.findAll("p1")).containsExactly(created);

        when(ruleRepository.findById("r1")).thenReturn(Optional.of(created));
        LeaveEntitlementPolicyEligibilityRule update = rule(EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.GREATER_THAN_OR_EQUAL, "12");
        LeaveEntitlementPolicyEligibilityRule updated = service.update("p1", "r1", update);
        assertThat(updated.getCriterionType()).isEqualTo(EligibilityCriterionType.SERVICE_MONTHS);
        assertThat(updated.getValue()).isEqualTo("12");

        service.delete("p1", "r1");
        verify(ruleRepository).delete(created);
    }

    @Test
    void coversConvenienceCreateUpdateDeleteAndFindById() {
        LeaveEntitlementPolicy policy = policy("p1", "tenant-a");
        LeaveEntitlementPolicyEligibilityRule existing = rule(EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.EQUALS, "3");
        existing.setId("r1");
        existing.setPolicyId("p1");
        when(policyService.findById("p1")).thenReturn(Optional.of(policy));
        when(ruleRepository.findById("r1")).thenReturn(Optional.of(existing));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.findById("r1")).contains(existing);

        LeaveEntitlementPolicyEligibilityRule toCreate = rule(EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.EQUALS, "6");
        toCreate.setPolicyId("p1");
        assertThat(service.create(toCreate).getPolicyId()).isEqualTo("p1");

        LeaveEntitlementPolicyEligibilityRule requested = rule(EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.LESS_THAN, "24");
        assertThat(service.update("r1", requested).getValue()).isEqualTo("24");
        service.delete("r1");
        verify(ruleRepository).delete(existing);
    }

    @Test
    void findByIdFiltersRulesWhosePolicyIsNotAccessible() {
        LeaveEntitlementPolicyEligibilityRule existing = rule(EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.EQUALS, "3");
        existing.setId("r1");
        existing.setPolicyId("p1");
        when(ruleRepository.findById("r1")).thenReturn(Optional.of(existing));
        when(policyService.findById("p1")).thenReturn(Optional.empty());
        assertThat(service.findById("r1")).isEmpty();
    }

    @Test
    void listsRulesForAllAccessiblePolicies() {
        LeaveEntitlementPolicy firstPolicy = policy("p1", "tenant-a");
        LeaveEntitlementPolicy secondPolicy = policy("p2", "tenant-a");
        LeaveEntitlementPolicyEligibilityRule firstRule = rule(EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.GREATER_THAN_OR_EQUAL, "3");
        firstRule.setId("r1");
        firstRule.setPolicyId("p1");
        LeaveEntitlementPolicyEligibilityRule secondRule = rule(EligibilityCriterionType.JURISDICTION_CODE, EligibilityOperator.EQUALS, "SG");
        secondRule.setId("r2");
        secondRule.setPolicyId("p2");

        when(policyService.findAll()).thenReturn(List.of(firstPolicy, secondPolicy));
        when(ruleRepository.findAllByPolicyIdOrderBySortOrderAsc("p1")).thenReturn(List.of(firstRule));
        when(ruleRepository.findAllByPolicyIdOrderBySortOrderAsc("p2")).thenReturn(List.of(secondRule));

        assertThat(service.findAllAccessible()).containsExactly(firstRule, secondRule);
    }

    @Test
    void validatesJurisdictionCodesAndSupportedOperators() {
        LeaveEntitlementPolicy policy = policy("p1", "tenant-a");
        when(policyService.findById("p1")).thenReturn(Optional.of(policy));
        when(jurisdictionRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("p1", rule(EligibilityCriterionType.JURISDICTION_CODE, EligibilityOperator.GREATER_THAN, "SG")))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class)
                .hasMessageContaining("supports only");
        assertThatThrownBy(() -> service.create("p1", rule(EligibilityCriterionType.JURISDICTION_CODE, EligibilityOperator.EQUALS, "UNKNOWN")))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class)
                .hasMessageContaining("Unknown jurisdiction");
    }

    @Test
    void allowsJurisdictionEligibilityForPlatformTemplates() {
        LeaveEntitlementPolicy template = policy("template-1", null);
        template.setScope(ConfigurationScope.PLATFORM_TEMPLATE);
        template.setLeaveTypeId(null);
        template.setJurisdictionId("SG");
        template.setJurisdictionLeaveTypeId("SG:ANNUAL_LEAVE");
        when(policyService.findById("template-1")).thenReturn(Optional.of(template));
        when(jurisdictionRepository.findByCode("SG")).thenReturn(Optional.of(Jurisdiction.builder().id("SG").code("SG").build()));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveEntitlementPolicyEligibilityRule created = service.create("template-1",
                rule(EligibilityCriterionType.JURISDICTION_CODE, EligibilityOperator.EQUALS, "SG"));

        assertThat(created.getValue()).isEqualTo("SG");
    }

    @Test
    void validatesJurisdictionCodesAndServiceMonthValues() {
        LeaveEntitlementPolicy policy = policy("p1", "tenant-a");
        when(policyService.findById("p1")).thenReturn(Optional.of(policy));
        when(jurisdictionRepository.findByCode("SG")).thenReturn(Optional.of(Jurisdiction.builder().id("SG").code("SG").build()));
        when(jurisdictionRepository.findByCode("MY")).thenReturn(Optional.of(Jurisdiction.builder().id("MY").code("MY").build()));
        when(jurisdictionRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LeaveEntitlementPolicyEligibilityRule jurisdiction = service.create("p1",
                rule(EligibilityCriterionType.JURISDICTION_CODE, EligibilityOperator.IN, "SG"));
        assertThat(jurisdiction.getValue()).isEqualTo("SG");
        assertThat(service.create("p1",
                rule(EligibilityCriterionType.JURISDICTION_CODE, EligibilityOperator.NOT_IN, "SG, MY")).getValue())
                .isEqualTo("SG, MY");

        assertThatThrownBy(() -> service.create("p1",
                rule(EligibilityCriterionType.JURISDICTION_CODE, EligibilityOperator.EQUALS, "UNKNOWN")))
                .hasMessageContaining("Unknown jurisdiction");
        assertThatThrownBy(() -> service.create("p1",
                rule(EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.GREATER_THAN, "-1")))
                .hasMessageContaining("non-negative integers");
        assertThatThrownBy(() -> service.create("p1",
                rule(EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.EQUALS, "abc")))
                .hasMessageContaining("non-negative integers");
        assertThatThrownBy(() -> service.create("p1",
                rule(EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.GREATER_THAN, "12,24")))
                .hasMessageContaining("require one value");
        assertThat(service.create("p1",
                rule(EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.IN, "12,24")).getValue()).isEqualTo("12,24");
        assertThat(service.create("p1",
                rule(EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.NOT_IN, "0,3")).getValue()).isEqualTo("0,3");
    }

    @Test
    void validatesDependantMatchingRules() {
        LeaveEntitlementPolicy policy = policy("p1", "tenant-a");
        when(policyService.findById("p1")).thenReturn(Optional.of(policy));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create("p1", rule(EligibilityCriterionType.HAS_DEPENDANT_MATCHING,
                EligibilityOperator.EQUALS, "relationship=CHILD;citizenship=SG;age_lt=7")).getValue())
                .contains("relationship=CHILD");
        assertThat(service.create("p1", rule(EligibilityCriterionType.HAS_DEPENDANT_MATCHING,
                EligibilityOperator.NOT_EQUALS, "relationship=CHILD;youngest=true")).getOperator())
                .isEqualTo(EligibilityOperator.NOT_EQUALS);

        assertThatThrownBy(() -> service.create("p1", rule(EligibilityCriterionType.HAS_DEPENDANT_MATCHING,
                EligibilityOperator.IN, "relationship=CHILD")))
                .hasMessageContaining("supports only EQUALS and NOT_EQUALS");
        assertThatThrownBy(() -> service.create("p1", rule(EligibilityCriterionType.HAS_DEPENDANT_MATCHING,
                EligibilityOperator.EQUALS, "age_lt=bad")))
                .hasMessageContaining("non-negative integer");
    }

    @Test
    void rejectsMissingRuleFieldsWrongPolicyRuleAndMissingPolicyId() {
        when(policyService.findById("p1")).thenReturn(Optional.of(policy("p1", "tenant-a")));
        LeaveEntitlementPolicyEligibilityRule missing = LeaveEntitlementPolicyEligibilityRule.builder().active(true).build();
        assertThatThrownBy(() -> service.create("p1", missing)).hasMessageContaining("criterionType");

        LeaveEntitlementPolicyEligibilityRule blank = rule(EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.EQUALS, " ");
        assertThatThrownBy(() -> service.create("p1", blank)).hasMessageContaining("criterion value");

        assertThatThrownBy(() -> service.create(LeaveEntitlementPolicyEligibilityRule.builder().build()))
                .hasMessageContaining("policyId is required");

        when(ruleRepository.findById("r1")).thenReturn(Optional.of(
                LeaveEntitlementPolicyEligibilityRule.builder().id("r1").policyId("other").build()));
        assertThatThrownBy(() -> service.delete("p1", "r1")).isInstanceOf(LeaveEntitlementPolicyNotFoundException.class);
        assertThatThrownBy(() -> service.update("p1", "r1",
                rule(EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.EQUALS, "3")))
                .isInstanceOf(LeaveEntitlementPolicyNotFoundException.class);
    }

    @Test
    void reportsMissingPolicies() {
        when(policyService.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findAll("missing")).isInstanceOf(LeaveEntitlementPolicyNotFoundException.class);
    }

    private LeaveEntitlementPolicy policy(String id, String tenantId) {
        return LeaveEntitlementPolicy.builder().id(id).tenantId(tenantId).scope(ConfigurationScope.TENANT).leaveTypeId("annual").name("Policy")
                .active(true).priority(1).entitlementUnit(EntitlementUnit.DAYS).entitlementAmount(BigDecimal.TEN)
                .accrualMethod(AccrualMethod.ANNUAL).prorationMethod(ProrationMethod.MONTHS)
                .effectiveFrom(LocalDate.of(2026, 1, 1)).build();
    }

    private LeaveEntitlementPolicyEligibilityRule rule(EligibilityCriterionType type, EligibilityOperator operator, String value) {
        return LeaveEntitlementPolicyEligibilityRule.builder().criterionType(type).operator(operator).value(value)
                .active(true).sortOrder(1).build();
    }
}

package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SingaporeEntitlementPolicySeedTest {

    @Autowired
    private LeaveEntitlementPolicyRepository policyRepository;

    @Autowired
    private LeaveEntitlementPolicyEligibilityRepository eligibilityRepository;

    @Test
    void seedsSingaporeTemplatesWithPlatformScopeAndOpenEndedValidity() {
        List<LeaveEntitlementPolicy> templates = policyRepository
                .findAllByScopeAndJurisdictionIdAndActiveTrue(ConfigurationScope.PLATFORM_TEMPLATE, "SG");

        assertThat(templates).hasSize(16);
        assertThat(templates).allSatisfy(policy -> {
            assertThat(policy.getTenantId()).isNull();
            assertThat(policy.getLeaveTypeId()).isNull();
            assertThat(policy.getJurisdictionId()).isEqualTo("SG");
            assertThat(policy.getJurisdictionLeaveTypeId()).startsWith("SG:");
            assertThat(policy.getEntitlementUnit()).isEqualTo(EntitlementUnit.DAYS);
            assertThat(policy.getEffectiveFrom())
                    .as("current statutory templates must not use the software seed date")
                    .isNull();
            assertThat(policy.getEffectiveTo())
                    .as("current statutory templates remain valid until superseded")
                    .isNull();
        });
    }

    @Test
    void seedsAnnualLeaveProgressionFromSevenToFourteenDays() {
        Map<String, BigDecimal> expected = Map.of(
                "SG_ANNUAL_03_11", new BigDecimal("7.0000"),
                "SG_ANNUAL_12_23", new BigDecimal("8.0000"),
                "SG_ANNUAL_24_35", new BigDecimal("9.0000"),
                "SG_ANNUAL_36_47", new BigDecimal("10.0000"),
                "SG_ANNUAL_48_59", new BigDecimal("11.0000"),
                "SG_ANNUAL_60_71", new BigDecimal("12.0000"),
                "SG_ANNUAL_72_83", new BigDecimal("13.0000"),
                "SG_ANNUAL_84_PLUS", new BigDecimal("14.0000")
        );

        expected.forEach((id, amount) -> {
            LeaveEntitlementPolicy policy = policyRepository.findById(id).orElseThrow();
            assertThat(policy.getEntitlementAmount()).isEqualByComparingTo(amount);
            assertThat(eligibilityRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc(id))
                    .isNotEmpty()
                    .allSatisfy(rule -> assertThat(rule.getCriterionType())
                            .isEqualTo(EligibilityCriterionType.SERVICE_MONTHS));
        });

        assertThat(eligibilityRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("SG_ANNUAL_03_11"))
                .extracting(LeaveEntitlementPolicyEligibilityRule::getOperator,
                        LeaveEntitlementPolicyEligibilityRule::getValue,
                        LeaveEntitlementPolicyEligibilityRule::getSortOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(EligibilityOperator.GREATER_THAN_OR_EQUAL, "3", 10),
                        org.assertj.core.groups.Tuple.tuple(EligibilityOperator.LESS_THAN, "12", 20));
    }

    @Test
    void seedsSickAndHospitalisationServiceProgression() {
        assertEntitlements("SG_SICK_", List.of("03", "04", "05", "06_PLUS"), List.of(5, 8, 11, 14));
        assertEntitlements("SG_HOSP_", List.of("03", "04", "05", "06_PLUS"), List.of(15, 30, 45, 60));
    }

    private void assertEntitlements(String prefix, List<String> suffixes, List<Integer> amounts) {
        for (int i = 0; i < suffixes.size(); i++) {
            String id = prefix + suffixes.get(i);
            LeaveEntitlementPolicy policy = policyRepository.findById(id).orElseThrow();
            assertThat(policy.getEntitlementAmount()).isEqualByComparingTo(BigDecimal.valueOf(amounts.get(i)));
            assertThat(eligibilityRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc(id))
                    .isNotEmpty()
                    .allSatisfy(rule -> assertThat(rule.getCriterionType())
                            .isEqualTo(EligibilityCriterionType.SERVICE_MONTHS));
        }
    }
}

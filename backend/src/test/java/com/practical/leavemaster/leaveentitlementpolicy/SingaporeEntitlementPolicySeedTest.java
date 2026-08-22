package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

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

        assertThat(templates).hasSize(17);
        assertThat(templates).allSatisfy(policy -> {
            assertThat(policy.getTenantId()).isNull();
            assertThat(policy.getLeaveTypeId()).isNull();
            assertThat(policy.getJurisdictionId()).isEqualTo("SG");
            assertThat(policy.getJurisdictionLeaveTypeId()).startsWith("SG:");
            assertThat(policy.getEntitlementUnit()).isEqualTo(EntitlementUnit.DAYS);
            assertThat(policy.getEffectiveFrom())
                    .as("current templates must not use the software seed date")
                    .isNull();
            assertThat(policy.getEffectiveTo())
                    .as("current templates remain valid until superseded")
                    .isNull();
        });
    }

    @Test
    void seedsCompanyDefaultAnnualLeaveProgressionFromFourteenToTwentyFourDays() {
        Map<String, BigDecimal> expected = new LinkedHashMap<>();
        expected.put("SG_ANNUAL_00_23", new BigDecimal("14.0000"));
        expected.put("SG_ANNUAL_24_47", new BigDecimal("16.0000"));
        expected.put("SG_ANNUAL_48_71", new BigDecimal("18.0000"));
        expected.put("SG_ANNUAL_72_95", new BigDecimal("20.0000"));
        expected.put("SG_ANNUAL_96_119", new BigDecimal("22.0000"));
        expected.put("SG_ANNUAL_120_PLUS", new BigDecimal("24.0000"));

        expected.forEach((id, amount) -> {
            LeaveEntitlementPolicy policy = policyRepository.findById(id).orElseThrow();
            assertThat(policy.getEntitlementAmount()).isEqualByComparingTo(amount);
            assertThat(policy.getAccrualMethod()).isEqualTo(AccrualMethod.NONE);
            assertThat(policy.getProrationMethod()).isEqualTo(ProrationMethod.CALENDAR_DAYS);
            assertThat(policy.isCarryForwardAllowed()).isFalse();
            assertThat(eligibilityRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc(id))
                    .isNotEmpty()
                    .allSatisfy(rule -> assertThat(rule.getCriterionType())
                            .isEqualTo(EligibilityCriterionType.SERVICE_MONTHS));
        });

        assertThat(policyRepository.findById("SG_ANNUAL_03_11")).isEmpty();
        assertThat(policyRepository.findById("SG_ANNUAL_84_PLUS")).isEmpty();
    }

    @Test
    void annualLeaveRulesChangeExactlyAtTwoYearServiceBoundariesAndCapAtTenYears() {
        assertRules("SG_ANNUAL_00_23",
                tuple(EligibilityOperator.LESS_THAN, "24", 10));
        assertRules("SG_ANNUAL_24_47",
                tuple(EligibilityOperator.GREATER_THAN_OR_EQUAL, "24", 10),
                tuple(EligibilityOperator.LESS_THAN, "48", 20));
        assertRules("SG_ANNUAL_48_71",
                tuple(EligibilityOperator.GREATER_THAN_OR_EQUAL, "48", 10),
                tuple(EligibilityOperator.LESS_THAN, "72", 20));
        assertRules("SG_ANNUAL_72_95",
                tuple(EligibilityOperator.GREATER_THAN_OR_EQUAL, "72", 10),
                tuple(EligibilityOperator.LESS_THAN, "96", 20));
        assertRules("SG_ANNUAL_96_119",
                tuple(EligibilityOperator.GREATER_THAN_OR_EQUAL, "96", 10),
                tuple(EligibilityOperator.LESS_THAN, "120", 20));
        assertRules("SG_ANNUAL_120_PLUS",
                tuple(EligibilityOperator.GREATER_THAN_OR_EQUAL, "120", 10));
    }

    @Test
    void seedsCompanyDefaultCompassionateMarriageAndUnpaidLeave() {
        assertCompanyDefault("SG_COMPASSIONATE_DEFAULT", "SG:COMPASSIONATE_LEAVE", 2);
        assertCompanyDefault("SG_MARRIAGE_DEFAULT", "SG:MARRIAGE_LEAVE", 2);
        assertCompanyDefault("SG_UNPAID_DEFAULT", "SG:UNPAID_LEAVE", 14);
    }

    @Test
    void preservesSickAndHospitalisationServiceProgression() {
        assertEntitlements("SG_SICK_", List.of("03", "04", "05", "06_PLUS"), List.of(5, 8, 11, 14));
        assertEntitlements("SG_HOSP_", List.of("03", "04", "05", "06_PLUS"), List.of(15, 30, 45, 60));
    }

    private void assertCompanyDefault(String id, String jurisdictionLeaveTypeId, int amount) {
        LeaveEntitlementPolicy policy = policyRepository.findById(id).orElseThrow();
        assertThat(policy.getJurisdictionLeaveTypeId()).isEqualTo(jurisdictionLeaveTypeId);
        assertThat(policy.getEntitlementAmount()).isEqualByComparingTo(BigDecimal.valueOf(amount));
        assertThat(policy.getAccrualMethod()).isEqualTo(AccrualMethod.NONE);
        assertThat(policy.getProrationMethod()).isEqualTo(ProrationMethod.NONE);
        assertThat(eligibilityRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc(id)).isEmpty();
    }

    private void assertRules(String policyId, org.assertj.core.groups.Tuple... expected) {
        assertThat(eligibilityRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc(policyId))
                .extracting(LeaveEntitlementPolicyEligibilityRule::getOperator,
                        LeaveEntitlementPolicyEligibilityRule::getValue,
                        LeaveEntitlementPolicyEligibilityRule::getSortOrder)
                .containsExactly(expected);
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

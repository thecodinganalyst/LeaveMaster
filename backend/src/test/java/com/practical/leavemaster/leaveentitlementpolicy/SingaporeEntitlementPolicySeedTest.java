package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    void seedsSingaporeTemplatesWithPlatformScopeAndStatutoryEffectiveDates() {
        List<LeaveEntitlementPolicy> templates = policyRepository
                .findAllByScopeAndJurisdictionIdAndActiveTrue(ConfigurationScope.PLATFORM_TEMPLATE, "SG");

        assertThat(templates).hasSize(27);
        assertThat(templates).allSatisfy(policy -> {
            assertThat(policy.getTenantId()).isNull();
            assertThat(policy.getLeaveTypeId()).isNull();
            assertThat(policy.getJurisdictionId()).isEqualTo("SG");
            assertThat(policy.getJurisdictionLeaveTypeId()).startsWith("SG:");
        });

        assertStatutoryTemplate("SG_CHILDCARE_CITIZEN_U7", EntitlementUnit.DAYS, 6,
                LocalDate.of(2026, 1, 1), null);
        assertStatutoryTemplate("SG_CHILDCARE_EA_U7", EntitlementUnit.DAYS, 2,
                LocalDate.of(2026, 1, 1), null);
        assertStatutoryTemplate("SG_EXTENDED_CHILDCARE_7_12", EntitlementUnit.DAYS, 2,
                LocalDate.of(2026, 1, 1), null);
        assertStatutoryTemplate("SG_UNPAID_INFANT_CARE_U2", EntitlementUnit.DAYS, 12,
                LocalDate.of(2024, 1, 1), null);
        assertStatutoryTemplate("SG_MATERNITY_EVENT", EntitlementUnit.WEEKS, 16,
                LocalDate.of(2026, 1, 1), null);
        assertStatutoryTemplate("SG_PATERNITY_EVENT", EntitlementUnit.WEEKS, 4,
                LocalDate.of(2025, 4, 1), null);
        assertStatutoryTemplate("SG_SHARED_PARENTAL_EVENT_6W", EntitlementUnit.WEEKS, 6,
                LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31));
        assertStatutoryTemplate("SG_SHARED_PARENTAL_EVENT", EntitlementUnit.WEEKS, 10,
                LocalDate.of(2026, 4, 1), null);
        assertStatutoryTemplate("SG_ADOPTION_EVENT", EntitlementUnit.WEEKS, 12,
                LocalDate.of(2026, 1, 1), null);
        assertStatutoryTemplate("SG_NS_CALL_UP_EVENT", EntitlementUnit.DAYS, 1,
                LocalDate.of(2026, 1, 1), null);
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
    }

    @Test
    void removesObsoleteAnnualLeavePoliciesAndAllTheirEligibilityRules() {
        List.of(
                "SG_ANNUAL_03_11",
                "SG_ANNUAL_12_23",
                "SG_ANNUAL_24_35",
                "SG_ANNUAL_36_47",
                "SG_ANNUAL_48_59",
                "SG_ANNUAL_60_71"
        ).forEach(this::assertRemovedHistoricalPolicy);

        assertRetiredHistoricalPolicy("SG_ANNUAL_72_83");
        assertRetiredHistoricalPolicy("SG_ANNUAL_84_PLUS");
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
    void representsSingaporeMedicalLeaveAsSickPlusAdditionalHospitalisationBalances() {
        List<String> suffixes = List.of("03", "04", "05", "06_PLUS");
        List<Integer> sickAmounts = List.of(5, 8, 11, 14);
        List<Integer> additionalHospitalisationAmounts = List.of(10, 22, 34, 46);
        List<Integer> combinedMedicalLeaveMaximums = List.of(15, 30, 45, 60);

        assertEntitlements("SG_SICK_", suffixes, sickAmounts);
        assertEntitlements("SG_HOSP_", suffixes, additionalHospitalisationAmounts);

        for (int i = 0; i < suffixes.size(); i++) {
            BigDecimal sickAmount = entitlementAmount("SG_SICK_" + suffixes.get(i));
            BigDecimal hospitalisationAmount = entitlementAmount("SG_HOSP_" + suffixes.get(i));
            assertThat(sickAmount.add(hospitalisationAmount))
                    .isEqualByComparingTo(BigDecimal.valueOf(combinedMedicalLeaveMaximums.get(i)));
        }
    }

    private void assertStatutoryTemplate(String id, EntitlementUnit unit, int amount,
                                         LocalDate effectiveFrom, LocalDate effectiveTo) {
        LeaveEntitlementPolicy policy = policyRepository.findById(id).orElseThrow();
        assertThat(policy.getScope()).isEqualTo(ConfigurationScope.PLATFORM_TEMPLATE);
        assertThat(policy.getEntitlementUnit()).isEqualTo(unit);
        assertThat(policy.getEntitlementAmount()).isEqualByComparingTo(BigDecimal.valueOf(amount));
        assertThat(policy.getEffectiveFrom()).isEqualTo(effectiveFrom);
        assertThat(policy.getEffectiveTo()).isEqualTo(effectiveTo);
    }

    private void assertRemovedHistoricalPolicy(String id) {
        assertThat(policyRepository.findById(id)).isEmpty();
        assertThat(eligibilityRepository.existsByPolicyId(id)).isFalse();
    }

    private void assertRetiredHistoricalPolicy(String id) {
        LeaveEntitlementPolicy policy = policyRepository.findById(id).orElseThrow();
        assertThat(policy.isActive()).isFalse();
        assertThat(eligibilityRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc(id)).isNotEmpty();
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

    private BigDecimal entitlementAmount(String policyId) {
        return policyRepository.findById(policyId).orElseThrow().getEntitlementAmount();
    }
}

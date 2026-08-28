package com.practical.leavemaster;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeRepository;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.leaveentitlementpolicy.EventEntitlementAmountMode;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeavePolicyModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IndonesiaSeedDataIntegrationTest {

    @Autowired
    JurisdictionRepository jurisdictionRepository;

    @Autowired
    JurisdictionLeaveTypeRepository leaveTypeRepository;

    @Autowired
    LeaveEntitlementPolicyRepository policyRepository;

    @Autowired
    LeaveEntitlementPolicyEligibilityRepository eligibilityRepository;

    @Test
    void shouldSeedIndonesiaJurisdictionLeaveTypesAndPolicies() {
        assertThat(jurisdictionRepository.findById("ID")).isPresent();
        assertThat(jurisdictionRepository.findById("ID").orElseThrow().getName()).isEqualTo("Indonesia");

        Set<String> leaveTypeCodes = leaveTypeRepository.findAll().stream()
                .filter(type -> "ID".equals(type.getJurisdictionId()))
                .map(type -> type.getCode())
                .collect(Collectors.toSet());
        assertThat(leaveTypeCodes).contains(
                "ANNUAL_LEAVE", "SICK_LEAVE", "MENSTRUAL_LEAVE", "MATERNITY_LEAVE",
                "MATERNITY_EXTENSION_LEAVE", "MISCARRIAGE_LEAVE", "PATERNITY_LEAVE",
                "MARRIAGE_LEAVE", "CHILD_MARRIAGE_LEAVE", "CHILD_CIRCUMCISION_LEAVE",
                "CHILD_BAPTISM_LEAVE", "SPOUSE_BEREAVEMENT_LEAVE", "PARENT_BEREAVEMENT_LEAVE",
                "PARENT_IN_LAW_BEREAVEMENT_LEAVE", "CHILD_BEREAVEMENT_LEAVE",
                "CHILD_IN_LAW_BEREAVEMENT_LEAVE", "HOUSEHOLD_BEREAVEMENT_LEAVE");

        LeaveEntitlementPolicy annual = policyRepository.findById("ID_ANNUAL_12").orElseThrow();
        assertThat(annual.getScope()).isEqualTo(ConfigurationScope.PLATFORM_TEMPLATE);
        assertThat(annual.getJurisdictionId()).isEqualTo("ID");
        assertThat(annual.getEntitlementAmount()).isEqualByComparingTo(new BigDecimal("12"));
        assertThat(annual.getPolicyModel()).isEqualTo(LeavePolicyModel.ANNUAL_ENTITLEMENT);
        assertThat(eligibilityRepository.findAllByPolicyIdOrderBySortOrderAsc("ID_ANNUAL_12"))
                .singleElement()
                .satisfies(rule -> assertThat(rule.getCriterionValue()).isEqualTo("12"));

        assertEventPolicy("ID_MARRIAGE_EVENT", "MARRIAGE", "3");
        assertEventPolicy("ID_CHILD_MARRIAGE_EVENT", "CHILD_MARRIAGE", "2");
        assertEventPolicy("ID_DEATH_HOUSEHOLD_EVENT", "DEATH_HOUSEHOLD_MEMBER", "1");
        assertEventPolicy("ID_PATERNITY_EVENT", "BIRTH", "2");

        LeaveEntitlementPolicy maternity = policyRepository.findById("ID_MATERNITY_EVENT").orElseThrow();
        assertThat(maternity.getEventEntitlementAmountMode()).isEqualTo(EventEntitlementAmountMode.APPROVED_EVENT_AMOUNT);
        assertThat(maternity.isEventRequiresVerification()).isTrue();

        assertThat(policyRepository.findAllByScopeAndJurisdictionIdAndActiveTrue(ConfigurationScope.PLATFORM_TEMPLATE, "ID"))
                .noneMatch(policy -> "ID:SICK_LEAVE".equals(policy.getJurisdictionLeaveTypeId()));
    }

    private void assertEventPolicy(String id, String eventCode, String amount) {
        LeaveEntitlementPolicy policy = policyRepository.findById(id).orElseThrow();
        assertThat(policy.getPolicyModel()).isEqualTo(LeavePolicyModel.EVENT_BASED);
        assertThat(policy.getQualifyingEventTypeCode()).isEqualTo(eventCode);
        assertThat(policy.getEntitlementAmount()).isEqualByComparingTo(new BigDecimal(amount));
    }
}

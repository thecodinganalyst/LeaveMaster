package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.Jurisdiction;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.jurisdiction.JurisdictionType;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveEntitlementPolicyOpenEndedResolutionTest {

    @Mock private StaffRepository staffRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyEligibilityRepository ruleRepository;
    @Mock private JurisdictionRepository jurisdictionRepository;
    @Mock private AppUserRepository appUserRepository;

    @InjectMocks private LeaveEntitlementPolicyResolutionService service;

    @Test
    void resolvesTemplateWithNullEffectiveFromAsOpenEnded() {
        Staff preview = Staff.builder()
                .id("__preview__")
                .name("Preview")
                .tenantId("tenant-a")
                .jurisdictionId("SG")
                .joinDate(LocalDate.of(2025, 1, 1))
                .build();
        LeaveEntitlementPolicy template = LeaveEntitlementPolicy.builder()
                .id("SG_ANNUAL_03_11")
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId("SG")
                .jurisdictionLeaveTypeId("SG:ANNUAL_LEAVE")
                .name("Singapore annual leave")
                .active(true)
                .priority(10)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(new BigDecimal("7"))
                .accrualMethod(AccrualMethod.NONE)
                .prorationMethod(ProrationMethod.MONTHS)
                .effectiveFrom(null)
                .effectiveTo(null)
                .build();
        Jurisdiction singapore = Jurisdiction.builder()
                .id("SG")
                .code("SG")
                .name("Singapore")
                .countryCode("SG")
                .jurisdictionType(JurisdictionType.COUNTRY)
                .active(true)
                .build();

        when(policyRepository.findAllByScopeAndJurisdictionIdAndTenantIdIsNullAndActiveTrue(
                ConfigurationScope.PLATFORM_TEMPLATE, "SG")).thenReturn(List.of(template));
        when(jurisdictionRepository.findById("SG")).thenReturn(java.util.Optional.of(singapore));
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc(template.getId()))
                .thenReturn(List.of());

        PolicyResolutionResult result = service.resolveTemplate(
                preview, "SG:ANNUAL_LEAVE", LocalDate.of(2026, 8, 21));

        assertThat(result.selectedPolicyId()).isEqualTo(template.getId());
        assertThat(result.ambiguous()).isFalse();
        assertThat(result.consideredPolicies()).singleElement().satisfies(evaluation -> {
            assertThat(evaluation.effective()).isTrue();
            assertThat(evaluation.matched()).isTrue();
        });
    }
}

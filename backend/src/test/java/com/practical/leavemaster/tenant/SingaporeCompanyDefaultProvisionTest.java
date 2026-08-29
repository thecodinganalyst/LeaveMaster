package com.practical.leavemaster.tenant;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.Jurisdiction;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveType;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeRepository;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeService;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendarRepository;
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.EventEntitlementAmountMode;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeavePolicyModel;
import com.practical.leavemaster.leaveentitlementpolicy.ProrationMethod;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SingaporeCompanyDefaultProvisionTest {
    @Mock private JurisdictionRepository jurisdictionRepository;
    @Mock private JurisdictionLeaveTypeService jurisdictionLeaveTypeService;
    @Mock private JurisdictionLeaveTypeRepository jurisdictionLeaveTypeRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyEligibilityRepository eligibilityRepository;
    @Mock private LeaveCalendarRepository leaveCalendarRepository;

    @InjectMocks private TenantLeaveConfigurationProvisionService service;

    @Test
    void copiesEventAndRequestBasedSingaporeCompanyDefaultsWithoutTurningThemIntoAnnualPolicies() {
        Tenant tenant = Tenant.builder().id("acme-sg").build();
        Jurisdiction singapore = Jurisdiction.builder().id("SG").code("SG").name("Singapore").active(true).build();
        JurisdictionLeaveType compassionate = leaveType("SG:COMPASSIONATE_LEAVE", "COMPASSIONATE_LEAVE", "Compassionate Leave");
        JurisdictionLeaveType marriage = leaveType("SG:MARRIAGE_LEAVE", "MARRIAGE_LEAVE", "Marriage Leave");
        JurisdictionLeaveType unpaid = leaveType("SG:UNPAID_LEAVE", "UNPAID_LEAVE", "Unpaid Leave");

        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(singapore));
        when(leaveTypeRepository.findAllByTenantId("acme-sg")).thenReturn(List.of());
        when(jurisdictionLeaveTypeService.resolveEffective("SG")).thenReturn(List.of(compassionate, marriage, unpaid));
        when(jurisdictionLeaveTypeRepository.findById(compassionate.getId())).thenReturn(Optional.of(compassionate));
        when(jurisdictionLeaveTypeRepository.findById(marriage.getId())).thenReturn(Optional.of(marriage));
        when(jurisdictionLeaveTypeRepository.findById(unpaid.getId())).thenReturn(Optional.of(unpaid));
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveEntitlementPolicy compassionatePolicy = eventPolicy(
                "SG_COMPASSIONATE_DEFAULT", compassionate.getId(), "Singapore Compassionate Leave - company default", "BEREAVEMENT");
        LeaveEntitlementPolicy marriagePolicy = eventPolicy(
                "SG_MARRIAGE_DEFAULT", marriage.getId(), "Singapore Marriage Leave - company default", "MARRIAGE");
        LeaveEntitlementPolicy unpaidPolicy = requestPolicy(unpaid.getId());
        when(policyRepository.findAllByScopeAndJurisdictionIdAndActiveTrue(ConfigurationScope.PLATFORM_TEMPLATE, "SG"))
                .thenReturn(List.of(compassionatePolicy, marriagePolicy, unpaidPolicy));
        when(policyRepository.existsByTenantIdAndSourceTemplateId(any(), any())).thenReturn(false);
        when(policyRepository.save(any(LeaveEntitlementPolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eligibilityRepository.findAllByPolicyIdOrderBySortOrderAsc(any())).thenReturn(List.of());

        service.provision(tenant, new TenantJurisdictionProvisionRequest("SG", false, true, null, null));

        ArgumentCaptor<LeaveEntitlementPolicy> captor = ArgumentCaptor.forClass(LeaveEntitlementPolicy.class);
        verify(policyRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(LeaveEntitlementPolicy::getSourceTemplateId, LeaveEntitlementPolicy::getPolicyModel)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("SG_COMPASSIONATE_DEFAULT", LeavePolicyModel.EVENT_BASED),
                        org.assertj.core.groups.Tuple.tuple("SG_MARRIAGE_DEFAULT", LeavePolicyModel.EVENT_BASED),
                        org.assertj.core.groups.Tuple.tuple("SG_UNPAID_DEFAULT", LeavePolicyModel.REQUEST_BASED));
        assertThat(captor.getAllValues().stream()
                .filter(policy -> "SG_UNPAID_DEFAULT".equals(policy.getSourceTemplateId()))
                .findFirst().orElseThrow().getEntitlementAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private JurisdictionLeaveType leaveType(String id, String code, String name) {
        return JurisdictionLeaveType.builder()
                .id(id)
                .jurisdictionId("SG")
                .code(code)
                .name(name)
                .active(true)
                .build();
    }

    private LeaveEntitlementPolicy eventPolicy(String id, String leaveTypeId, String name, String eventType) {
        return LeaveEntitlementPolicy.builder()
                .id(id)
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId("SG")
                .jurisdictionLeaveTypeId(leaveTypeId)
                .name(name)
                .active(true)
                .priority(10)
                .policyModel(LeavePolicyModel.EVENT_BASED)
                .qualifyingEventTypeCode(eventType)
                .eventRequiresVerification(false)
                .eventValidityDaysBefore(0)
                .eventValidityDaysAfter(30)
                .eventEntitlementAmountMode(EventEntitlementAmountMode.FIXED)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(BigDecimal.valueOf(2))
                .accrualMethod(AccrualMethod.NONE)
                .prorationMethod(ProrationMethod.NONE)
                .build();
    }

    private LeaveEntitlementPolicy requestPolicy(String leaveTypeId) {
        return LeaveEntitlementPolicy.builder()
                .id("SG_UNPAID_DEFAULT")
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId("SG")
                .jurisdictionLeaveTypeId(leaveTypeId)
                .name("Singapore Unpaid Leave - company default")
                .active(true)
                .priority(10)
                .policyModel(LeavePolicyModel.REQUEST_BASED)
                .eventEntitlementAmountMode(EventEntitlementAmountMode.FIXED)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(BigDecimal.ZERO)
                .accrualMethod(AccrualMethod.NONE)
                .prorationMethod(ProrationMethod.NONE)
                .build();
    }
}

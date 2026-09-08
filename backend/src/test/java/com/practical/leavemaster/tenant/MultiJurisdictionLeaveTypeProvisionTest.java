package com.practical.leavemaster.tenant;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.Jurisdiction;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveType;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeRepository;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeService;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.jurisdiction.JurisdictionType;
import com.practical.leavemaster.leavecalendar.LeaveCalendarRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiJurisdictionLeaveTypeProvisionTest {
    private final List<LeaveType> tenantLeaveTypes = new ArrayList<>();
    private final List<LeaveEntitlementPolicy> tenantPolicies = new ArrayList<>();

    private JurisdictionRepository jurisdictionRepository;
    private JurisdictionLeaveTypeRepository jurisdictionLeaveTypeRepository;
    private LeaveTypeRepository leaveTypeRepository;
    private LeaveEntitlementPolicyRepository policyRepository;
    private LeaveEntitlementPolicyEligibilityRepository eligibilityRepository;
    private TenantLeaveConfigurationProvisionService service;

    private JurisdictionLeaveType sgAnnual;
    private JurisdictionLeaveType sgCompassionate;
    private JurisdictionLeaveType auAnnual;
    private JurisdictionLeaveType auPersonal;
    private JurisdictionLeaveType auCompassionate;
    private JurisdictionLeaveType auParental;
    private JurisdictionLeaveType auCommunity;
    private JurisdictionLeaveType auFamilyViolence;
    private JurisdictionLeaveType auLongService;
    private JurisdictionLeaveType nswLongService;
    private JurisdictionLeaveType waLongService;

    @BeforeEach
    void setUp() {
        jurisdictionRepository = mock(JurisdictionRepository.class);
        jurisdictionLeaveTypeRepository = mock(JurisdictionLeaveTypeRepository.class);
        leaveTypeRepository = mock(LeaveTypeRepository.class);
        policyRepository = mock(LeaveEntitlementPolicyRepository.class);
        eligibilityRepository = mock(LeaveEntitlementPolicyEligibilityRepository.class);
        LeaveCalendarRepository leaveCalendarRepository = mock(LeaveCalendarRepository.class);

        service = new TenantLeaveConfigurationProvisionService(
                jurisdictionRepository,
                new JurisdictionLeaveTypeService(jurisdictionLeaveTypeRepository, jurisdictionRepository),
                jurisdictionLeaveTypeRepository,
                leaveTypeRepository,
                policyRepository,
                eligibilityRepository,
                leaveCalendarRepository);

        stubJurisdiction(jurisdiction("SG", null, JurisdictionType.COUNTRY));
        stubJurisdiction(jurisdiction("AU", null, JurisdictionType.COUNTRY));
        stubJurisdiction(jurisdiction("AU-NSW", "AU", JurisdictionType.STATE));
        stubJurisdiction(jurisdiction("AU-WA", "AU", JurisdictionType.STATE));

        sgAnnual = leaveType("SG", "ANNUAL_LEAVE", "Annual Leave");
        sgCompassionate = leaveType("SG", "COMPASSIONATE_LEAVE", "Compassionate Leave");
        auAnnual = leaveType("AU", "ANNUAL_LEAVE", "Annual Leave");
        auPersonal = leaveType("AU", "PERSONAL_CARERS_LEAVE", "Personal / Carer's Leave");
        auCompassionate = leaveType("AU", "COMPASSIONATE_LEAVE", "Compassionate Leave");
        auParental = leaveType("AU", "PARENTAL_LEAVE", "Unpaid Parental Leave");
        auCommunity = leaveType("AU", "COMMUNITY_SERVICE_LEAVE", "Community Service Leave");
        auFamilyViolence = leaveType("AU", "FAMILY_DOMESTIC_VIOLENCE_LEAVE", "Family and Domestic Violence Leave");
        auLongService = leaveType("AU", "LONG_SERVICE_LEAVE", "Long Service Leave");
        nswLongService = leaveType("AU-NSW", "LONG_SERVICE_LEAVE", "Long Service Leave");
        nswLongService.setSourceName("NSW Industrial Relations");
        waLongService = leaveType("AU-WA", "LONG_SERVICE_LEAVE", "Long Service Leave");
        waLongService.setSourceName("WA Government");

        when(jurisdictionLeaveTypeRepository.findByJurisdictionIdAndActiveTrue("SG"))
                .thenReturn(List.of(sgAnnual, sgCompassionate));
        when(jurisdictionLeaveTypeRepository.findByJurisdictionIdAndActiveTrue("AU"))
                .thenReturn(List.of(auAnnual, auPersonal, auCompassionate, auParental, auCommunity,
                        auFamilyViolence, auLongService));
        when(jurisdictionLeaveTypeRepository.findByJurisdictionIdAndActiveTrue("AU-NSW"))
                .thenReturn(List.of(nswLongService));
        when(jurisdictionLeaveTypeRepository.findByJurisdictionIdAndActiveTrue("AU-WA"))
                .thenReturn(List.of(waLongService));
        when(jurisdictionLeaveTypeRepository.findById(any()))
                .thenAnswer(invocation -> sourceById(invocation.getArgument(0)));

        when(leaveTypeRepository.findAllByTenantId("global-co")).thenAnswer(invocation -> List.copyOf(tenantLeaveTypes));
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(invocation -> {
            LeaveType value = invocation.getArgument(0);
            if (tenantLeaveTypes.stream().noneMatch(existing -> existing.getId().equals(value.getId()))) {
                tenantLeaveTypes.add(value);
            }
            return value;
        });
        when(policyRepository.findAllByTenantId("global-co")).thenAnswer(invocation -> List.copyOf(tenantPolicies));
        when(policyRepository.save(any(LeaveEntitlementPolicy.class))).thenAnswer(invocation -> {
            LeaveEntitlementPolicy value = invocation.getArgument(0);
            if (value.getId() == null) value.setId("tenant-policy-" + (tenantPolicies.size() + 1));
            tenantPolicies.add(value);
            return value;
        });
        when(eligibilityRepository.findAllByPolicyIdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(policyRepository.findAllByScopeAndJurisdictionIdAndActiveTrue(any(), any())).thenReturn(List.of());
    }

    @Test
    void shouldProvisionSingaporeNswAndWaWithoutCrossJurisdictionCollisions() {
        Tenant tenant = Tenant.builder().id("global-co").build();

        provision(tenant, "SG");
        provision(tenant, "AU-NSW");
        provision(tenant, "AU-WA");
        provision(tenant, "AU-WA");

        List<LeaveType> singapore = forJurisdiction("SG");
        List<LeaveType> nsw = forJurisdiction("AU-NSW");
        List<LeaveType> wa = forJurisdiction("AU-WA");

        assertThat(singapore).hasSize(2);
        assertThat(nsw).hasSize(7);
        assertThat(wa).hasSize(7);
        assertThat(tenantLeaveTypes).hasSize(16);
        assertThat(tenantLeaveTypes).extracting(LeaveType::getId).doesNotHaveDuplicates();

        assertThat(nsw).extracting(LeaveType::getName).contains(
                "Annual Leave", "Personal / Carer's Leave", "Compassionate Leave",
                "Unpaid Parental Leave", "Community Service Leave",
                "Family and Domestic Violence Leave", "Long Service Leave");
        assertThat(wa).extracting(LeaveType::getName).contains(
                "Annual Leave", "Personal / Carer's Leave", "Compassionate Leave",
                "Unpaid Parental Leave", "Community Service Leave",
                "Family and Domestic Violence Leave", "Long Service Leave");

        LeaveType nswAnnual = byCode(nsw, "ANNUAL_LEAVE");
        LeaveType waAnnual = byCode(wa, "ANNUAL_LEAVE");
        assertThat(nswAnnual.getSourceJurisdictionLeaveTypeId()).isEqualTo("AU:ANNUAL_LEAVE");
        assertThat(waAnnual.getSourceJurisdictionLeaveTypeId()).isEqualTo("AU:ANNUAL_LEAVE");
        assertThat(nswAnnual.getJurisdictionId()).isEqualTo("AU-NSW");
        assertThat(waAnnual.getJurisdictionId()).isEqualTo("AU-WA");

        assertThat(byCode(nsw, "LONG_SERVICE_LEAVE").getSourceJurisdictionLeaveTypeId())
                .isEqualTo("AU-NSW:LONG_SERVICE_LEAVE");
        assertThat(byCode(wa, "LONG_SERVICE_LEAVE").getSourceJurisdictionLeaveTypeId())
                .isEqualTo("AU-WA:LONG_SERVICE_LEAVE");
    }

    @Test
    void shouldCopySameParentPolicyIndependentlyForNswAndWa() {
        LeaveEntitlementPolicy annualTemplate = LeaveEntitlementPolicy.builder()
                .id("AU:ANNUAL_POLICY")
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId("AU")
                .jurisdictionLeaveTypeId("AU:ANNUAL_LEAVE")
                .name("Australian Annual Leave")
                .active(true)
                .build();
        when(policyRepository.findAllByScopeAndJurisdictionIdAndActiveTrue(ConfigurationScope.PLATFORM_TEMPLATE, "AU"))
                .thenReturn(List.of(annualTemplate));

        Tenant tenant = Tenant.builder().id("global-co").build();
        provision(tenant, "AU-NSW");
        provision(tenant, "AU-WA");

        assertThat(tenantPolicies).hasSize(2);
        assertThat(tenantPolicies).extracting(LeaveEntitlementPolicy::getJurisdictionId)
                .containsExactlyInAnyOrder("AU-NSW", "AU-WA");
        assertThat(tenantPolicies).extracting(LeaveEntitlementPolicy::getSourceTemplateId)
                .containsOnly("AU:ANNUAL_POLICY");
        assertThat(tenantPolicies).extracting(LeaveEntitlementPolicy::getLeaveTypeId)
                .containsExactlyInAnyOrder(
                        "global-co:AU-NSW:ANNUAL_LEAVE",
                        "global-co:AU-WA:ANNUAL_LEAVE");
    }

    private void provision(Tenant tenant, String jurisdictionId) {
        service.provision(tenant, new TenantJurisdictionProvisionRequest(
                jurisdictionId, false, true, null, null));
    }

    private List<LeaveType> forJurisdiction(String jurisdictionId) {
        return tenantLeaveTypes.stream()
                .filter(item -> jurisdictionId.equals(item.getJurisdictionId()))
                .toList();
    }

    private LeaveType byCode(List<LeaveType> leaveTypes, String code) {
        return leaveTypes.stream()
                .filter(item -> item.getSourceJurisdictionLeaveTypeId() != null)
                .filter(item -> item.getSourceJurisdictionLeaveTypeId().endsWith(":" + code))
                .findFirst()
                .orElseThrow();
    }

    private Optional<JurisdictionLeaveType> sourceById(String id) {
        return List.of(sgAnnual, sgCompassionate, auAnnual, auPersonal, auCompassionate, auParental,
                        auCommunity, auFamilyViolence, auLongService, nswLongService, waLongService)
                .stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
    }

    private void stubJurisdiction(Jurisdiction jurisdiction) {
        when(jurisdictionRepository.findById(jurisdiction.getId())).thenReturn(Optional.of(jurisdiction));
    }

    private Jurisdiction jurisdiction(String id, String parentId, JurisdictionType type) {
        return Jurisdiction.builder()
                .id(id)
                .code(id)
                .name(id)
                .jurisdictionType(type)
                .parentId(parentId)
                .countryCode(id.startsWith("AU") ? "AU" : id)
                .active(true)
                .build();
    }

    private JurisdictionLeaveType leaveType(String jurisdictionId, String code, String name) {
        return JurisdictionLeaveType.builder()
                .id(jurisdictionId + ":" + code)
                .jurisdictionId(jurisdictionId)
                .code(code)
                .name(name)
                .statutory(true)
                .active(true)
                .build();
    }
}

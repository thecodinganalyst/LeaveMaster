package com.practical.leavemaster.tenant;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.Jurisdiction;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveType;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeRepository;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeService;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.jurisdiction.JurisdictionType;
import com.practical.leavemaster.leavecalendar.LeaveCalendarRepository;
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

class AustraliaLeaveTypeInheritanceProvisionTest {
    private JurisdictionRepository jurisdictionRepository;
    private JurisdictionLeaveTypeRepository jurisdictionLeaveTypeRepository;
    private LeaveTypeRepository leaveTypeRepository;
    private LeaveEntitlementPolicyRepository policyRepository;
    private TenantLeaveConfigurationProvisionService service;
    private final List<LeaveType> tenantLeaveTypes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        jurisdictionRepository = mock(JurisdictionRepository.class);
        jurisdictionLeaveTypeRepository = mock(JurisdictionLeaveTypeRepository.class);
        leaveTypeRepository = mock(LeaveTypeRepository.class);
        policyRepository = mock(LeaveEntitlementPolicyRepository.class);
        LeaveEntitlementPolicyEligibilityRepository eligibilityRepository = mock(LeaveEntitlementPolicyEligibilityRepository.class);
        LeaveCalendarRepository leaveCalendarRepository = mock(LeaveCalendarRepository.class);
        JurisdictionLeaveTypeService leaveTypeService = new JurisdictionLeaveTypeService(
                jurisdictionLeaveTypeRepository, jurisdictionRepository);
        service = new TenantLeaveConfigurationProvisionService(
                jurisdictionRepository,
                leaveTypeService,
                jurisdictionLeaveTypeRepository,
                leaveTypeRepository,
                policyRepository,
                eligibilityRepository,
                leaveCalendarRepository);

        Jurisdiction au = jurisdiction("AU", null, JurisdictionType.COUNTRY);
        Jurisdiction nsw = jurisdiction("AU-NSW", "AU", JurisdictionType.STATE);
        when(jurisdictionRepository.findById("AU")).thenReturn(Optional.of(au));
        when(jurisdictionRepository.findById("AU-NSW")).thenReturn(Optional.of(nsw));

        JurisdictionLeaveType annual = leaveType("AU", "ANNUAL_LEAVE", "Annual Leave");
        JurisdictionLeaveType personal = leaveType("AU", "PERSONAL_CARERS_LEAVE", "Personal / Carer's Leave");
        JurisdictionLeaveType federalLongService = leaveType("AU", "LONG_SERVICE_LEAVE", "Federal Long Service Leave");
        JurisdictionLeaveType nswLongService = leaveType("AU-NSW", "LONG_SERVICE_LEAVE", "Long Service Leave");
        nswLongService.setSourceName("NSW Industrial Relations");

        when(jurisdictionLeaveTypeRepository.findByJurisdictionIdAndActiveTrue("AU"))
                .thenReturn(List.of(annual, personal, federalLongService));
        when(jurisdictionLeaveTypeRepository.findByJurisdictionIdAndActiveTrue("AU-NSW"))
                .thenReturn(List.of(nswLongService));
        when(jurisdictionLeaveTypeRepository.findById(any()))
                .thenAnswer(invocation -> {
                    String id = invocation.getArgument(0);
                    return List.of(annual, personal, federalLongService, nswLongService).stream()
                            .filter(item -> item.getId().equals(id))
                            .findFirst();
                });
        when(leaveTypeRepository.findAllByTenantId("acme-au")).thenAnswer(invocation -> List.copyOf(tenantLeaveTypes));
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(invocation -> {
            LeaveType value = invocation.getArgument(0);
            tenantLeaveTypes.add(value);
            return value;
        });
        when(policyRepository.findAllByScopeAndJurisdictionIdAndActiveTrue(
                ConfigurationScope.PLATFORM_TEMPLATE, "AU")).thenReturn(List.of());
        when(policyRepository.findAllByScopeAndJurisdictionIdAndActiveTrue(
                ConfigurationScope.PLATFORM_TEMPLATE, "AU-NSW")).thenReturn(List.of());
    }

    @Test
    void shouldProvisionFederalLeaveTypesWithNswOverride() {
        Tenant tenant = Tenant.builder().id("acme-au").build();

        service.provision(tenant, new TenantJurisdictionProvisionRequest(
                "AU-NSW", false, true, null, null));

        assertThat(tenantLeaveTypes).hasSize(3);
        assertThat(tenantLeaveTypes).extracting(LeaveType::getName)
                .containsExactlyInAnyOrder("Annual Leave", "Personal / Carer's Leave", "Long Service Leave");
        LeaveType longService = tenantLeaveTypes.stream()
                .filter(item -> item.getId().equals("acme-au:LONG_SERVICE_LEAVE"))
                .findFirst().orElseThrow();
        assertThat(longService.getSourceJurisdictionLeaveTypeId()).isEqualTo("AU-NSW:LONG_SERVICE_LEAVE");
        assertThat(longService.getSourceName()).isEqualTo("NSW Industrial Relations");
    }

    @Test
    void shouldNotDuplicateLeaveTypesWhenParentThenChildAreProvisioned() {
        Tenant tenant = Tenant.builder().id("acme-au").build();

        service.provision(tenant, new TenantJurisdictionProvisionRequest(
                "AU", false, true, null, null));
        service.provision(tenant, new TenantJurisdictionProvisionRequest(
                "AU-NSW", false, true, null, null));

        assertThat(tenantLeaveTypes).hasSize(3);
        assertThat(tenantLeaveTypes).extracting(LeaveType::getId)
                .doesNotHaveDuplicates();
    }

    private Jurisdiction jurisdiction(String id, String parentId, JurisdictionType type) {
        return Jurisdiction.builder()
                .id(id).code(id).name(id)
                .jurisdictionType(type)
                .parentId(parentId)
                .countryCode("AU")
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

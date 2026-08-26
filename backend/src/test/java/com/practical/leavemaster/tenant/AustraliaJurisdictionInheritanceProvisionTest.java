package com.practical.leavemaster.tenant;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.Jurisdiction;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeRepository;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeService;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarRepository;
import com.practical.leavemaster.leavecalendar.PublicHoliday;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AustraliaJurisdictionInheritanceProvisionTest {
    @Mock private JurisdictionRepository jurisdictionRepository;
    @Mock private JurisdictionLeaveTypeService jurisdictionLeaveTypeService;
    @Mock private JurisdictionLeaveTypeRepository jurisdictionLeaveTypeRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyEligibilityRepository eligibilityRepository;
    @Mock private LeaveCalendarRepository leaveCalendarRepository;

    @InjectMocks private TenantLeaveConfigurationProvisionService service;

    @Test
    void shouldMergeFederalAndStatePublicHolidaysIntoOneTenantCalendar() {
        Tenant tenant = Tenant.builder().id("acme-au").build();
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        Jurisdiction au = australia();
        Jurisdiction vic = victoria();
        LeaveCalendar federal = federalCalendar(start, end);
        LeaveCalendar victoria = victoriaCalendar(start, end);

        stubHierarchy(au, vic);
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "AU-VIC"))
                .thenReturn(List.of(victoria));
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "AU"))
                .thenReturn(List.of(federal));
        when(leaveCalendarRepository.findByTenantIdAndJurisdictionIdAndStartAndEnd("acme-au", "AU-VIC", start, end))
                .thenReturn(Optional.empty());
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.provision(tenant, new TenantJurisdictionProvisionRequest(
                "AU-VIC", true, false, start, end));

        ArgumentCaptor<LeaveCalendar> captor = ArgumentCaptor.forClass(LeaveCalendar.class);
        verify(leaveCalendarRepository).save(captor.capture());
        assertMergedCalendar(captor.getValue());
    }

    @Test
    void shouldMergeInheritedCalendarsDuringDefaultTenantProvisioning() {
        Tenant tenant = Tenant.builder().id("acme-au").jurisdictionId("AU-VIC").build();
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        Jurisdiction au = australia();
        Jurisdiction vic = victoria();

        stubHierarchy(au, vic);
        stubEmptyLeaveConfiguration();
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "AU-VIC"))
                .thenReturn(List.of(victoriaCalendar(start, end)));
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "AU"))
                .thenReturn(List.of(federalCalendar(start, end)));
        when(leaveCalendarRepository.findByTenantIdAndJurisdictionIdAndStartAndEnd("acme-au", "AU-VIC", start, end))
                .thenReturn(Optional.empty());
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.provision(tenant);

        ArgumentCaptor<LeaveCalendar> captor = ArgumentCaptor.forClass(LeaveCalendar.class);
        verify(leaveCalendarRepository).save(captor.capture());
        assertMergedCalendar(captor.getValue());
    }

    @Test
    void shouldNotRewriteAlreadyMergedInheritedCalendar() {
        Tenant tenant = Tenant.builder().id("acme-au").jurisdictionId("AU-VIC").build();
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        Jurisdiction au = australia();
        Jurisdiction vic = victoria();
        LeaveCalendar federal = federalCalendar(start, end);
        LeaveCalendar victoria = victoriaCalendar(start, end);
        LeaveCalendar existing = LeaveCalendar.builder()
                .id("tenant-au-vic-2026")
                .scope(ConfigurationScope.TENANT)
                .tenantId("acme-au")
                .jurisdictionId("AU-VIC")
                .start(start)
                .end(end)
                .sourceTemplateId("au-vic-2026,au-2026")
                .publicHolidays(new ArrayList<>(List.of(
                        copyHoliday(victoria.getPublicHolidays().getFirst()),
                        copyHoliday(federal.getPublicHolidays().getFirst()))))
                .build();

        stubHierarchy(au, vic);
        stubEmptyLeaveConfiguration();
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "AU-VIC"))
                .thenReturn(List.of(victoria));
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "AU"))
                .thenReturn(List.of(federal));
        when(leaveCalendarRepository.findByTenantIdAndJurisdictionIdAndStartAndEnd("acme-au", "AU-VIC", start, end))
                .thenReturn(Optional.of(existing));
        when(leaveCalendarRepository.findById("tenant-au-vic-2026")).thenReturn(Optional.of(existing));

        service.provision(tenant);

        verify(leaveCalendarRepository, never()).save(any(LeaveCalendar.class));
    }

    @Test
    void shouldRejectCyclicHierarchyWhenResolvingInheritedCalendars() {
        Tenant tenant = Tenant.builder().id("acme-au").build();
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        Jurisdiction au = australia();
        au.setParentId("AU-VIC");
        Jurisdiction vic = victoria();

        stubHierarchy(au, vic);
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "AU-VIC"))
                .thenReturn(List.of());
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "AU"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.provision(tenant, new TenantJurisdictionProvisionRequest(
                "AU-VIC", true, false, start, end)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cycle");
    }

    private void stubHierarchy(Jurisdiction au, Jurisdiction vic) {
        when(jurisdictionRepository.findById("AU-VIC")).thenReturn(Optional.of(vic));
        when(jurisdictionRepository.findById("AU")).thenReturn(Optional.of(au));
    }

    private void stubEmptyLeaveConfiguration() {
        when(jurisdictionLeaveTypeService.resolveEffective("AU-VIC")).thenReturn(List.of());
        when(leaveTypeRepository.findAllByTenantId("acme-au")).thenReturn(List.of());
        when(policyRepository.findAllByScopeAndJurisdictionIdAndActiveTrue(ConfigurationScope.PLATFORM_TEMPLATE, "AU-VIC"))
                .thenReturn(List.of());
        when(policyRepository.findAllByScopeAndJurisdictionIdAndActiveTrue(ConfigurationScope.PLATFORM_TEMPLATE, "AU"))
                .thenReturn(List.of());
    }

    private void assertMergedCalendar(LeaveCalendar saved) {
        assertThat(saved.getJurisdictionId()).isEqualTo("AU-VIC");
        assertThat(saved.getPublicHolidays())
                .extracting(PublicHoliday::getHolidayName)
                .containsExactlyInAnyOrder("Australia Day", "Labour Day");
        assertThat(saved.getSourceTemplateId()).contains("au-vic-2026").contains("au-2026");
    }

    private Jurisdiction australia() {
        return Jurisdiction.builder()
                .id("AU").code("AU").name("Australia").countryCode("AU").active(true).build();
    }

    private Jurisdiction victoria() {
        return Jurisdiction.builder()
                .id("AU-VIC").code("AU-VIC").name("Victoria").countryCode("AU")
                .parentId("AU").active(true).build();
    }

    private LeaveCalendar federalCalendar(LocalDate start, LocalDate end) {
        return LeaveCalendar.builder()
                .id("au-2026").scope(ConfigurationScope.PLATFORM_TEMPLATE).jurisdictionId("AU")
                .start(start).end(end)
                .publicHolidays(List.of(PublicHoliday.builder()
                        .holidayDate(LocalDate.of(2026, 1, 26)).holidayName("Australia Day").build()))
                .build();
    }

    private LeaveCalendar victoriaCalendar(LocalDate start, LocalDate end) {
        return LeaveCalendar.builder()
                .id("au-vic-2026").scope(ConfigurationScope.PLATFORM_TEMPLATE).jurisdictionId("AU-VIC")
                .start(start).end(end)
                .publicHolidays(List.of(PublicHoliday.builder()
                        .holidayDate(LocalDate.of(2026, 3, 9)).holidayName("Labour Day").build()))
                .build();
    }

    private PublicHoliday copyHoliday(PublicHoliday holiday) {
        return PublicHoliday.builder()
                .holidayDate(holiday.getHolidayDate())
                .holidayName(holiday.getHolidayName())
                .build();
    }
}

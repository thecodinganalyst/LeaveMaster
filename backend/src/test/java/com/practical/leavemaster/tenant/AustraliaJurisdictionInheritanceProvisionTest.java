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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        Jurisdiction au = Jurisdiction.builder()
                .id("AU").code("AU").name("Australia").countryCode("AU").active(true).build();
        Jurisdiction vic = Jurisdiction.builder()
                .id("AU-VIC").code("AU-VIC").name("Victoria").countryCode("AU")
                .parentId("AU").active(true).build();
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        LeaveCalendar federal = LeaveCalendar.builder()
                .id("au-2026").scope(ConfigurationScope.PLATFORM_TEMPLATE).jurisdictionId("AU")
                .start(start).end(end)
                .publicHolidays(List.of(PublicHoliday.builder()
                        .holidayDate(LocalDate.of(2026, 1, 26)).holidayName("Australia Day").build()))
                .build();
        LeaveCalendar victoria = LeaveCalendar.builder()
                .id("au-vic-2026").scope(ConfigurationScope.PLATFORM_TEMPLATE).jurisdictionId("AU-VIC")
                .start(start).end(end)
                .publicHolidays(List.of(PublicHoliday.builder()
                        .holidayDate(LocalDate.of(2026, 3, 9)).holidayName("Labour Day").build()))
                .build();

        when(jurisdictionRepository.findById("AU-VIC")).thenReturn(Optional.of(vic));
        when(jurisdictionRepository.findById("AU")).thenReturn(Optional.of(au));
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
        LeaveCalendar saved = captor.getValue();
        assertThat(saved.getJurisdictionId()).isEqualTo("AU-VIC");
        assertThat(saved.getPublicHolidays())
                .extracting(PublicHoliday::getHolidayName)
                .containsExactlyInAnyOrder("Australia Day", "Labour Day");
        assertThat(saved.getSourceTemplateId()).contains("au-vic-2026").contains("au-2026");
    }
}

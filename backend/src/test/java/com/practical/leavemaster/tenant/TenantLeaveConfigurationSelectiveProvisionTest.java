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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantLeaveConfigurationSelectiveProvisionTest {

    @Mock private JurisdictionRepository jurisdictionRepository;
    @Mock private JurisdictionLeaveTypeService jurisdictionLeaveTypeService;
    @Mock private JurisdictionLeaveTypeRepository jurisdictionLeaveTypeRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyEligibilityRepository eligibilityRepository;
    @Mock private LeaveCalendarRepository leaveCalendarRepository;
    @InjectMocks private TenantLeaveConfigurationProvisionService service;

    @Test
    void shouldCreateRequestedCalendarAndCopyOnlyHolidaysInRange() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        Tenant tenant = Tenant.builder().id("ACME").build();
        Jurisdiction sg = Jurisdiction.builder().id("SG").code("SG").name("Singapore").build();
        LeaveCalendar template = LeaveCalendar.builder()
                .id("sg-2026")
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId("SG")
                .start(start)
                .end(end)
                .publicHolidays(List.of(
                        PublicHoliday.builder().holidayDate(LocalDate.of(2026, 1, 1)).holidayName("New Year").build(),
                        PublicHoliday.builder().holidayDate(LocalDate.of(2026, 12, 25)).holidayName("Christmas").build()))
                .build();
        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(sg));
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "SG")).thenReturn(List.of(template));
        when(leaveCalendarRepository.findByTenantIdAndJurisdictionIdAndStartAndEnd("ACME", "SG", start, end)).thenReturn(Optional.empty());
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.provision(tenant, new TenantJurisdictionProvisionRequest("SG", true, false, start, end));

        ArgumentCaptor<LeaveCalendar> calendarCaptor = ArgumentCaptor.forClass(LeaveCalendar.class);
        verify(leaveCalendarRepository).save(calendarCaptor.capture());
        LeaveCalendar created = calendarCaptor.getValue();
        assertThat(created.getTenantId()).isEqualTo("ACME");
        assertThat(created.getJurisdictionId()).isEqualTo("SG");
        assertThat(created.getStart()).isEqualTo(start);
        assertThat(created.getEnd()).isEqualTo(end);
        assertThat(created.getSourceTemplateId()).isEqualTo("sg-2026");
        assertThat(created.getPublicHolidays()).extracting(PublicHoliday::getHolidayName)
                .containsExactly("New Year", "Christmas");
        verify(jurisdictionLeaveTypeService, never()).resolveEffective(any());
    }

    @Test
    void shouldDoNothingWhenBothTemplateOptionsAreDisabled() {
        Tenant tenant = Tenant.builder().id("ACME").build();
        Jurisdiction sg = Jurisdiction.builder().id("SG").code("SG").name("Singapore").build();
        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(sg));

        service.provision(tenant, new TenantJurisdictionProvisionRequest("SG", false, false, null, null));

        verify(jurisdictionLeaveTypeService, never()).resolveEffective(any());
        verify(leaveCalendarRepository, never()).findAllByScopeAndJurisdictionId(any(), any());
        verify(leaveCalendarRepository, never()).save(any());
    }

    @Test
    void shouldRejectHolidayImportWhenTemplateDoesNotCoverRequestedRange() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        Tenant tenant = Tenant.builder().id("ACME").build();
        Jurisdiction sg = Jurisdiction.builder().id("SG").code("SG").name("Singapore").build();
        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(sg));
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "SG")).thenReturn(List.of());

        assertThatThrownBy(() -> service.provision(
                tenant,
                new TenantJurisdictionProvisionRequest("SG", true, false, start, end)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No public holiday template");
    }

    @Test
    void shouldRequireCalendarDatesWhenHolidayImportIsRequested() {
        Tenant tenant = Tenant.builder().id("ACME").build();
        Jurisdiction sg = Jurisdiction.builder().id("SG").code("SG").name("Singapore").build();
        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(sg));

        assertThatThrownBy(() -> service.provision(
                tenant,
                new TenantJurisdictionProvisionRequest("SG", true, false, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Calendar start and end dates");
    }
}

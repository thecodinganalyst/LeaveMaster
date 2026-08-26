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
class MalaysiaJurisdictionInheritanceProvisionTest {
    @Mock private JurisdictionRepository jurisdictionRepository;
    @Mock private JurisdictionLeaveTypeService jurisdictionLeaveTypeService;
    @Mock private JurisdictionLeaveTypeRepository jurisdictionLeaveTypeRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyEligibilityRepository eligibilityRepository;
    @Mock private LeaveCalendarRepository leaveCalendarRepository;

    @InjectMocks private TenantLeaveConfigurationProvisionService service;

    @Test
    void selangorShouldReceiveMalaysiaAndSelangorHolidaysButNotJohorHolidays() {
        Tenant tenant = Tenant.builder().id("acme-my").build();
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        Jurisdiction malaysia = Jurisdiction.builder()
                .id("MY").code("MY").name("Malaysia").countryCode("MY").active(true).build();
        Jurisdiction selangor = Jurisdiction.builder()
                .id("MY-SGR").code("MY-SGR").name("Selangor").countryCode("MY")
                .parentId("MY").active(true).build();

        when(jurisdictionRepository.findById("MY-SGR")).thenReturn(Optional.of(selangor));
        when(jurisdictionRepository.findById("MY")).thenReturn(Optional.of(malaysia));
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "MY-SGR"))
                .thenReturn(List.of(calendar("my-sgr-2026", "MY-SGR", start, end,
                        LocalDate.of(2026, 12, 11), "Birthday of the Sultan of Selangor")));
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "MY"))
                .thenReturn(List.of(calendar("my-2026", "MY", start, end,
                        LocalDate.of(2026, 8, 31), "National Day")));
        when(leaveCalendarRepository.findByTenantIdAndJurisdictionIdAndStartAndEnd("acme-my", "MY-SGR", start, end))
                .thenReturn(Optional.empty());
        when(leaveCalendarRepository.save(any(LeaveCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.provision(tenant, new TenantJurisdictionProvisionRequest("MY-SGR", true, false, start, end));

        ArgumentCaptor<LeaveCalendar> captor = ArgumentCaptor.forClass(LeaveCalendar.class);
        verify(leaveCalendarRepository).save(captor.capture());
        assertThat(captor.getValue().getPublicHolidays())
                .extracting(PublicHoliday::getHolidayName)
                .containsExactlyInAnyOrder("National Day", "Birthday of the Sultan of Selangor")
                .doesNotContain("Birthday of the Sultan of Johor");
        assertThat(captor.getValue().getSourceTemplateId()).contains("my-2026").contains("my-sgr-2026");
    }

    private LeaveCalendar calendar(String id, String jurisdictionId, LocalDate start, LocalDate end,
                                   LocalDate holidayDate, String holidayName) {
        return LeaveCalendar.builder()
                .id(id)
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId(jurisdictionId)
                .start(start)
                .end(end)
                .publicHolidays(List.of(PublicHoliday.builder()
                        .holidayDate(holidayDate)
                        .holidayName(holidayName)
                        .build()))
                .build();
    }
}

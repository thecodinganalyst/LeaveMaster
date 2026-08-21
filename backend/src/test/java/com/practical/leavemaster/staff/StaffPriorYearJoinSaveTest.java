package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffPriorYearJoinSaveTest {

    @Mock private StaffRepository staffRepository;
    @Mock private LeaveCalendarService leaveCalendarService;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveApproverRepository leaveApproverRepository;
    @Mock private LeaveApplicationRepository leaveApplicationRepository;
    @Mock private AppUserService appUserService;
    @Mock private TenantActivityService tenantActivityService;
    @Mock private AppUserRepository appUserRepository;

    @InjectMocks private StaffService staffService;

    private LocalDate today;
    private LocalDate priorYearJoinDate;
    private LeaveCalendar currentCalendar;
    private LeaveType childcareLeave;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        priorYearJoinDate = today.minusYears(1);
        currentCalendar = LeaveCalendar.builder()
                .id("tenant-a:SG:" + today.getYear())
                .tenantId("tenant-a")
                .jurisdictionId("SG")
                .start(LocalDate.of(today.getYear(), 1, 1))
                .end(LocalDate.of(today.getYear(), 12, 31))
                .build();
        childcareLeave = LeaveType.builder()
                .id("childcare")
                .name("Childcare Leave")
                .tenantId("tenant-a")
                .used(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hr", "n/a", List.of()));
        when(appUserRepository.findById("hr")).thenReturn(Optional.of(AppUser.builder()
                .loginName("hr")
                .password("n/a")
                .active(true)
                .tenantId("tenant-a")
                .build()));
        when(leaveCalendarService.getCalendarFor("SG", today)).thenReturn(Optional.of(currentCalendar));
        when(leaveTypeRepository.findById("childcare")).thenReturn(Optional.of(childcareLeave));
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSavePriorYearJoinerWithManualCurrentYearEntitlement() {
        LeaveEntitlement manualEntitlement = LeaveEntitlement.builder()
                .leaveType(LeaveType.builder().id("childcare").build())
                .from(currentCalendar.getStart())
                .to(currentCalendar.getEnd())
                .entitlement(new BigDecimal("6.0"))
                .build();
        Staff staff = Staff.builder()
                .id("S1")
                .name("Alice")
                .joinDate(priorYearJoinDate)
                .jurisdictionId("SG")
                .leaveEntitlements(List.of(manualEntitlement))
                .build();

        Staff saved = staffService.save(staff);

        assertThat(saved.getJoinDate()).isEqualTo(priorYearJoinDate);
        assertThat(saved.getLeaveEntitlements()).singleElement().satisfies(entitlement -> {
            assertThat(entitlement.getFrom()).isEqualTo(currentCalendar.getStart());
            assertThat(entitlement.getTo()).isEqualTo(currentCalendar.getEnd());
            assertThat(entitlement.getEntitlement()).isEqualByComparingTo("6.0");
        });
        verify(leaveCalendarService).getCalendarFor("SG", today);
        verify(leaveCalendarService, never()).getCalendarFor("SG", priorYearJoinDate);
    }

    @Test
    void shouldDefaultPriorYearJoinerEntitlementToCurrentCalendarPeriod() {
        LeaveEntitlement entitlementWithoutPeriod = LeaveEntitlement.builder()
                .leaveType(LeaveType.builder().id("childcare").build())
                .entitlement(new BigDecimal("6.0"))
                .build();
        Staff staff = Staff.builder()
                .id("S2")
                .name("Bob")
                .joinDate(priorYearJoinDate)
                .jurisdictionId("SG")
                .leaveEntitlements(List.of(entitlementWithoutPeriod))
                .build();

        Staff saved = staffService.save(staff);

        assertThat(saved.getLeaveEntitlements()).singleElement().satisfies(entitlement -> {
            assertThat(entitlement.getFrom()).isEqualTo(currentCalendar.getStart());
            assertThat(entitlement.getTo()).isEqualTo(currentCalendar.getEnd());
            assertThat(entitlement.getEntitlement()).isEqualByComparingTo("6.0");
        });
        verify(leaveCalendarService, never()).getCalendarFor("SG", priorYearJoinDate);
    }
}

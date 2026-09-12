package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.email.EmailService;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leavecalendar.PublicHoliday;
import com.practical.leavemaster.leaveentitlement.EventLeaveEntitlementService;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.storage.StorageService;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.testsupport.ScenarioDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class CoreBusinessScenarioRegressionTest {

    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 9, 12);

    @Mock LeaveApplicationRepository leaveApplicationRepository;
    @Mock StaffRepository staffRepository;
    @Mock LeaveTypeRepository leaveTypeRepository;
    @Mock LeaveCalendarService leaveCalendarService;
    @Mock LeaveApproverRepository leaveApproverRepository;
    @Mock EmailService emailService;
    @Mock TenantActivityService tenantActivityService;
    @Mock StorageService storageService;
    @Mock EventLeaveEntitlementService eventLeaveEntitlementService;

    @InjectMocks LeaveApplicationService leaveApplicationService;

    private ScenarioDataFactory.Scenario scenario;

    @BeforeEach
    void setUpScenario() {
        scenario = ScenarioDataFactory.standardSingaporeScenario("core-regression", REFERENCE_DATE);
    }

    @Test
    void standardScenarioContainsTenantStaffAndAnnualEntitlements() {
        assertThat(scenario.tenant().getJurisdictionIds()).containsExactly("SG");
        assertThat(scenario.staff()).containsKeys("admin", "hr", "manager01", "manager02",
                "staff001", "staff002", "staff003", "staff004", "staff005");
        assertThat(scenario.entitlements()).hasSize(5);
        assertThat(scenario.staff("staff001").getWorkSchedule()).hasSize(5);
    }

    @Test
    void midYearJoinerStartsEntitlementOnJoinDateAndReceivesProratedAmount() {
        var entitlement = scenario.entitlements().get("staff002");
        assertThat(entitlement.getFrom()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(entitlement.getEntitlement()).isEqualByComparingTo(new BigDecimal("7.00"));
    }

    @Test
    void cannotApplyBeforeEmploymentStartDate() {
        Staff staff = scenario.staff("staff003");
        LeaveType leaveType = scenario.leaveTypes().getFirst();
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));

        LeaveApplicationRequest request = request(staff, leaveType, staff.getJoinDate().minusDays(1), staff.getJoinDate());

        assertThatThrownBy(() -> leaveApplicationService.apply(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before employment start date");
        verify(leaveTypeRepository, never()).findById(any());
    }

    @Test
    void cannotApplyAfterTerminationDate() {
        Staff base = scenario.staff("staff001");
        Staff staff = base.toBuilder().termDate(REFERENCE_DATE.plusDays(5)).build();
        LeaveType leaveType = scenario.leaveTypes().getFirst();
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));

        LeaveApplicationRequest request = request(staff, leaveType, REFERENCE_DATE.plusDays(5), REFERENCE_DATE.plusDays(6));

        assertThatThrownBy(() -> leaveApplicationService.apply(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("after termination date");
    }

    @Test
    void assignedManagerCanApproveAndUnrelatedManagerCannotAct() {
        Staff employee = scenario.staff("staff001");
        Staff assignedManager = scenario.staff("manager01");
        Staff unrelatedManager = scenario.staff("manager02");
        LeaveApplication pending = pendingApplication(employee);
        when(leaveApplicationRepository.findById("request-1")).thenReturn(Optional.of(pending));
        when(staffRepository.findById(assignedManager.getId())).thenReturn(Optional.of(assignedManager));
        when(leaveApproverRepository.findActiveApproversForStaff(employee, pending.getLeaveDate()))
                .thenReturn(List.of(scenario.approvers().getFirst()));
        when(leaveApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveApplication approved = leaveApplicationService.approve("request-1", assignedManager.getId());
        assertThat(approved.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(approved.getApprover().getId()).isEqualTo(assignedManager.getId());

        pending.setStatus(LeaveStatus.PENDING);
        when(staffRepository.findById(unrelatedManager.getId())).thenReturn(Optional.of(unrelatedManager));
        assertThatThrownBy(() -> leaveApplicationService.approve("request-1", unrelatedManager.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not pending for this approver");
    }

    @Test
    void assignedManagerCanRejectPendingRequest() {
        Staff employee = scenario.staff("staff001");
        Staff manager = scenario.staff("manager01");
        LeaveApplication pending = pendingApplication(employee);
        when(leaveApplicationRepository.findById("request-1")).thenReturn(Optional.of(pending));
        when(staffRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(leaveApproverRepository.findActiveApproversForStaff(employee, pending.getLeaveDate()))
                .thenReturn(List.of(scenario.approvers().getFirst()));
        when(leaveApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveApplication rejected = leaveApplicationService.reject("request-1", manager.getId());
        assertThat(rejected.getStatus()).isEqualTo(LeaveStatus.DENIED);
        assertThat(rejected.getApprover().getId()).isEqualTo(manager.getId());
    }

    @Test
    void weekendAndPublicHolidayAreNotCharged() {
        Staff staff = scenario.staff("staff001");
        LeaveType leaveType = scenario.leaveTypes().getFirst();
        LocalDate publicHoliday = LocalDate.of(2026, 9, 9);
        LeaveCalendar calendar = LeaveCalendar.builder()
                .id("sg-2026")
                .tenantId(scenario.tenant().getId())
                .jurisdictionId("SG")
                .start(LocalDate.of(2026, 1, 1))
                .end(LocalDate.of(2026, 12, 31))
                .publicHolidays(List.of(PublicHoliday.builder()
                        .holidayDate(publicHoliday).holidayName("E2E holiday").build()))
                .build();
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById(leaveType.getId())).thenReturn(Optional.of(leaveType));
        when(leaveCalendarService.getCalendarFor("SG", LocalDate.of(2026, 9, 7))).thenReturn(Optional.of(calendar));
        when(leaveCalendarService.getCalendarFor("SG", LocalDate.of(2026, 9, 8))).thenReturn(Optional.of(calendar));
        when(leaveCalendarService.getCalendarFor("SG", LocalDate.of(2026, 9, 9))).thenReturn(Optional.of(calendar));
        when(leaveCalendarService.getCalendarFor("SG", LocalDate.of(2026, 9, 10))).thenReturn(Optional.of(calendar));
        when(leaveCalendarService.getCalendarFor("SG", LocalDate.of(2026, 9, 11))).thenReturn(Optional.of(calendar));
        when(leaveApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LeaveApplication> result = leaveApplicationService.apply(
                request(staff, leaveType, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13)), null);

        assertThat(result).extracting(LeaveApplication::getLeaveDate).containsExactly(
                LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11));
    }

    @Test
    void tenantBoundaryRejectsLeaveTypeFromAnotherTenant() {
        Staff staff = scenario.staff("staff001");
        LeaveType otherTenantType = LeaveType.builder()
                .id("other:SG:ANNUAL_LEAVE").name("Annual Leave").used(true)
                .tenantId("OTHER-TENANT").jurisdictionId("SG").build();
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById(otherTenantType.getId())).thenReturn(Optional.of(otherTenantType));

        assertThatThrownBy(() -> leaveApplicationService.apply(
                request(staff, otherTenantType, REFERENCE_DATE.plusDays(2), REFERENCE_DATE.plusDays(2)), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to the staff tenant");
    }

    @Test
    void applicableStaffJurisdictionSelectsCalendarInMultiJurisdictionContext() {
        Staff staff = ScenarioDataFactory.withJurisdiction(scenario.staff("staff004"), "AU-NSW");
        LeaveType leaveType = scenario.leaveTypes().getFirst();
        LocalDate date = LocalDate.of(2026, 9, 14);
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById(leaveType.getId())).thenReturn(Optional.of(leaveType));
        when(leaveApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        leaveApplicationService.apply(request(staff, leaveType, date, date), null);

        verify(leaveCalendarService).getCalendarFor("AU-NSW", date);
        verify(leaveCalendarService, never()).getCalendarFor("SG", date);
    }

    private LeaveApplicationRequest request(Staff staff, LeaveType leaveType, LocalDate from, LocalDate to) {
        return LeaveApplicationRequest.builder()
                .staffId(staff.getId())
                .fromDate(from)
                .toDate(to)
                .leaveTypeId(leaveType.getId())
                .leaveDuration(LeaveDuration.FULL)
                .status(LeaveStatus.PENDING)
                .build();
    }

    private LeaveApplication pendingApplication(Staff employee) {
        return LeaveApplication.builder()
                .id("request-1")
                .staff(employee)
                .leaveDate(REFERENCE_DATE.plusDays(2))
                .leaveType(scenario.leaveTypes().getFirst())
                .leaveDuration(LeaveDuration.FULL)
                .status(LeaveStatus.PENDING)
                .applicationDate(REFERENCE_DATE)
                .tenantId(scenario.tenant().getId())
                .build();
    }
}

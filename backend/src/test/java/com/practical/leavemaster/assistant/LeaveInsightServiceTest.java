package com.practical.leavemaster.assistant;

import com.practical.leavemaster.leaveapplication.*;
import com.practical.leavemaster.leaveapprover.*;
import com.practical.leavemaster.staff.*;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LeaveInsightServiceTest {
    @Test
    void returnsDeterministicEvidenceForMissingConfigurationAndInvalidLeaveDates() {
        StaffRepository staffRepo=mock(StaffRepository.class);
        LeaveApplicationRepository appRepo=mock(LeaveApplicationRepository.class);
        LeaveApproverRepository approverRepo=mock(LeaveApproverRepository.class);
        Staff staff=Staff.builder().id("S1").name("One").tenantId("T1").joinDate(LocalDate.of(2026,2,1))
                .termDate(LocalDate.of(2026,10,31)).workSchedule(List.of()).leaveEntitlements(List.of()).build();
        when(staffRepo.findById("S1")).thenReturn(Optional.of(staff));
        when(approverRepo.findActiveApproversForStaff(staff,LocalDate.of(2026,9,1))).thenReturn(List.of());
        when(appRepo.findByStaff(staff)).thenReturn(List.of(
                LeaveApplication.builder().staff(staff).leaveDate(LocalDate.of(2026,1,20)).build(),
                LeaveApplication.builder().staff(staff).leaveDate(LocalDate.of(2026,11,2)).build()));

        var findings=new LeaveInsightService(staffRepo,appRepo,approverRepo)
                .findStaffAnomalies("S1",LocalDate.of(2026,9,1));

        assertThat(findings).extracting(LeaveInsightService.StaffLeaveFinding::code)
                .containsExactly("MISSING_JURISDICTION","INVALID_WORK_SCHEDULE","MISSING_APPROVER",
                        "MISSING_ENTITLEMENT","LEAVE_BEFORE_JOIN_DATE","LEAVE_AFTER_TERMINATION");
        assertThat(findings).allSatisfy(f -> { assertThat(f.reason()).isNotBlank(); assertThat(f.evidence()).isNotBlank(); });
    }

    @Test
    void returnsOnlyAssignedApproverPendingActions() {
        StaffRepository staffRepo=mock(StaffRepository.class);
        LeaveApplicationRepository appRepo=mock(LeaveApplicationRepository.class);
        LeaveApproverRepository approverRepo=mock(LeaveApproverRepository.class);
        Staff staff=Staff.builder().id("S1").name("One").tenantId("T1").build();
        when(appRepo.findPendingByApproverId("M1")).thenReturn(List.of(
                LeaveApplication.builder().id("A1").staff(staff).tenantId("T1").leaveDate(LocalDate.of(2026,9,30)).status(LeaveStatus.PENDING).build()));
        var actions=new LeaveInsightService(staffRepo,appRepo,approverRepo).pendingActionsForApprover("M1");
        assertThat(actions).singleElement().satisfies(a -> { assertThat(a.applicationId()).isEqualTo("A1"); assertThat(a.staffId()).isEqualTo("S1"); });
    }

    @Test
    void teamLeaveFiltersCrossTenantRecords() {
        StaffRepository staffRepo=mock(StaffRepository.class);
        LeaveApplicationRepository appRepo=mock(LeaveApplicationRepository.class);
        LeaveApproverRepository approverRepo=mock(LeaveApproverRepository.class);
        Staff manager=Staff.builder().id("M1").tenantId("T1").build();
        Staff own=Staff.builder().id("S1").name("Own").tenantId("T1").build();
        Staff other=Staff.builder().id("S2").name("Other").tenantId("T2").build();
        when(staffRepo.findById("M1")).thenReturn(Optional.of(manager));
        when(approverRepo.findByApprover(manager)).thenReturn(List.of(
                LeaveApprover.builder().staff(own).approver(manager).build(),LeaveApprover.builder().staff(other).approver(manager).build()));
        when(appRepo.findByStaffAndLeaveDateBetween(eq(own),any(),any())).thenReturn(List.of(
                LeaveApplication.builder().staff(own).leaveDate(LocalDate.of(2026,9,30)).status(LeaveStatus.APPROVED).build()));
        when(appRepo.findByStaffAndLeaveDateBetween(eq(other),any(),any())).thenReturn(List.of(
                LeaveApplication.builder().staff(other).leaveDate(LocalDate.of(2026,9,30)).status(LeaveStatus.APPROVED).build()));
        var leaves=new LeaveInsightService(staffRepo,appRepo,approverRepo)
                .upcomingLeaveForApprover("M1",LocalDate.of(2026,9,29),LocalDate.of(2026,10,5));
        assertThat(leaves).extracting(LeaveInsightService.UpcomingLeave::staffId).containsExactly("S1");
    }

    @Test
    void returnsEmptyAnomaliesForHealthyStaff() {
        StaffRepository staffRepo=mock(StaffRepository.class);
        LeaveApplicationRepository appRepo=mock(LeaveApplicationRepository.class);
        LeaveApproverRepository approverRepo=mock(LeaveApproverRepository.class);
        var type=com.practical.leavemaster.leavetype.LeaveType.builder().id("AL").name("Annual Leave").build();
        var entitlement=com.practical.leavemaster.leaveentitlement.LeaveEntitlement.builder().leaveType(type).policyId("P1")
                .from(LocalDate.of(2026,1,1)).to(LocalDate.of(2026,12,31)).entitlement(java.math.BigDecimal.TEN).build();
        Staff staff=Staff.builder().id("S1").name("One").tenantId("T1").jurisdictionId("SG").joinDate(LocalDate.of(2026,1,1))
                .workSchedule(List.of(WorkScheduleDay.builder().dayOfWeek(DayOfWeek.MONDAY).daySchedule(DaySchedule.FULL).build()))
                .leaveEntitlements(List.of(entitlement)).build();
        when(staffRepo.findById("S1")).thenReturn(Optional.of(staff));
        when(approverRepo.findActiveApproversForStaff(eq(staff),any())).thenReturn(List.of(LeaveApprover.builder().staff(staff).approver(Staff.builder().id("M").build()).build()));
        when(appRepo.findByStaff(staff)).thenReturn(List.of());
        assertThat(new LeaveInsightService(staffRepo,appRepo,approverRepo).findStaffAnomalies("S1",LocalDate.of(2026,9,1))).isEmpty();
    }

    @Test
    void excludesDeniedAndDraftLeaveFromTeamView() {
        StaffRepository staffRepo=mock(StaffRepository.class);
        LeaveApplicationRepository appRepo=mock(LeaveApplicationRepository.class);
        LeaveApproverRepository approverRepo=mock(LeaveApproverRepository.class);
        Staff manager=Staff.builder().id("M1").tenantId("T1").build();
        Staff own=Staff.builder().id("S1").name("Own").tenantId("T1").build();
        when(staffRepo.findById("M1")).thenReturn(Optional.of(manager));
        when(approverRepo.findByApprover(manager)).thenReturn(List.of(LeaveApprover.builder().staff(own).approver(manager).build()));
        when(appRepo.findByStaffAndLeaveDateBetween(eq(own),any(),any())).thenReturn(List.of(
                LeaveApplication.builder().staff(own).leaveDate(LocalDate.of(2026,9,30)).status(LeaveStatus.DENIED).build(),
                LeaveApplication.builder().staff(own).leaveDate(LocalDate.of(2026,10,1)).status(LeaveStatus.DRAFT).build()));
        assertThat(new LeaveInsightService(staffRepo,appRepo,approverRepo)
                .upcomingLeaveForApprover("M1",LocalDate.of(2026,9,29),LocalDate.of(2026,10,5))).isEmpty();
    }

    @Test
    void pendingActionsExcludeNonPendingRowsDefensively() {
        LeaveApplicationRepository appRepo=mock(LeaveApplicationRepository.class);
        Staff staff=Staff.builder().id("S1").tenantId("T1").build();
        when(appRepo.findPendingByApproverId("M1")).thenReturn(List.of(
                LeaveApplication.builder().staff(staff).status(LeaveStatus.APPROVED).build()));
        assertThat(new LeaveInsightService(mock(StaffRepository.class),appRepo,mock(LeaveApproverRepository.class))
                .pendingActionsForApprover("M1")).isEmpty();
    }
}

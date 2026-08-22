package com.practical.leavemaster.leaveentitlement;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRequest;
import com.practical.leavemaster.leaveapplication.LeaveDuration;
import com.practical.leavemaster.leaveapplication.LeaveStatus;
import com.practical.leavemaster.leaveeligibility.LeaveEligibilityFactService;
import com.practical.leavemaster.leaveeligibility.QualifyingEventStatus;
import com.practical.leavemaster.leaveeligibility.QualifyingLeaveEvent;
import com.practical.leavemaster.leaveeligibility.QualifyingLeaveEventWriteRequest;
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyResolutionService;
import com.practical.leavemaster.leaveentitlementpolicy.LeavePolicyModel;
import com.practical.leavemaster.leaveentitlementpolicy.PolicyResolutionResult;
import com.practical.leavemaster.leaveentitlementpolicy.ProrationMethod;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
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
class EventLeaveEntitlementServiceTest {

    @Mock EventLeaveEntitlementRepository entitlementRepository;
    @Mock LeaveEntitlementPolicyRepository policyRepository;
    @Mock LeaveEntitlementPolicyResolutionService resolutionService;
    @Mock LeaveEligibilityFactService factService;
    @Mock StaffRepository staffRepository;
    @Mock LeaveTypeRepository leaveTypeRepository;
    @Mock LeaveApplicationRepository applicationRepository;

    @InjectMocks EventLeaveEntitlementService service;

    @Test
    void ordinaryLeaveDoesNotRequireAnEvent() {
        Staff staff = staff();
        LeaveType leaveType = leaveType();
        when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-1", "event-leave"))
                .thenReturn(List.of(annualPolicy()));

        Optional<EventLeaveEntitlement> result = service.prepareForRequest(staff, leaveType,
                LeaveApplicationRequest.builder().fromDate(LocalDate.of(2026, 8, 10)).build());

        assertThat(result).isEmpty();
        verify(factService, never()).createEvent(any(), any());
    }

    @Test
    void requestFirstCreatesEventAndActiveEntitlementIdempotently() {
        Staff staff = staff();
        LeaveType leaveType = leaveType();
        LeaveEntitlementPolicy policy = eventPolicy(false);
        LocalDate callUp = LocalDate.of(2026, 9, 10);
        QualifyingLeaveEvent event = event("event-1", QualifyingEventStatus.VERIFIED, callUp);
        EventLeaveEntitlement saved = entitlement("ent-1", EventLeaveEntitlementStatus.ACTIVE, BigDecimal.ZERO);

        when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-1", "event-leave"))
                .thenReturn(List.of(policy));
        when(resolutionService.resolve(staff, "event-leave", callUp)).thenReturn(selected());
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(policy));
        when(factService.findEvents("staff-1")).thenReturn(List.of());
        when(factService.createEvent(any(), any(QualifyingLeaveEventWriteRequest.class))).thenReturn(event);
        when(entitlementRepository.findByQualifyingEventIdAndPolicyId("event-1", "policy-1"))
                .thenReturn(Optional.empty());
        when(entitlementRepository.save(any(EventLeaveEntitlement.class))).thenAnswer(inv -> {
            EventLeaveEntitlement value = inv.getArgument(0);
            value.setId("ent-1");
            return value;
        });

        EventLeaveEntitlement result = service.prepareForRequest(staff, leaveType,
                LeaveApplicationRequest.builder().eventDate(callUp).eventStartDate(callUp)
                        .eventEndDate(callUp.plusDays(4)).eventExternalReference("CALL-1").build()).orElseThrow();

        assertThat(result.getId()).isEqualTo("ent-1");
        assertThat(result.getGrantedAmount()).isEqualByComparingTo("5");
        assertThat(result.getValidFrom()).isEqualTo(callUp);
        assertThat(result.getValidTo()).isEqualTo(callUp.plusDays(4));
        assertThat(result.getStatus()).isEqualTo(EventLeaveEntitlementStatus.ACTIVE);
        verify(factService).createEvent("staff-1", new QualifyingLeaveEventWriteRequest(
                null, "MILITARY_CALL_UP", callUp, callUp, callUp.plusDays(4), "CALL-1", null,
                QualifyingEventStatus.VERIFIED));

        when(entitlementRepository.findByQualifyingEventIdAndPolicyId("event-1", "policy-1"))
                .thenReturn(Optional.of(saved));
        assertThat(service.generate("staff-1", "event-leave", "event-1").getId()).isEqualTo("ent-1");
    }

    @Test
    void reservesAndReleasesOnlyWithinActiveEntitlement() {
        EventLeaveEntitlement entitlement = entitlement("ent-1", EventLeaveEntitlementStatus.ACTIVE, new BigDecimal("1.0"));
        when(entitlementRepository.save(entitlement)).thenReturn(entitlement);

        service.reserve(entitlement, new BigDecimal("1.5"), LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11));
        assertThat(entitlement.getUsedAmount()).isEqualByComparingTo("2.5");

        when(entitlementRepository.findById("ent-1")).thenReturn(Optional.of(entitlement));
        service.release("ent-1", new BigDecimal("0.5"));
        assertThat(entitlement.getUsedAmount()).isEqualByComparingTo("2.0");

        entitlement.setStatus(EventLeaveEntitlementStatus.PENDING_VERIFICATION);
        assertThatThrownBy(() -> service.reserve(entitlement, BigDecimal.ONE,
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("verification");
    }

    @Test
    void verifiedEventActivatesPendingRequestsAndReservesOnce() {
        Staff staff = staff();
        LeaveType leaveType = leaveType();
        LeaveEntitlementPolicy policy = eventPolicy(true);
        LocalDate eventDate = LocalDate.of(2026, 10, 1);
        QualifyingLeaveEvent event = event("event-1", QualifyingEventStatus.VERIFIED, eventDate);
        EventLeaveEntitlement pending = entitlement("ent-1", EventLeaveEntitlementStatus.PENDING_VERIFICATION, BigDecimal.ZERO);
        LeaveApplication application = LeaveApplication.builder()
                .id("app-1").leaveDate(eventDate).leaveDuration(LeaveDuration.FULL)
                .status(LeaveStatus.PENDING_VERIFICATION).eventEntitlementId("ent-1").build();

        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("event-leave")).thenReturn(Optional.of(leaveType));
        when(factService.findEvent("staff-1", "event-1")).thenReturn(event);
        when(resolutionService.resolve(staff, "event-leave", eventDate)).thenReturn(selected());
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(policy));
        when(entitlementRepository.findByQualifyingEventIdAndPolicyId("event-1", "policy-1"))
                .thenReturn(Optional.of(pending));
        when(entitlementRepository.save(any(EventLeaveEntitlement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.findAllByEventEntitlementIdAndStatus("ent-1", LeaveStatus.PENDING_VERIFICATION))
                .thenReturn(List.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        EventLeaveEntitlement result = service.generate("staff-1", "event-leave", "event-1");

        assertThat(result.getStatus()).isEqualTo(EventLeaveEntitlementStatus.ACTIVE);
        assertThat(result.getUsedAmount()).isEqualByComparingTo("1");
        assertThat(application.getStatus()).isEqualTo(LeaveStatus.PENDING);
    }

    @Test
    void rejectsWrongEventAndOverdraw() {
        LeaveEntitlementPolicy policy = eventPolicy(false);
        QualifyingLeaveEvent wrong = event("event-1", QualifyingEventStatus.VERIFIED, LocalDate.of(2026, 9, 10));
        wrong.setEventTypeCode("BIRTH");
        Staff staff = staff();
        LeaveType leaveType = leaveType();

        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("event-leave")).thenReturn(Optional.of(leaveType));
        when(factService.findEvent("staff-1", "event-1")).thenReturn(wrong);
        when(resolutionService.resolve(staff, "event-leave", wrong.getEventDate())).thenReturn(selected());
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> service.generate("staff-1", "event-leave", "event-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("does not match");

        EventLeaveEntitlement full = entitlement("ent-2", EventLeaveEntitlementStatus.ACTIVE, new BigDecimal("5"));
        assertThatThrownBy(() -> service.reserve(full, BigDecimal.ONE,
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exceeds");
    }

    private Staff staff() {
        return Staff.builder().id("staff-1").name("Alice").tenantId("tenant-1")
                .joinDate(LocalDate.of(2025, 1, 1)).build();
    }

    private LeaveType leaveType() {
        return LeaveType.builder().id("event-leave").name("Event Leave").tenantId("tenant-1").build();
    }

    private LeaveEntitlementPolicy eventPolicy(boolean verify) {
        return LeaveEntitlementPolicy.builder().id("policy-1").tenantId("tenant-1").leaveTypeId("event-leave")
                .name("Event leave").active(true).priority(10).policyModel(LeavePolicyModel.EVENT_BASED)
                .qualifyingEventTypeCode("MILITARY_CALL_UP").eventRequiresVerification(verify)
                .entitlementUnit(EntitlementUnit.DAYS).entitlementAmount(new BigDecimal("5"))
                .accrualMethod(AccrualMethod.NONE).prorationMethod(ProrationMethod.NONE)
                .effectiveFrom(LocalDate.of(2026, 1, 1)).build();
    }

    private LeaveEntitlementPolicy annualPolicy() {
        return LeaveEntitlementPolicy.builder().id("annual-policy").tenantId("tenant-1")
                .leaveTypeId("event-leave").policyModel(LeavePolicyModel.ANNUAL_ENTITLEMENT).build();
    }

    private QualifyingLeaveEvent event(String id, QualifyingEventStatus status, LocalDate date) {
        return QualifyingLeaveEvent.builder().id(id).tenantId("tenant-1").staffId("staff-1")
                .eventTypeCode("MILITARY_CALL_UP").eventDate(date).startDate(date).endDate(date.plusDays(4))
                .status(status).build();
    }

    private EventLeaveEntitlement entitlement(String id, EventLeaveEntitlementStatus status, BigDecimal used) {
        return EventLeaveEntitlement.builder().id(id).tenantId("tenant-1").staffId("staff-1")
                .leaveTypeId("event-leave").policyId("policy-1").qualifyingEventId("event-1")
                .validFrom(LocalDate.of(2026, 9, 1)).validTo(LocalDate.of(2026, 12, 31))
                .grantedAmount(new BigDecimal("5")).usedAmount(used).status(status).build();
    }

    private PolicyResolutionResult selected() {
        return new PolicyResolutionResult("staff-1", "event-leave", "policy-1", false, "selected", List.of());
    }
}

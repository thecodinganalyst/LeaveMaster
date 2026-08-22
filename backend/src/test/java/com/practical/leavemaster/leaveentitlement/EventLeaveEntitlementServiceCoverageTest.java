package com.practical.leavemaster.leaveentitlement;

import com.practical.leavemaster.leaveapplication.LeaveApplicationRequest;
import com.practical.leavemaster.leaveeligibility.LeaveEligibilityFactService;
import com.practical.leavemaster.leaveeligibility.QualifyingEventStatus;
import com.practical.leavemaster.leaveeligibility.QualifyingLeaveEvent;
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
import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
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
class EventLeaveEntitlementServiceCoverageTest {

    @Mock EventLeaveEntitlementRepository entitlementRepository;
    @Mock LeaveEntitlementPolicyRepository policyRepository;
    @Mock LeaveEntitlementPolicyResolutionService resolutionService;
    @Mock LeaveEligibilityFactService factService;
    @Mock StaffRepository staffRepository;
    @Mock LeaveTypeRepository leaveTypeRepository;
    @Mock LeaveApplicationRepository applicationRepository;
    @Mock EventEntitlementAmountResolver amountResolver;

    @InjectMocks EventLeaveEntitlementService service;

    @Test
    void findForStaffValidatesInputAndReturnsTenantScopedEntitlements() {
        Staff staff = staff("tenant-1");
        EventLeaveEntitlement entitlement = entitlement("ent-1");
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(entitlementRepository.findAllByTenantIdAndStaffIdAndLeaveTypeIdOrderByValidFromAsc(
                "tenant-1", "staff-1", "event-leave")).thenReturn(List.of(entitlement));

        assertThat(service.findForStaff("staff-1", "event-leave")).containsExactly(entitlement);
        assertThatThrownBy(() -> service.findForStaff("staff-1", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leaveTypeId is required");

        when(staffRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findForStaff("missing", "event-leave"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown staff id");
    }

    @Test
    void reserveByIdAndReleaseGuardPathsAreCovered() {
        EventLeaveEntitlement entitlement = entitlement("ent-1");
        when(entitlementRepository.findById("ent-1")).thenReturn(Optional.of(entitlement));
        when(entitlementRepository.save(entitlement)).thenReturn(entitlement);

        EventLeaveEntitlement reserved = service.reserve("ent-1", BigDecimal.ONE,
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10));
        assertThat(reserved.getUsedAmount()).isEqualByComparingTo("1");

        service.release(null, BigDecimal.ONE);
        service.release("ent-1", null);
        service.release("ent-1", BigDecimal.ZERO);

        when(entitlementRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reserve("missing", BigDecimal.ONE,
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event entitlement not found");
    }

    @Test
    void prepareCanUseExplicitExistingEvent() {
        Staff staff = staff("tenant-1");
        LeaveType leaveType = leaveType("tenant-1");
        LeaveEntitlementPolicy policy = eventPolicy(false);
        QualifyingLeaveEvent event = event("event-1", QualifyingEventStatus.VERIFIED);
        EventLeaveEntitlement existing = entitlement("ent-1");
        when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-1", "event-leave"))
                .thenReturn(List.of(policy));
        when(resolutionService.resolve(staff, "event-leave", event.getEventDate())).thenReturn(selected("policy-1"));
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(policy));
        when(factService.findEvent("staff-1", "event-1")).thenReturn(event);
        when(entitlementRepository.findByQualifyingEventIdAndPolicyId("event-1", "policy-1"))
                .thenReturn(Optional.of(existing));

        Optional<EventLeaveEntitlement> result = service.prepareForRequest(staff, leaveType,
                LeaveApplicationRequest.builder()
                        .fromDate(event.getEventDate())
                        .qualifyingEventId("event-1")
                        .build());

        assertThat(result).contains(existing);
        verify(factService, never()).createEvent(any(), any());
    }

    @Test
    void prepareRequiresEventDateWhenNoExistingEventIsSelected() {
        Staff staff = staff("tenant-1");
        LeaveType leaveType = leaveType("tenant-1");
        LeaveEntitlementPolicy policy = eventPolicy(false);
        LocalDate leaveDate = LocalDate.of(2026, 9, 10);
        when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-1", "event-leave"))
                .thenReturn(List.of(policy));
        when(resolutionService.resolve(staff, "event-leave", leaveDate)).thenReturn(selected("policy-1"));
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> service.prepareForRequest(staff, leaveType,
                LeaveApplicationRequest.builder().fromDate(leaveDate).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventDate is required");
    }

    @Test
    void prepareReusesMatchingEventAndNormalizesReference() {
        Staff staff = staff("tenant-1");
        LeaveType leaveType = leaveType("tenant-1");
        LeaveEntitlementPolicy policy = eventPolicy(false);
        QualifyingLeaveEvent event = event("event-1", QualifyingEventStatus.VERIFIED);
        event.setExternalReference("CALL-1");
        EventLeaveEntitlement existing = entitlement("ent-1");
        when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-1", "event-leave"))
                .thenReturn(List.of(policy));
        when(resolutionService.resolve(staff, "event-leave", event.getEventDate())).thenReturn(selected("policy-1"));
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(policy));
        when(factService.findEvents("staff-1")).thenReturn(List.of(event));
        when(entitlementRepository.findByQualifyingEventIdAndPolicyId("event-1", "policy-1"))
                .thenReturn(Optional.of(existing));

        Optional<EventLeaveEntitlement> result = service.prepareForRequest(staff, leaveType,
                LeaveApplicationRequest.builder()
                        .eventDate(event.getEventDate())
                        .eventTypeCode(" military_call_up ")
                        .eventExternalReference(" CALL-1 ")
                        .build());

        assertThat(result).contains(existing);
        verify(factService, never()).createEvent(any(), any());
    }

    @Test
    void policyResolutionFailuresAreReportedClearly() {
        Staff staff = staff("tenant-1");
        LeaveType leaveType = leaveType("tenant-1");
        LocalDate date = LocalDate.of(2026, 9, 10);
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("event-leave")).thenReturn(Optional.of(leaveType));
        when(factService.findEvent("staff-1", "event-1")).thenReturn(event("event-1", QualifyingEventStatus.VERIFIED));

        when(resolutionService.resolve(staff, "event-leave", date))
                .thenReturn(new PolicyResolutionResult("staff-1", "event-leave", null, true, "ambiguous", List.of()));
        assertThatThrownBy(() -> service.generate("staff-1", "event-leave", "event-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Multiple event-based policies");

        when(resolutionService.resolve(staff, "event-leave", date))
                .thenReturn(new PolicyResolutionResult("staff-1", "event-leave", null, false, "none", List.of()));
        assertThatThrownBy(() -> service.generate("staff-1", "event-leave", "event-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("No matching event-based");

        when(resolutionService.resolve(staff, "event-leave", date)).thenReturn(selected("missing-policy"));
        when(policyRepository.findById("missing-policy")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.generate("staff-1", "event-leave", "event-1"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no longer exists");

        LeaveEntitlementPolicy annual = eventPolicy(false);
        annual.setPolicyModel(LeavePolicyModel.ANNUAL_ENTITLEMENT);
        when(resolutionService.resolve(staff, "event-leave", date)).thenReturn(selected("policy-1"));
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(annual));
        assertThatThrownBy(() -> service.generate("staff-1", "event-leave", "event-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not event-based");
    }

    @Test
    void generationRejectsInvalidEventStatusAndInvalidPeriod() {
        Staff staff = staff("tenant-1");
        LeaveType leaveType = leaveType("tenant-1");
        LeaveEntitlementPolicy policy = eventPolicy(false);
        LocalDate date = LocalDate.of(2026, 9, 10);
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("event-leave")).thenReturn(Optional.of(leaveType));
        when(resolutionService.resolve(staff, "event-leave", date)).thenReturn(selected("policy-1"));
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(policy));

        QualifyingLeaveEvent rejected = event("rejected", QualifyingEventStatus.REJECTED);
        when(factService.findEvent("staff-1", "rejected")).thenReturn(rejected);
        assertThatThrownBy(() -> service.generate("staff-1", "event-leave", "rejected"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not valid");

        QualifyingLeaveEvent invalidPeriod = event("bad-period", QualifyingEventStatus.VERIFIED);
        invalidPeriod.setStartDate(date.plusDays(2));
        invalidPeriod.setEndDate(date.plusDays(1));
        when(factService.findEvent("staff-1", "bad-period")).thenReturn(invalidPeriod);
        when(entitlementRepository.findByQualifyingEventIdAndPolicyId("bad-period", "policy-1"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.generate("staff-1", "event-leave", "bad-period"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("period is invalid");
    }

    @Test
    void generationUsesPolicyValidityOffsetsWhenEventRangeIsMissing() {
        Staff staff = staff("tenant-1");
        LeaveType leaveType = leaveType("tenant-1");
        LeaveEntitlementPolicy policy = eventPolicy(false);
        policy.setEventValidityDaysBefore(2);
        policy.setEventValidityDaysAfter(3);
        QualifyingLeaveEvent event = event("event-1", QualifyingEventStatus.VERIFIED);
        event.setStartDate(null);
        event.setEndDate(null);
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("event-leave")).thenReturn(Optional.of(leaveType));
        when(factService.findEvent("staff-1", "event-1")).thenReturn(event);
        when(resolutionService.resolve(staff, "event-leave", event.getEventDate())).thenReturn(selected("policy-1"));
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(policy));
        when(entitlementRepository.findByQualifyingEventIdAndPolicyId("event-1", "policy-1"))
                .thenReturn(Optional.empty());
        when(amountResolver.resolve(staff, policy, event)).thenReturn(new BigDecimal("5"));
        when(entitlementRepository.save(any(EventLeaveEntitlement.class))).thenAnswer(inv -> inv.getArgument(0));

        EventLeaveEntitlement generated = service.generate("staff-1", "event-leave", "event-1");

        assertThat(generated.getGrantedAmount()).isEqualByComparingTo("5");
        assertThat(generated.getValidFrom()).isEqualTo(event.getEventDate().minusDays(2));
        assertThat(generated.getValidTo()).isEqualTo(event.getEventDate().plusDays(3));
    }

    @Test
    void reserveRejectsOutOfWindowAndTenantMismatchIsRejected() {
        EventLeaveEntitlement entitlement = entitlement("ent-1");
        assertThatThrownBy(() -> service.reserve(entitlement, BigDecimal.ONE,
                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("outside");
        assertThatThrownBy(() -> service.reserve(entitlement, BigDecimal.ZERO,
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exceeds");

        Staff staff = staff("tenant-1");
        LeaveType foreign = leaveType("tenant-2");
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("event-leave")).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.generate("staff-1", "event-leave", "event-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("staff tenant");
    }

    private Staff staff(String tenantId) {
        return Staff.builder().id("staff-1").name("Alice").tenantId(tenantId)
                .joinDate(LocalDate.of(2025, 1, 1)).build();
    }

    private LeaveType leaveType(String tenantId) {
        return LeaveType.builder().id("event-leave").name("Event Leave").tenantId(tenantId).build();
    }

    private LeaveEntitlementPolicy eventPolicy(boolean verify) {
        return LeaveEntitlementPolicy.builder().id("policy-1").tenantId("tenant-1").leaveTypeId("event-leave")
                .name("Event leave").active(true).priority(10).policyModel(LeavePolicyModel.EVENT_BASED)
                .qualifyingEventTypeCode("MILITARY_CALL_UP").eventRequiresVerification(verify)
                .entitlementUnit(EntitlementUnit.DAYS).entitlementAmount(new BigDecimal("5"))
                .accrualMethod(AccrualMethod.NONE).prorationMethod(ProrationMethod.NONE)
                .effectiveFrom(LocalDate.of(2026, 1, 1)).build();
    }

    private QualifyingLeaveEvent event(String id, QualifyingEventStatus status) {
        LocalDate date = LocalDate.of(2026, 9, 10);
        return QualifyingLeaveEvent.builder().id(id).tenantId("tenant-1").staffId("staff-1")
                .eventTypeCode("MILITARY_CALL_UP").eventDate(date).startDate(date).endDate(date.plusDays(4))
                .status(status).build();
    }

    private EventLeaveEntitlement entitlement(String id) {
        return EventLeaveEntitlement.builder().id(id).tenantId("tenant-1").staffId("staff-1")
                .leaveTypeId("event-leave").policyId("policy-1").qualifyingEventId("event-1")
                .validFrom(LocalDate.of(2026, 9, 1)).validTo(LocalDate.of(2026, 12, 31))
                .grantedAmount(new BigDecimal("5")).usedAmount(BigDecimal.ZERO)
                .status(EventLeaveEntitlementStatus.ACTIVE).build();
    }

    private PolicyResolutionResult selected(String policyId) {
        return new PolicyResolutionResult("staff-1", "event-leave", policyId, false, "selected", List.of());
    }
}

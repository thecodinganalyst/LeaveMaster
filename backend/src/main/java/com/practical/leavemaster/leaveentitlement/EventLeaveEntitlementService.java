package com.practical.leavemaster.leaveentitlement;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRequest;
import com.practical.leavemaster.leaveapplication.LeaveStatus;
import com.practical.leavemaster.leaveeligibility.LeaveEligibilityFactService;
import com.practical.leavemaster.leaveeligibility.QualifyingEventStatus;
import com.practical.leavemaster.leaveeligibility.QualifyingLeaveEvent;
import com.practical.leavemaster.leaveeligibility.QualifyingLeaveEventWriteRequest;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyResolutionService;
import com.practical.leavemaster.leaveentitlementpolicy.LeavePolicyModel;
import com.practical.leavemaster.leaveentitlementpolicy.PolicyResolutionResult;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventLeaveEntitlementService {

    private final EventLeaveEntitlementRepository entitlementRepository;
    private final LeaveEntitlementPolicyRepository policyRepository;
    private final LeaveEntitlementPolicyResolutionService resolutionService;
    private final LeaveEligibilityFactService factService;
    private final StaffRepository staffRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveApplicationRepository applicationRepository;

    public List<EventLeaveEntitlement> findForStaff(String staffId, String leaveTypeId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown staff id: " + staffId));
        if (leaveTypeId == null || leaveTypeId.isBlank()) {
            throw new IllegalArgumentException("leaveTypeId is required");
        }
        return entitlementRepository.findAllByTenantIdAndStaffIdAndLeaveTypeIdOrderByValidFromAsc(
                staff.getTenantId(), staffId, leaveTypeId);
    }

    @Transactional
    public EventLeaveEntitlement generate(String staffId, String leaveTypeId, String eventId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown staff id: " + staffId));
        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown leave type id: " + leaveTypeId));
        requireSameTenant(staff, leaveType);
        QualifyingLeaveEvent event = factService.findEvent(staffId, eventId);
        LeaveEntitlementPolicy policy = requireEventPolicy(staff, leaveTypeId, event.getEventDate());
        return generate(staff, leaveType, policy, event);
    }

    @Transactional
    public Optional<EventLeaveEntitlement> prepareForRequest(
            Staff staff, LeaveType leaveType, LeaveApplicationRequest request) {
        List<LeaveEntitlementPolicy> configured = policyRepository
                .findAllByTenantIdAndLeaveTypeIdAndActiveTrue(staff.getTenantId(), leaveType.getId());
        boolean hasEventPolicy = configured.stream()
                .anyMatch(policy -> policy.getPolicyModel() == LeavePolicyModel.EVENT_BASED);
        if (!hasEventPolicy) {
            return Optional.empty();
        }

        LocalDate effectiveDate = request.getEventDate() != null ? request.getEventDate() : request.getFromDate();
        LeaveEntitlementPolicy policy = requireEventPolicy(staff, leaveType.getId(), effectiveDate);
        QualifyingLeaveEvent event = resolveOrCreateEvent(staff, policy, request);
        return Optional.of(generate(staff, leaveType, policy, event));
    }

    @Transactional
    public EventLeaveEntitlement reserve(EventLeaveEntitlement entitlement, BigDecimal amount,
                                         LocalDate requestFrom, LocalDate requestTo) {
        if (entitlement.getStatus() != EventLeaveEntitlementStatus.ACTIVE) {
            throw new IllegalArgumentException("Event-based entitlement is awaiting verification");
        }
        if (requestFrom.isBefore(entitlement.getValidFrom()) || requestTo.isAfter(entitlement.getValidTo())) {
            throw new IllegalArgumentException("Requested leave is outside the qualifying event entitlement period");
        }
        BigDecimal available = entitlement.getGrantedAmount().subtract(entitlement.getUsedAmount());
        if (amount.signum() <= 0 || available.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Requested leave exceeds the remaining event-based entitlement");
        }
        entitlement.setUsedAmount(entitlement.getUsedAmount().add(amount));
        return entitlementRepository.save(entitlement);
    }

    @Transactional
    public void release(String eventEntitlementId, BigDecimal amount) {
        if (eventEntitlementId == null || amount == null || amount.signum() <= 0) {
            return;
        }
        entitlementRepository.findById(eventEntitlementId).ifPresent(entitlement -> {
            entitlement.setUsedAmount(entitlement.getUsedAmount().subtract(amount).max(BigDecimal.ZERO));
            entitlementRepository.save(entitlement);
        });
    }

    private EventLeaveEntitlement generate(
            Staff staff, LeaveType leaveType, LeaveEntitlementPolicy policy, QualifyingLeaveEvent event) {
        validateEventForPolicy(policy, event);
        Optional<EventLeaveEntitlement> existing = entitlementRepository
                .findByQualifyingEventIdAndPolicyId(event.getId(), policy.getId());
        EventLeaveEntitlementStatus desiredStatus = requiresPendingVerification(policy, event)
                ? EventLeaveEntitlementStatus.PENDING_VERIFICATION
                : EventLeaveEntitlementStatus.ACTIVE;
        if (existing.isPresent()) {
            EventLeaveEntitlement entitlement = existing.get();
            if (entitlement.getStatus() == EventLeaveEntitlementStatus.PENDING_VERIFICATION
                    && desiredStatus == EventLeaveEntitlementStatus.ACTIVE) {
                entitlement.setStatus(EventLeaveEntitlementStatus.ACTIVE);
                EventLeaveEntitlement saved = entitlementRepository.save(entitlement);
                activatePendingApplications(saved.getId());
                return saved;
            }
            return entitlement;
        }

        LocalDate validFrom = event.getStartDate() != null
                ? event.getStartDate()
                : event.getEventDate().minusDays(valueOrZero(policy.getEventValidityDaysBefore()));
        LocalDate validTo = event.getEndDate() != null
                ? event.getEndDate()
                : event.getEventDate().plusDays(valueOrZero(policy.getEventValidityDaysAfter()));
        if (validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException("Qualifying event entitlement period is invalid");
        }

        return entitlementRepository.save(EventLeaveEntitlement.builder()
                .tenantId(staff.getTenantId())
                .staffId(staff.getId())
                .leaveTypeId(leaveType.getId())
                .policyId(policy.getId())
                .qualifyingEventId(event.getId())
                .validFrom(validFrom)
                .validTo(validTo)
                .grantedAmount(policy.getEntitlementAmount())
                .usedAmount(BigDecimal.ZERO)
                .status(desiredStatus)
                .generatedAt(Instant.now())
                .build());
    }

    private QualifyingLeaveEvent resolveOrCreateEvent(
            Staff staff, LeaveEntitlementPolicy policy, LeaveApplicationRequest request) {
        if (request.getQualifyingEventId() != null && !request.getQualifyingEventId().isBlank()) {
            return factService.findEvent(staff.getId(), request.getQualifyingEventId());
        }
        if (request.getEventDate() == null) {
            throw new IllegalArgumentException("eventDate is required for event-based leave when no existing event is selected");
        }

        String eventType = request.getEventTypeCode() == null || request.getEventTypeCode().isBlank()
                ? policy.getQualifyingEventTypeCode()
                : request.getEventTypeCode().trim().toUpperCase();
        Optional<QualifyingLeaveEvent> matching = factService.findEvents(staff.getId()).stream()
                .filter(event -> Objects.equals(event.getEventTypeCode(), eventType))
                .filter(event -> Objects.equals(event.getEventDate(), request.getEventDate()))
                .filter(event -> Objects.equals(normalize(event.getExternalReference()), normalize(request.getEventExternalReference())))
                .findFirst();
        if (matching.isPresent()) {
            return matching.get();
        }

        return factService.createEvent(staff.getId(), new QualifyingLeaveEventWriteRequest(
                request.getDependantId(),
                eventType,
                request.getEventDate(),
                request.getEventStartDate(),
                request.getEventEndDate(),
                request.getEventExternalReference(),
                request.getEventSupportingDocumentReference(),
                policy.isEventRequiresVerification() ? QualifyingEventStatus.RECORDED : QualifyingEventStatus.VERIFIED));
    }

    private LeaveEntitlementPolicy requireEventPolicy(Staff staff, String leaveTypeId, LocalDate effectiveDate) {
        PolicyResolutionResult resolution = resolutionService.resolve(staff, leaveTypeId, effectiveDate);
        if (resolution.ambiguous()) {
            throw new IllegalArgumentException("Multiple event-based policies match with the same priority");
        }
        if (resolution.selectedPolicyId() == null) {
            throw new IllegalArgumentException("No matching event-based entitlement policy");
        }
        LeaveEntitlementPolicy policy = policyRepository.findById(resolution.selectedPolicyId())
                .orElseThrow(() -> new IllegalStateException("Resolved policy no longer exists"));
        if (policy.getPolicyModel() != LeavePolicyModel.EVENT_BASED) {
            throw new IllegalArgumentException("The selected leave policy is not event-based");
        }
        return policy;
    }

    private void validateEventForPolicy(LeaveEntitlementPolicy policy, QualifyingLeaveEvent event) {
        if (!Objects.equals(policy.getQualifyingEventTypeCode(), event.getEventTypeCode())) {
            throw new IllegalArgumentException("Qualifying event type does not match the selected leave policy");
        }
        if (event.getStatus() == QualifyingEventStatus.REJECTED || event.getStatus() == QualifyingEventStatus.CANCELLED) {
            throw new IllegalArgumentException("Qualifying event is not valid for leave entitlement generation");
        }
    }

    private boolean requiresPendingVerification(LeaveEntitlementPolicy policy, QualifyingLeaveEvent event) {
        return policy.isEventRequiresVerification() && event.getStatus() != QualifyingEventStatus.VERIFIED;
    }

    private void activatePendingApplications(String eventEntitlementId) {
        for (LeaveApplication application : applicationRepository
                .findAllByEventEntitlementIdAndStatus(eventEntitlementId, LeaveStatus.PENDING_VERIFICATION)) {
            application.setStatus(LeaveStatus.PENDING);
            applicationRepository.save(application);
        }
    }

    private void requireSameTenant(Staff staff, LeaveType leaveType) {
        if (!Objects.equals(staff.getTenantId(), leaveType.getTenantId())) {
            throw new IllegalArgumentException("Leave type does not belong to the staff tenant");
        }
    }

    private long valueOrZero(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

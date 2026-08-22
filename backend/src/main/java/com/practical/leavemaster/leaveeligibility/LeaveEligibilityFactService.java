package com.practical.leavemaster.leaveeligibility;

import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveEligibilityFactService {

    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";

    private final StaffDependantRepository dependantRepository;
    private final QualifyingLeaveEventRepository eventRepository;
    private final StaffRepository staffRepository;
    private final AppUserRepository appUserRepository;
    private final TenantActivityService tenantActivityService;

    public List<StaffDependant> findDependants(String staffId) {
        Staff staff = requireReadableStaff(staffId);
        Optional<AppUser> user = currentUser();
        if (user.isPresent() && !isPlatformAdmin(user.get())) {
            return dependantRepository.findAllByTenantIdAndStaffId(staff.getTenantId(), staffId);
        }
        return dependantRepository.findAllByStaffId(staffId);
    }

    public StaffDependant findDependant(String staffId, String dependantId) {
        Staff staff = requireReadableStaff(staffId);
        StaffDependant dependant = dependantRepository.findById(dependantId)
                .orElseThrow(() -> new NoSuchElementException("Dependant not found: " + dependantId));
        requireSameOwner(staff, dependant.getStaffId(), dependant.getTenantId());
        return dependant;
    }

    public StaffDependant createDependant(String staffId, StaffDependantWriteRequest request) {
        Staff staff = requireWritableStaff(staffId);
        validateDependantRequest(request);
        StaffDependant dependant = StaffDependant.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(staff.getTenantId())
                .staffId(staffId)
                .name(request.name().trim())
                .relationshipCode(normalizeCode(request.relationshipCode(), "relationshipCode"))
                .dateOfBirth(request.dateOfBirth())
                .citizenshipCode(normalizeOptionalCode(request.citizenshipCode()))
                .residencyCode(normalizeOptionalCode(request.residencyCode()))
                .adoptionDate(request.adoptionDate())
                .effectiveFrom(request.effectiveFrom())
                .effectiveTo(request.effectiveTo())
                .active(request.active() == null || request.active())
                .build();
        StaffDependant saved = dependantRepository.save(dependant);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public StaffDependant updateDependant(String staffId, String dependantId, StaffDependantWriteRequest request) {
        Staff staff = requireWritableStaff(staffId);
        validateDependantRequest(request);
        StaffDependant existing = dependantRepository.findById(dependantId)
                .orElseThrow(() -> new NoSuchElementException("Dependant not found: " + dependantId));
        requireSameOwner(staff, existing.getStaffId(), existing.getTenantId());
        existing.setName(request.name().trim());
        existing.setRelationshipCode(normalizeCode(request.relationshipCode(), "relationshipCode"));
        existing.setDateOfBirth(request.dateOfBirth());
        existing.setCitizenshipCode(normalizeOptionalCode(request.citizenshipCode()));
        existing.setResidencyCode(normalizeOptionalCode(request.residencyCode()));
        existing.setAdoptionDate(request.adoptionDate());
        existing.setEffectiveFrom(request.effectiveFrom());
        existing.setEffectiveTo(request.effectiveTo());
        existing.setActive(request.active() == null || request.active());
        StaffDependant saved = dependantRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public void deleteDependant(String staffId, String dependantId) {
        Staff staff = requireWritableStaff(staffId);
        StaffDependant existing = dependantRepository.findById(dependantId)
                .orElseThrow(() -> new NoSuchElementException("Dependant not found: " + dependantId));
        requireSameOwner(staff, existing.getStaffId(), existing.getTenantId());
        if (eventRepository.existsByDependantId(dependantId)) {
            throw new IllegalStateException("Dependant is referenced by a qualifying leave event");
        }
        dependantRepository.delete(existing);
        tenantActivityService.touch(existing.getTenantId());
    }

    public List<QualifyingLeaveEvent> findEvents(String staffId) {
        Staff staff = requireReadableStaff(staffId);
        Optional<AppUser> user = currentUser();
        if (user.isPresent() && !isPlatformAdmin(user.get())) {
            return eventRepository.findAllByTenantIdAndStaffId(staff.getTenantId(), staffId);
        }
        return eventRepository.findAllByStaffId(staffId);
    }

    public QualifyingLeaveEvent findEvent(String staffId, String eventId) {
        Staff staff = requireReadableStaff(staffId);
        QualifyingLeaveEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Qualifying leave event not found: " + eventId));
        requireSameOwner(staff, event.getStaffId(), event.getTenantId());
        return event;
    }

    public QualifyingLeaveEvent createEvent(String staffId, QualifyingLeaveEventWriteRequest request) {
        Staff staff = requireWritableStaff(staffId);
        validateEventRequest(request);
        String dependantId = validateOptionalDependant(staff, request.dependantId());
        QualifyingLeaveEvent event = QualifyingLeaveEvent.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(staff.getTenantId())
                .staffId(staffId)
                .dependantId(dependantId)
                .eventTypeCode(normalizeCode(request.eventTypeCode(), "eventTypeCode"))
                .eventDate(request.eventDate())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .externalReference(trimToNull(request.externalReference()))
                .supportingDocumentReference(trimToNull(request.supportingDocumentReference()))
                .approvedEntitlementAmount(request.approvedEntitlementAmount())
                .status(request.status() == null ? QualifyingEventStatus.RECORDED : request.status())
                .build();
        QualifyingLeaveEvent saved = eventRepository.save(event);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public QualifyingLeaveEvent updateEvent(String staffId, String eventId, QualifyingLeaveEventWriteRequest request) {
        Staff staff = requireWritableStaff(staffId);
        validateEventRequest(request);
        QualifyingLeaveEvent existing = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Qualifying leave event not found: " + eventId));
        requireSameOwner(staff, existing.getStaffId(), existing.getTenantId());
        existing.setDependantId(validateOptionalDependant(staff, request.dependantId()));
        existing.setEventTypeCode(normalizeCode(request.eventTypeCode(), "eventTypeCode"));
        existing.setEventDate(request.eventDate());
        existing.setStartDate(request.startDate());
        existing.setEndDate(request.endDate());
        existing.setExternalReference(trimToNull(request.externalReference()));
        existing.setSupportingDocumentReference(trimToNull(request.supportingDocumentReference()));
        existing.setApprovedEntitlementAmount(request.approvedEntitlementAmount());
        existing.setStatus(request.status() == null ? QualifyingEventStatus.RECORDED : request.status());
        QualifyingLeaveEvent saved = eventRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public void deleteEvent(String staffId, String eventId) {
        Staff staff = requireWritableStaff(staffId);
        QualifyingLeaveEvent existing = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Qualifying leave event not found: " + eventId));
        requireSameOwner(staff, existing.getStaffId(), existing.getTenantId());
        eventRepository.delete(existing);
        tenantActivityService.touch(existing.getTenantId());
    }

    private Staff requireReadableStaff(String staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new NoSuchElementException("Staff not found: " + staffId));
        Optional<AppUser> user = currentUser();
        if (user.isEmpty() || isPlatformAdmin(user.get())) {
            return staff;
        }
        String tenantId = user.get().getTenantId();
        if (tenantId == null || tenantId.isBlank() || !Objects.equals(tenantId, staff.getTenantId())) {
            throw new NoSuchElementException("Staff not found: " + staffId);
        }
        return staff;
    }

    private Staff requireWritableStaff(String staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new NoSuchElementException("Staff not found: " + staffId));
        Optional<AppUser> user = currentUser();
        if (user.isEmpty()) {
            return staff;
        }
        if (isPlatformAdmin(user.get())) {
            throw new AccessDeniedException("Platform administrators cannot mutate tenant-owned leave eligibility facts");
        }
        String tenantId = user.get().getTenantId();
        if (tenantId == null || tenantId.isBlank() || !Objects.equals(tenantId, staff.getTenantId())) {
            throw new AccessDeniedException("Staff does not belong to the current tenant");
        }
        return staff;
    }

    private String validateOptionalDependant(Staff staff, String dependantId) {
        String normalizedId = trimToNull(dependantId);
        if (normalizedId == null) {
            return null;
        }
        StaffDependant dependant = dependantRepository.findById(normalizedId)
                .orElseThrow(() -> new IllegalArgumentException("Dependant not found: " + normalizedId));
        requireSameOwner(staff, dependant.getStaffId(), dependant.getTenantId());
        return normalizedId;
    }

    private void requireSameOwner(Staff staff, String ownedStaffId, String ownedTenantId) {
        if (!Objects.equals(staff.getId(), ownedStaffId) || !Objects.equals(staff.getTenantId(), ownedTenantId)) {
            throw new NoSuchElementException("Leave eligibility fact does not belong to the requested staff member");
        }
    }

    private void validateDependantRequest(StaffDependantWriteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dependant request is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Dependant name is required");
        }
        normalizeCode(request.relationshipCode(), "relationshipCode");
        validateRange(request.effectiveFrom(), request.effectiveTo(), "effective period");
        if (request.adoptionDate() != null && request.dateOfBirth() != null && request.adoptionDate().isBefore(request.dateOfBirth())) {
            throw new IllegalArgumentException("adoptionDate must not be before dateOfBirth");
        }
    }

    private void validateEventRequest(QualifyingLeaveEventWriteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Qualifying leave event request is required");
        }
        normalizeCode(request.eventTypeCode(), "eventTypeCode");
        if (request.eventDate() == null) {
            throw new IllegalArgumentException("eventDate is required");
        }
        validateRange(request.startDate(), request.endDate(), "event period");
        if (request.approvedEntitlementAmount() != null && request.approvedEntitlementAmount().signum() <= 0) {
            throw new IllegalArgumentException("approvedEntitlementAmount must be positive when supplied");
        }
    }

    private void validateRange(LocalDate from, LocalDate to, String label) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException(label + " end date must not be before start date");
        }
    }

    private String normalizeCode(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalCode(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Optional<AppUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return Optional.empty();
        }
        return appUserRepository.findById(authentication.getName());
    }

    private boolean isPlatformAdmin(AppUser user) {
        return user != null && user.isActive() && user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role != null
                        && role.isActive()
                        && role.getId() != null
                        && PLATFORM_ADMIN_ROLE_ID.equalsIgnoreCase(role.getId().trim()));
    }
}

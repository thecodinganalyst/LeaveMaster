package com.practical.leavemaster.tenant;

import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendarRepository;
import com.practical.leavemaster.leaveeligibility.QualifyingLeaveEventRepository;
import com.practical.leavemaster.leaveeligibility.StaffDependantRepository;
import com.practical.leavemaster.leaveentitlement.EventLeaveEntitlementRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveApproverRepository leaveApproverRepository;
    private final EventLeaveEntitlementRepository eventLeaveEntitlementRepository;
    private final QualifyingLeaveEventRepository qualifyingLeaveEventRepository;
    private final StaffDependantRepository staffDependantRepository;
    private final StaffRepository staffRepository;
    private final LeaveEntitlementPolicyRepository leaveEntitlementPolicyRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveCalendarRepository leaveCalendarRepository;
    private final AppUserRepository appUserRepository;
    private final TenantAdminProvisionService tenantAdminProvisionService;
    private final TenantLeaveConfigurationProvisionService tenantLeaveConfigurationProvisionService;
    private final JurisdictionRepository jurisdictionRepository;
    private final TenantJurisdictionRepository tenantJurisdictionRepository;

    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    public Optional<Tenant> findById(String id) {
        return tenantRepository.findById(id);
    }

    @Transactional
    public Tenant save(Tenant tenant) {
        List<TenantJurisdictionProvisionRequest> requestedJurisdictions = tenant.getJurisdictions();
        boolean legacyRequest = requestedJurisdictions == null || requestedJurisdictions.isEmpty();

        if (legacyRequest) {
            validateJurisdiction(tenant.getJurisdictionId());
        } else {
            validateRequestedJurisdictions(requestedJurisdictions);
            tenant.setJurisdictionId(requestedJurisdictions.get(0).jurisdictionId());
        }

        tenant.setLastModified(LocalDateTime.now());
        Tenant saved = tenantRepository.save(tenant);

        if (legacyRequest) {
            ensureTenantJurisdiction(saved.getId(), saved.getJurisdictionId());
            tenantLeaveConfigurationProvisionService.provision(saved);
        } else {
            for (TenantJurisdictionProvisionRequest request : requestedJurisdictions) {
                ensureTenantJurisdiction(saved.getId(), request.jurisdictionId());
                tenantLeaveConfigurationProvisionService.provision(
                        saved,
                        applyCalendarDefaults(request, tenant.getCalendarStart(), tenant.getCalendarEnd())
                );
            }
        }

        tenantAdminProvisionService.provision(saved.getId());
        return saved;
    }

    public Tenant update(String id, Tenant updated) {
        Tenant existing = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
        existing.setName(updated.getName());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setStatus(updated.getStatus());
        existing.setLastModified(LocalDateTime.now());
        return tenantRepository.save(existing);
    }

    public List<TenantJurisdiction> findJurisdictionsForUser(String loginName) {
        String tenantId = currentUserTenantId(loginName);
        return tenantJurisdictionRepository.findAllByTenantIdOrderByJurisdictionIdAsc(tenantId);
    }

    @Transactional
    public TenantJurisdiction addJurisdictionForUser(String loginName, TenantJurisdictionProvisionRequest request) {
        String tenantId = currentUserTenantId(loginName);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        validateJurisdiction(request.jurisdictionId());
        if (tenantJurisdictionRepository.existsByTenantIdAndJurisdictionId(tenantId, request.jurisdictionId())) {
            throw new IllegalArgumentException("Jurisdiction is already associated with this tenant: " + request.jurisdictionId());
        }

        TenantJurisdiction association = ensureTenantJurisdiction(tenantId, request.jurisdictionId());
        tenantLeaveConfigurationProvisionService.provision(tenant, applyCalendarDefaults(request, null, null));
        return association;
    }

    @Transactional
    public int markDormantTenants(LocalDateTime cutoff) {
        List<Tenant> dormantCandidates = tenantRepository.findAllByStatusAndLastModifiedBefore(TenantStatus.ACTIVE, cutoff);
        if (dormantCandidates.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        dormantCandidates.forEach(tenant -> {
            tenant.setStatus(TenantStatus.DORMANT);
            tenant.setLastModified(now);
        });
        tenantRepository.saveAll(dormantCandidates);
        return dormantCandidates.size();
    }

    @Transactional
    public void delete(String id) {
        tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
        leaveApplicationRepository.deleteAllByTenantId(id);
        leaveApproverRepository.deleteAllByTenantId(id);
        eventLeaveEntitlementRepository.deleteAllByTenantId(id);
        qualifyingLeaveEventRepository.deleteAllByTenantId(id);
        staffDependantRepository.deleteAllByTenantId(id);
        staffRepository.deleteAll(staffRepository.findAllByTenantId(id));
        leaveEntitlementPolicyRepository.deleteAllByTenantId(id);
        leaveTypeRepository.deleteAllByTenantId(id);
        leaveCalendarRepository.deleteAllByTenantId(id);
        tenantJurisdictionRepository.deleteAllByTenantId(id);
        appUserRepository.deleteAllByTenantId(id);
        tenantAdminProvisionService.deprovision(id);
        tenantRepository.deleteById(id);
    }

    private void validateRequestedJurisdictions(List<TenantJurisdictionProvisionRequest> requests) {
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("At least one jurisdiction is required when creating a tenant");
        }
        Set<String> seen = new HashSet<>();
        for (TenantJurisdictionProvisionRequest request : requests) {
            validateJurisdiction(request.jurisdictionId());
            if (!seen.add(request.jurisdictionId())) {
                throw new IllegalArgumentException("Duplicate jurisdiction selected: " + request.jurisdictionId());
            }
        }
    }

    private void validateJurisdiction(String jurisdictionId) {
        if (jurisdictionId == null || jurisdictionId.isBlank() || !jurisdictionRepository.existsById(jurisdictionId)) {
            throw new IllegalArgumentException("A valid jurisdictionId is required when creating a tenant");
        }
    }

    private TenantJurisdiction ensureTenantJurisdiction(String tenantId, String jurisdictionId) {
        String associationId = TenantJurisdiction.idFor(tenantId, jurisdictionId);
        return tenantJurisdictionRepository.findById(associationId)
                .orElseGet(() -> tenantJurisdictionRepository.save(TenantJurisdiction.builder()
                        .id(associationId)
                        .tenantId(tenantId)
                        .jurisdictionId(jurisdictionId)
                        .build()));
    }

    private TenantJurisdictionProvisionRequest applyCalendarDefaults(
            TenantJurisdictionProvisionRequest request,
            LocalDate requestedStart,
            LocalDate requestedEnd
    ) {
        if (!request.shouldIncludePublicHolidays()) return request;

        int currentYear = LocalDate.now().getYear();
        LocalDate defaultStart = requestedStart != null ? requestedStart : LocalDate.of(currentYear, 1, 1);
        LocalDate defaultEnd = requestedEnd != null ? requestedEnd : LocalDate.of(currentYear, 12, 31);
        TenantJurisdictionProvisionRequest normalized = request.withCalendarDefaults(defaultStart, defaultEnd);
        if (normalized.calendarEnd().isBefore(normalized.calendarStart())) {
            throw new IllegalArgumentException("Calendar end date must not be before calendar start date");
        }
        return normalized;
    }

    private String currentUserTenantId(String loginName) {
        AppUser user = appUserRepository.findById(loginName)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        if (user.getTenantId() == null || user.getTenantId().isBlank()) {
            throw new IllegalArgumentException("Tenant-scoped user is required");
        }
        return user.getTenantId();
    }
}

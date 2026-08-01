package com.practical.leavemaster.tenant;

import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendarRepository;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.location.LocationRepository;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveApproverRepository leaveApproverRepository;
    private final StaffRepository staffRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveCalendarRepository leaveCalendarRepository;
    private final LocationRepository locationRepository;
    private final AppUserRepository appUserRepository;

    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    public Optional<Tenant> findById(String id) {
        return tenantRepository.findById(id);
    }

    public Tenant save(Tenant tenant) {
        tenant.setLastModified(LocalDateTime.now());
        return tenantRepository.save(tenant);
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
        staffRepository.deleteAll(staffRepository.findAllByTenantId(id));
        leaveTypeRepository.deleteAllByTenantId(id);
        leaveCalendarRepository.deleteAllByTenantId(id);
        locationRepository.deleteAllByTenantId(id);
        appUserRepository.deleteAllByTenantId(id);
        tenantRepository.deleteById(id);
    }
}

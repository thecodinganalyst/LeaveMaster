package com.practical.leavemaster.leaveapprover;

import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffNotFoundException;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveApproverService {

    private final LeaveApproverRepository leaveApproverRepository;
    private final StaffRepository staffRepository;
    private final TenantActivityService tenantActivityService;

    public List<LeaveApprover> findAll() {
        return leaveApproverRepository.findAll();
    }

    public List<LeaveApprover> findByStaffId(String staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException(staffId));
        return leaveApproverRepository.findByStaff(staff);
    }

    public Optional<LeaveApprover> findById(String id) {
        return leaveApproverRepository.findById(id);
    }

    public LeaveApprover create(LeaveApproverRequest request) {
        validateDates(request);
        Staff[] staffEntities = resolveStaff(request);
        LeaveApprover leaveApprover = LeaveApprover.builder()
                .staff(staffEntities[0])
                .approver(staffEntities[1])
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .admin(staffEntities[2])
                .adminDate(LocalDate.now())
                .tenantId(staffEntities[0].getTenantId())
                .build();
        LeaveApprover saved = leaveApproverRepository.save(leaveApprover);
        tenantActivityService.touch(resolveTenantId(saved));
        return saved;
    }

    public LeaveApprover update(String id, LeaveApproverRequest request) {
        validateDates(request);
        LeaveApprover existing = leaveApproverRepository.findById(id)
                .orElseThrow(() -> new LeaveApproverNotFoundException(id));
        Staff[] staffEntities = resolveStaff(request);
        existing.setStaff(staffEntities[0]);
        existing.setApprover(staffEntities[1]);
        existing.setEffectiveFrom(request.getEffectiveFrom());
        existing.setEffectiveTo(request.getEffectiveTo());
        existing.setAdmin(staffEntities[2]);
        existing.setAdminDate(LocalDate.now());
        existing.setTenantId(staffEntities[0].getTenantId());
        LeaveApprover saved = leaveApproverRepository.save(existing);
        tenantActivityService.touch(resolveTenantId(saved));
        return saved;
    }

    public void delete(String id) {
        LeaveApprover existing = leaveApproverRepository.findById(id)
                .orElseThrow(() -> new LeaveApproverNotFoundException(id));
        leaveApproverRepository.deleteById(id);
        tenantActivityService.touch(resolveTenantId(existing));
    }

    private void validateDates(LeaveApproverRequest request) {
        if (request.getEffectiveFrom() == null) {
            throw new IllegalArgumentException("effectiveFrom is required");
        }
        if (request.getEffectiveTo() != null && !request.getEffectiveTo().isAfter(request.getEffectiveFrom())) {
            throw new IllegalArgumentException("effectiveTo must be after effectiveFrom");
        }
    }

    private Staff[] resolveStaff(LeaveApproverRequest request) {
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new StaffNotFoundException(request.getStaffId()));
        Staff approver = staffRepository.findById(request.getApproverId())
                .orElseThrow(() -> new StaffNotFoundException(request.getApproverId()));
        Staff admin = staffRepository.findById(request.getAdminId())
                .orElseThrow(() -> new StaffNotFoundException(request.getAdminId()));
        return new Staff[]{staff, approver, admin};
    }

    private String resolveTenantId(LeaveApprover leaveApprover) {
        if (leaveApprover.getTenantId() != null && !leaveApprover.getTenantId().isBlank()) {
            return leaveApprover.getTenantId();
        }
        return leaveApprover.getStaff() != null ? leaveApprover.getStaff().getTenantId() : null;
    }
}

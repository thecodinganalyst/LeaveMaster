package com.practical.leavemaster.leaveapprover;

import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffNotFoundException;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeaveApproverService {

    private final LeaveApproverRepository leaveApproverRepository;
    private final StaffRepository staffRepository;
    private final TenantActivityService tenantActivityService;
    private final AppUserRepository appUserRepository;

    public List<LeaveApprover> findAll() {
        Optional<AppUser> user = currentUser();
        if (user.isPresent() && user.get().getTenantId() != null && !user.get().getTenantId().isBlank()) {
            return leaveApproverRepository.findAllByTenantId(user.get().getTenantId());
        }
        return leaveApproverRepository.findAll();
    }

    public List<Staff> findTenantStaffOptions() {
        Optional<AppUser> user = currentUser();
        if (user.isPresent() && user.get().getTenantId() != null && !user.get().getTenantId().isBlank()) {
            return staffRepository.findAllByTenantId(user.get().getTenantId());
        }
        return staffRepository.findAll();
    }

    public List<Staff> findEligibleApproverOptions() {
        return findTenantStaffOptions().stream()
                .filter(this::canApproveLeave)
                .toList();
    }

    public List<LeaveApprover> findByStaffId(String staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException(staffId));
        validateTenantMembership(staff, "Staff");
        return leaveApproverRepository.findByStaff(staff);
    }

    public Optional<LeaveApprover> findById(String id) {
        return leaveApproverRepository.findById(id)
                .filter(this::belongsToCurrentTenant);
    }

    public LeaveApprover create(LeaveApproverRequest request) {
        validateDates(request);
        Staff staff = resolveStaff(request.getStaffId());
        Staff approver = resolveStaff(request.getApproverId());
        Staff admin = resolveAdmin(request);
        validateAssignment(staff, approver, admin, null);

        LeaveApprover leaveApprover = LeaveApprover.builder()
                .staff(staff)
                .approver(approver)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .admin(admin)
                .adminDate(LocalDate.now())
                .tenantId(staff.getTenantId())
                .build();
        LeaveApprover saved = leaveApproverRepository.save(leaveApprover);
        tenantActivityService.touch(resolveTenantId(saved));
        return saved;
    }

    public LeaveApprover update(String id, LeaveApproverRequest request) {
        validateDates(request);
        LeaveApprover existing = leaveApproverRepository.findById(id)
                .orElseThrow(() -> new LeaveApproverNotFoundException(id));
        if (!belongsToCurrentTenant(existing)) {
            throw new IllegalArgumentException("Leave approver record does not belong to the current tenant");
        }

        Staff staff = resolveStaff(request.getStaffId());
        Staff approver = resolveStaff(request.getApproverId());
        Staff admin = resolveAdmin(request);
        validateAssignment(staff, approver, admin, id);

        existing.setStaff(staff);
        existing.setApprover(approver);
        existing.setEffectiveFrom(request.getEffectiveFrom());
        existing.setEffectiveTo(request.getEffectiveTo());
        existing.setAdmin(admin);
        existing.setAdminDate(LocalDate.now());
        existing.setTenantId(staff.getTenantId());
        LeaveApprover saved = leaveApproverRepository.save(existing);
        tenantActivityService.touch(resolveTenantId(saved));
        return saved;
    }

    public void delete(String id) {
        LeaveApprover existing = leaveApproverRepository.findById(id)
                .orElseThrow(() -> new LeaveApproverNotFoundException(id));
        if (!belongsToCurrentTenant(existing)) {
            throw new IllegalArgumentException("Leave approver record does not belong to the current tenant");
        }
        leaveApproverRepository.deleteById(id);
        tenantActivityService.touch(resolveTenantId(existing));
    }

    private void validateAssignment(Staff staff, Staff approver, Staff admin, String excludedRecordId) {
        validateTenantMembership(staff, "Staff");
        validateTenantMembership(approver, "Approver");
        validateTenantMembership(admin, "Admin staff");
        if (!Objects.equals(staff.getTenantId(), approver.getTenantId()) || !Objects.equals(staff.getTenantId(), admin.getTenantId())) {
            throw new IllegalArgumentException("Staff, approver and admin staff must belong to the same tenant");
        }
        if (Objects.equals(staff.getId(), approver.getId())) {
            throw new IllegalArgumentException("Leave approver assignment would create a circular dependency: a staff member cannot approve their own leave");
        }
        if (!canApproveLeave(approver)) {
            throw new IllegalArgumentException("Selected approver is not authorised to approve leave");
        }
        validateNoCircularDependency(staff, approver, excludedRecordId);
    }

    private void validateNoCircularDependency(Staff staff, Staff approver, String excludedRecordId) {
        String tenantId = staff.getTenantId();
        List<LeaveApprover> relationships = tenantId == null || tenantId.isBlank()
                ? leaveApproverRepository.findAll()
                : leaveApproverRepository.findAllByTenantId(tenantId);

        Set<String> visited = new HashSet<>();
        if (reachesStaff(approver.getId(), staff.getId(), excludedRecordId, relationships, visited)) {
            throw new IllegalArgumentException("Leave approver assignment would create a circular dependency");
        }
    }

    private boolean reachesStaff(String currentStaffId, String targetStaffId, String excludedRecordId,
                                 List<LeaveApprover> relationships, Set<String> visited) {
        if (Objects.equals(currentStaffId, targetStaffId)) {
            return true;
        }
        if (!visited.add(currentStaffId)) {
            return false;
        }
        for (LeaveApprover relationship : relationships) {
            if (relationship == null || Objects.equals(excludedRecordId, relationship.getId())
                    || relationship.getStaff() == null || relationship.getApprover() == null) {
                continue;
            }
            if (Objects.equals(currentStaffId, relationship.getStaff().getId())
                    && reachesStaff(relationship.getApprover().getId(), targetStaffId, excludedRecordId, relationships, visited)) {
                return true;
            }
        }
        return false;
    }

    private void validateDates(LeaveApproverRequest request) {
        if (request.getEffectiveFrom() == null) {
            throw new IllegalArgumentException("effectiveFrom is required");
        }
        if (request.getEffectiveTo() != null && !request.getEffectiveTo().isAfter(request.getEffectiveFrom())) {
            throw new IllegalArgumentException("effectiveTo must be after effectiveFrom");
        }
    }

    private Staff resolveStaff(String staffId) {
        if (staffId == null || staffId.isBlank()) {
            throw new IllegalArgumentException("staffId and approverId are required");
        }
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException(staffId));
    }

    private Staff resolveAdmin(LeaveApproverRequest request) {
        Optional<AppUser> user = currentUser();
        String adminId = user.map(AppUser::getStaffId)
                .filter(id -> id != null && !id.isBlank())
                .orElse(request.getAdminId());
        if (adminId == null || adminId.isBlank()) {
            throw new IllegalArgumentException("Authenticated user is not associated with a staff record");
        }
        return resolveStaff(adminId);
    }

    private boolean canApproveLeave(Staff staff) {
        return appUserRepository.findByStaffId(staff.getId())
                .filter(AppUser::isActive)
                .map(user -> user.getRoles() != null && user.getRoles().stream()
                        .filter(Objects::nonNull)
                        .filter(role -> role.isActive() && role.getPermissions() != null)
                        .flatMap(role -> role.getPermissions().stream())
                        .filter(Objects::nonNull)
                        .anyMatch(permission -> RbacPermissions.LEAVE_APPLICATION_APPROVE.equals(permission.getCode())))
                .orElse(false);
    }

    private void validateTenantMembership(Staff staff, String label) {
        Optional<AppUser> user = currentUser();
        if (user.isEmpty() || user.get().getTenantId() == null || user.get().getTenantId().isBlank()) {
            return;
        }
        if (!Objects.equals(user.get().getTenantId(), staff.getTenantId())) {
            throw new IllegalArgumentException(label + " does not belong to the current tenant");
        }
    }

    private boolean belongsToCurrentTenant(LeaveApprover leaveApprover) {
        Optional<AppUser> user = currentUser();
        if (user.isEmpty() || user.get().getTenantId() == null || user.get().getTenantId().isBlank()) {
            return true;
        }
        return Objects.equals(user.get().getTenantId(), resolveTenantId(leaveApprover));
    }

    private Optional<AppUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return Optional.empty();
        }
        return appUserRepository.findById(authentication.getName());
    }

    private String resolveTenantId(LeaveApprover leaveApprover) {
        if (leaveApprover.getTenantId() != null && !leaveApprover.getTenantId().isBlank()) {
            return leaveApprover.getTenantId();
        }
        return leaveApprover.getStaff() != null ? leaveApprover.getStaff().getTenantId() : null;
    }
}

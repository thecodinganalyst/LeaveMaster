package com.practical.leavemaster.leavetype;

import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";

    private final LeaveTypeRepository leaveTypeRepository;
    private final TenantActivityService tenantActivityService;
    private final AppUserRepository appUserRepository;

    public List<LeaveType> findAll() {
        Optional<AppUser> currentUser = currentUser();
        if (currentUser.isPresent() && !isPlatformAdmin(currentUser.get())) {
            String tenantId = currentUser.get().getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                return List.of();
            }
            return leaveTypeRepository.findAllByTenantId(tenantId);
        }
        return leaveTypeRepository.findAll();
    }

    public Optional<LeaveType> findById(String id) {
        return leaveTypeRepository.findById(id)
                .filter(this::isAccessibleToCurrentUser);
    }

    public LeaveType save(LeaveType leaveType) {
        applyCurrentUsersTenant(leaveType);
        leaveType.setUsed(false);
        LeaveType saved = leaveTypeRepository.save(leaveType);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public LeaveType update(String id, LeaveType updated) {
        LeaveType existing = findById(id)
                .orElseThrow(() -> new LeaveTypeNotFoundException(id));
        existing.setName(updated.getName());
        LeaveType saved = leaveTypeRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public void delete(String id) {
        LeaveType leaveType = findById(id)
                .orElseThrow(() -> new LeaveTypeNotFoundException(id));
        if (leaveType.isUsed()) {
            throw new LeaveTypeInUseException(id);
        }
        leaveTypeRepository.deleteById(id);
        tenantActivityService.touch(leaveType.getTenantId());
    }

    private boolean isAccessibleToCurrentUser(LeaveType leaveType) {
        Optional<AppUser> currentUser = currentUser();
        if (currentUser.isEmpty() || isPlatformAdmin(currentUser.get())) {
            return true;
        }
        String tenantId = currentUser.get().getTenantId();
        return tenantId != null && !tenantId.isBlank() && Objects.equals(tenantId, leaveType.getTenantId());
    }

    private void applyCurrentUsersTenant(LeaveType leaveType) {
        Optional<AppUser> currentUser = currentUser();
        if (currentUser.isEmpty() || isPlatformAdmin(currentUser.get())) {
            return;
        }
        String tenantId = currentUser.get().getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Authenticated tenant user does not have a tenant id");
        }
        leaveType.setTenantId(tenantId);
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

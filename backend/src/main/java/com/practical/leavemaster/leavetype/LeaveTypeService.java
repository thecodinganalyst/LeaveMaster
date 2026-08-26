package com.practical.leavemaster.leavetype;

import com.practical.leavemaster.jurisdiction.JurisdictionLeaveType;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";

    private final LeaveTypeRepository leaveTypeRepository;
    private final JurisdictionLeaveTypeRepository jurisdictionLeaveTypeRepository;
    private final TenantActivityService tenantActivityService;
    private final AppUserRepository appUserRepository;

    public List<LeaveType> findAll() {
        Optional<AppUser> currentUser = currentUser();
        List<LeaveType> leaveTypes;
        if (currentUser.isPresent() && !isPlatformAdmin(currentUser.get())) {
            String tenantId = currentUser.get().getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                return List.of();
            }
            leaveTypes = leaveTypeRepository.findAllByTenantId(tenantId);
        } else {
            leaveTypes = leaveTypeRepository.findAll();
        }
        enrichJurisdictionIds(leaveTypes);
        return leaveTypes;
    }

    public Optional<LeaveType> findById(String id) {
        Optional<LeaveType> leaveType = leaveTypeRepository.findById(id)
                .filter(this::isAccessibleToCurrentUser);
        leaveType.ifPresent(this::enrichJurisdictionId);
        return leaveType;
    }

    public LeaveType save(LeaveType leaveType) {
        applyCurrentUsersTenant(leaveType);
        if (leaveType.getId() == null || leaveType.getId().isBlank()) {
            leaveType.setId(UUID.randomUUID().toString());
        }
        leaveType.setUsed(false);
        LeaveType saved = leaveTypeRepository.save(leaveType);
        enrichJurisdictionId(saved);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public LeaveType update(String id, LeaveType updated) {
        LeaveType existing = findById(id)
                .orElseThrow(() -> new LeaveTypeNotFoundException(id));
        existing.setName(updated.getName());
        existing.setActive(updated.isActive());
        existing.setStatutory(updated.isStatutory());
        existing.setPaid(updated.getPaid());
        existing.setSourceName(updated.getSourceName());
        existing.setSourceUrl(updated.getSourceUrl());
        existing.setEffectiveFrom(updated.getEffectiveFrom());
        existing.setEffectiveTo(updated.getEffectiveTo());
        LeaveType saved = leaveTypeRepository.save(existing);
        enrichJurisdictionId(saved);
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

    private void enrichJurisdictionIds(Collection<LeaveType> leaveTypes) {
        Map<String, JurisdictionLeaveType> sourcesById = jurisdictionLeaveTypeRepository.findAllById(
                        leaveTypes.stream()
                                .map(LeaveType::getSourceJurisdictionLeaveTypeId)
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(value -> !value.isEmpty())
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(JurisdictionLeaveType::getId, Function.identity()));

        for (LeaveType leaveType : leaveTypes) {
            String sourceId = leaveType.getSourceJurisdictionLeaveTypeId();
            JurisdictionLeaveType source = sourceId == null ? null : sourcesById.get(sourceId.trim());
            leaveType.setJurisdictionId(source == null ? null : source.getJurisdictionId());
        }
    }

    private void enrichJurisdictionId(LeaveType leaveType) {
        String sourceId = leaveType.getSourceJurisdictionLeaveTypeId();
        if (sourceId == null || sourceId.isBlank()) {
            leaveType.setJurisdictionId(null);
            return;
        }
        leaveType.setJurisdictionId(jurisdictionLeaveTypeRepository.findById(sourceId.trim())
                .map(JurisdictionLeaveType::getJurisdictionId)
                .orElse(null));
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

package com.practical.leavemaster.rbac;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserNotFoundException;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppRoleService {

    private final AppRoleRepository appRoleRepository;
    private final AppPermissionRepository appPermissionRepository;
    private final AppUserRepository appUserRepository;

    public List<AppRole> findAll() {
        return appRoleRepository.findAll();
    }

    public List<AppRole> findByTenantId(String tenantId) {
        return appRoleRepository.findAllByTenantId(tenantId);
    }

    public List<AppPermission> findAllPermissions() {
        return appPermissionRepository.findAll();
    }

    public Optional<AppRole> findById(String roleId) {
        return appRoleRepository.findById(roleId);
    }

    @Transactional
    public AppRole create(RoleRequest request) {
        if (request.getId() == null || request.getId().isBlank()) {
            throw new IllegalArgumentException("Role id must not be blank");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Role description must not be blank");
        }
        if (appRoleRepository.existsById(request.getId())) {
            throw new IllegalArgumentException("Role already exists: " + request.getId());
        }

        AppRole role = AppRole.builder()
                .id(request.getId())
                .description(request.getDescription())
                .active(request.isActive())
                .permissions(resolvePermissions(request.getPermissionCodes()))
                .build();

        return appRoleRepository.save(role);
    }

    @Transactional
    public AppRole update(String roleId, RoleRequest request) {
        AppRole role = appRoleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));

        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Role description must not be blank");
        }

        role.setDescription(request.getDescription());
        role.setActive(request.isActive());
        role.setPermissions(resolvePermissions(request.getPermissionCodes()));
        return appRoleRepository.save(role);
    }

    @Transactional
    public AppRole disable(String roleId) {
        AppRole role = appRoleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
        role.setActive(false);
        return appRoleRepository.save(role);
    }

    @Transactional
    public AppRole enable(String roleId) {
        AppRole role = appRoleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
        role.setActive(true);
        return appRoleRepository.save(role);
    }

    @Transactional
    public AppUser addUserToRole(String roleId, String loginName) {
        AppRole role = appRoleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
        if (!role.isActive()) {
            throw new RoleDisabledException(roleId);
        }

        AppUser user = appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        user.getRoles().add(role);
        return appUserRepository.save(user);
    }

    @Transactional
    public AppUser removeUserFromRole(String roleId, String loginName) {
        AppRole role = appRoleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));

        AppUser user = appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        user.getRoles().removeIf(r -> r.getId().equals(role.getId()));
        return appUserRepository.save(user);
    }

    private Set<AppPermission> resolvePermissions(Set<String> permissionCodes) {
        Set<String> normalizedCodes = permissionCodes == null ? Set.of() : permissionCodes;
        Set<AppPermission> permissions = new HashSet<>(appPermissionRepository.findAllById(normalizedCodes));
        if (permissions.size() != normalizedCodes.size()) {
            Set<String> foundCodes = permissions.stream().map(AppPermission::getCode).collect(java.util.stream.Collectors.toSet());
            Set<String> missingCodes = normalizedCodes.stream()
                    .filter(code -> !foundCodes.contains(code))
                    .collect(java.util.stream.Collectors.toSet());
            throw new IllegalArgumentException("Unknown permission codes: " + missingCodes);
        }
        return permissions;
    }
}

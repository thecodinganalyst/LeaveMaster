package com.practical.leavemaster.staff;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffRoleAssignmentPolicy {

    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";

    private final AppRoleRepository appRoleRepository;
    private final AppUserRepository appUserRepository;

    public List<AppRole> findAssignableRoles() {
        Optional<AppUser> currentUser = currentUser();
        if (currentUser.isEmpty() || !currentUser.get().isActive()) {
            return List.of();
        }

        String tenantId = currentUser.get().getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return List.of();
        }

        Set<String> grantedPermissions = grantedPermissions(currentUser.get());
        return appRoleRepository.findAllByTenantId(tenantId).stream()
                .filter(AppRole::isActive)
                .filter(role -> !isPlatformAdmin(role.getId()))
                .filter(role -> grantedPermissions.containsAll(permissionCodes(role)))
                .sorted(Comparator.comparing(AppRole::getId, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void validateAssignableRoleIds(Set<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        Set<String> assignableRoleIds = findAssignableRoles().stream()
                .map(AppRole::getId)
                .collect(Collectors.toSet());

        Set<String> normalizedRoleIds = new LinkedHashSet<>();
        for (String roleId : roleIds) {
            if (roleId == null || roleId.isBlank()) {
                throw new IllegalArgumentException("Role id must not be blank");
            }
            normalizedRoleIds.add(roleId.trim());
        }

        for (String roleId : normalizedRoleIds) {
            if (!assignableRoleIds.contains(roleId)) {
                throw new IllegalArgumentException("Role is not assignable by the current user: " + roleId);
            }
        }
    }

    private Set<String> grantedPermissions(AppUser user) {
        if (user.getRoles() == null) {
            return Set.of();
        }
        return user.getRoles().stream()
                .filter(role -> role != null && role.isActive())
                .flatMap(role -> role.getPermissions() == null ? Set.<AppPermission>of().stream() : role.getPermissions().stream())
                .filter(permission -> permission != null && permission.getCode() != null)
                .map(AppPermission::getCode)
                .collect(Collectors.toSet());
    }

    private Set<String> permissionCodes(AppRole role) {
        if (role.getPermissions() == null) {
            return Set.of();
        }
        return role.getPermissions().stream()
                .filter(permission -> permission != null && permission.getCode() != null)
                .map(AppPermission::getCode)
                .collect(Collectors.toSet());
    }

    private Optional<AppUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return Optional.empty();
        }
        String principalName = authentication.getName();
        return appUserRepository.findById(principalName)
                .or(() -> appUserRepository.findUniqueByLoginName(principalName));
    }

    private boolean isPlatformAdmin(String roleId) {
        return roleId != null && PLATFORM_ADMIN_ROLE_ID.equalsIgnoreCase(roleId.trim());
    }
}

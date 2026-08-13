package com.practical.leavemaster.config;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppPermissionRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PlatformJurisdictionPermissionInitializer {
    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final Map<String, String> PERMISSIONS = new LinkedHashMap<>();

    static {
        PERMISSIONS.put("JURISDICTION_READ", "Read platform jurisdiction data");
        PERMISSIONS.put("JURISDICTION_WRITE", "Create, update and delete platform jurisdictions");
        PERMISSIONS.put("JURISDICTION_LEAVE_TYPE_READ", "Read jurisdiction leave type data");
        PERMISSIONS.put("JURISDICTION_LEAVE_TYPE_WRITE", "Create, update and delete jurisdiction leave types");
    }

    private final AppPermissionRepository permissionRepository;
    private final AppRoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconcilePermissions() {
        Set<AppPermission> required = new HashSet<>();
        PERMISSIONS.forEach((code, description) -> required.add(permissionRepository.findById(code)
                .orElseGet(() -> permissionRepository.save(AppPermission.builder().code(code).description(description).build()))));

        roleRepository.findById(PLATFORM_ADMIN).ifPresent(role -> {
            Set<AppPermission> permissions = new HashSet<>(role.getPermissions() == null ? Set.of() : role.getPermissions());
            permissions.addAll(required);
            role.setPermissions(permissions);
            roleRepository.save(role);
        });
    }
}

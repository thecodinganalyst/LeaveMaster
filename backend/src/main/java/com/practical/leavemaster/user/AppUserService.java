package com.practical.leavemaster.user;

import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";

    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantActivityService tenantActivityService;

    public List<AppUser> findAll() {
        List<AppUser> users = appUserRepository.findAll();
        Optional<AppUser> currentUser = currentUser();
        if (currentUser.map(this::isActivePlatformAdminUser).orElse(false)) {
            return users;
        }
        if (currentUser.isPresent() && currentUser.get().getTenantId() != null) {
            String tenantId = currentUser.get().getTenantId();
            return users.stream()
                    .filter(user -> Objects.equals(tenantId, user.getTenantId()))
                    .filter(user -> !isPlatformAdminUser(user))
                    .toList();
        }
        return users.stream()
                .filter(user -> !isPlatformAdminUser(user))
                .toList();
    }

    public Optional<AppUser> findByLoginName(String loginName) {
        return resolveUserForCurrentContext(loginName)
                .filter(user -> !isPlatformAdminUser(user) || isCurrentUserPlatformAdmin());
    }

    public Optional<AppUser> findByLoginName(String tenantId, String loginName) {
        return appUserRepository.findScopedByLoginName(tenantId, loginName);
    }

    public AppUser save(AppUser user) {
        String loginName = normalizeLoginName(user.getLoginName());
        user.setLoginName(loginName);
        if (appUserRepository.existsScopedLoginName(user.getTenantId(), loginName)) {
            throw new DuplicateLoginNameException(loginName);
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        user.setUserId(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        applyOidcCredentials(user, user.getOidcProvider(), user.getOidcSubject());
        AppUser saved = appUserRepository.save(user);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public AppUser update(String loginName, AppUser updated) {
        AppUser existing = resolveUserForCurrentContext(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        existing.setActive(updated.isActive());
        applyOidcCredentials(existing, updated.getOidcProvider(), updated.getOidcSubject());
        AppUser saved = appUserRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public AppUser changePassword(String loginName, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password must not be blank");
        }
        AppUser existing = resolveUserForCurrentContext(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        existing.setPassword(passwordEncoder.encode(newPassword));
        AppUser saved = appUserRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public void changeOwnPassword(String userId, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password must not be blank");
        }
        validateSelfServicePassword(newPassword);

        AppUser existing = appUserRepository.findById(userId)
                .orElseGet(() -> appUserRepository.findUniqueByLoginName(userId)
                        .orElseThrow(() -> new AppUserNotFoundException(userId)));

        if (existing.getPassword() == null || !passwordEncoder.matches(currentPassword, existing.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, existing.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        existing.setPassword(passwordEncoder.encode(newPassword));
        AppUser saved = appUserRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
    }

    public AppUser activate(String loginName) {
        AppUser existing = resolveUserForCurrentContext(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        existing.setActive(true);
        AppUser saved = appUserRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public AppUser deactivate(String loginName) {
        AppUser existing = resolveUserForCurrentContext(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        existing.setActive(false);
        AppUser saved = appUserRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public void delete(String loginName) {
        AppUser existing = resolveUserForCurrentContext(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        appUserRepository.deleteById(existing.getUserId());
        tenantActivityService.touch(existing.getTenantId());
    }

    public AppUser createForStaff(String staffId, String loginName, String password, boolean active) {
        return createForStaffWithPassword(staffId, loginName, password, active, null, Set.of());
    }

    public AppUser createForStaff(String staffId, String loginName, String password, boolean active, String tenantId) {
        return createForStaffWithPassword(staffId, loginName, password, active, tenantId, Set.of());
    }

    public AppUser createForStaff(
            String staffId,
            String loginName,
            String ignoredDefaultPassword,
            boolean active,
            String tenantId,
            Set<String> roleIds) {
        return createPendingForStaff(staffId, loginName, active, tenantId, roleIds);
    }

    private AppUser createForStaffWithPassword(
            String staffId,
            String loginName,
            String password,
            boolean active,
            String tenantId,
            Set<String> roleIds) {
        String normalizedLoginName = normalizeLoginName(loginName);
        assertLoginNameAvailable(tenantId, normalizedLoginName);
        AppUser user = AppUser.builder()
                .loginName(normalizedLoginName)
                .password(passwordEncoder.encode(password))
                .active(active)
                .staffId(staffId)
                .oidcProvider(null)
                .oidcSubject(null)
                .tenantId(tenantId)
                .roles(resolveStaffRoles(roleIds, tenantId))
                .build();
        AppUser saved = appUserRepository.save(user);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public AppUser createPendingForStaff(
            String staffId,
            String loginName,
            boolean active,
            String tenantId,
            Set<String> roleIds) {
        String normalizedLoginName = normalizeLoginName(loginName);
        assertLoginNameAvailable(tenantId, normalizedLoginName);
        AppUser user = AppUser.builder()
                .loginName(normalizedLoginName)
                .password(null)
                .active(active)
                .staffId(staffId)
                .oidcProvider(null)
                .oidcSubject(null)
                .tenantId(tenantId)
                .roles(resolveStaffRoles(roleIds, tenantId))
                .build();
        AppUser saved = appUserRepository.save(user);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public AppUser completeInitialPassword(String loginName, String newPassword) {
        validateSelfServicePassword(newPassword);
        AppUser existing = appUserRepository.findUniqueByLoginName(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        return completeInitialPassword(existing.getUserId(), newPassword, true);
    }

    public AppUser completeInitialPasswordByUserId(String userId, String newPassword) {
        validateSelfServicePassword(newPassword);
        return completeInitialPassword(userId, newPassword, false);
    }

    private AppUser completeInitialPassword(String userId, String newPassword, boolean alreadyValidated) {
        if (!alreadyValidated) {
            validateSelfServicePassword(newPassword);
        }
        AppUser existing = appUserRepository.findById(userId)
                .orElseThrow(() -> new AppUserNotFoundException(userId));
        if (!existing.isActive() || existing.getPassword() != null) {
            throw new IllegalStateException("Account is not eligible for initial password setup");
        }
        existing.setPassword(passwordEncoder.encode(newPassword));
        AppUser saved = appUserRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public Set<String> findRoleIdsByStaffId(String staffId) {
        return appUserRepository.findUniqueByStaffId(staffId)
                .map(this::roleIds)
                .orElseGet(LinkedHashSet::new);
    }

    public Set<String> findRoleIdsByStaffId(String staffId, String tenantId) {
        return appUserRepository.findByTenantIdAndStaffId(tenantId, staffId)
                .map(this::roleIds)
                .orElseGet(LinkedHashSet::new);
    }

    public AppUser updateRolesByStaffId(String staffId, Set<String> roleIds, String tenantId) {
        AppUser user = appUserRepository.findByTenantIdAndStaffId(tenantId, staffId)
                .orElseThrow(() -> new AppUserNotFoundException(staffId));
        user.setRoles(resolveStaffRoles(roleIds, tenantId));
        AppUser saved = appUserRepository.save(user);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public void deactivateByStaffId(String staffId) {
        appUserRepository.findUniqueByStaffId(staffId).ifPresent(this::deactivateUser);
    }

    public void deactivateByStaffId(String staffId, String tenantId) {
        appUserRepository.findByTenantIdAndStaffId(tenantId, staffId).ifPresent(this::deactivateUser);
    }

    public AppUser login(String loginName, String password) {
        AppUser user = appUserRepository.findUniqueByLoginName(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        if (!user.isActive()) {
            throw new IllegalStateException("User account is not active");
        }
        if (password == null || user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return user;
    }

    private void deactivateUser(AppUser user) {
        user.setActive(false);
        AppUser saved = appUserRepository.save(user);
        tenantActivityService.touch(saved.getTenantId());
    }

    private Set<String> roleIds(AppUser user) {
        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(AppRole::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private void assertLoginNameAvailable(String tenantId, String loginName) {
        if (appUserRepository.existsScopedLoginName(tenantId, loginName)) {
            throw new DuplicateLoginNameException(loginName);
        }
    }

    private String normalizeLoginName(String loginName) {
        if (loginName == null || loginName.isBlank()) {
            throw new IllegalArgumentException("Login name must not be blank");
        }
        return loginName.trim();
    }

    private Optional<AppUser> resolveUserForCurrentContext(String loginName) {
        if (loginName == null || loginName.isBlank()) {
            return Optional.empty();
        }
        Optional<AppUser> currentUser = currentUser();
        if (currentUser.isPresent() && !isActivePlatformAdminUser(currentUser.get())) {
            return appUserRepository.findScopedByLoginName(currentUser.get().getTenantId(), loginName);
        }
        if (currentUser.isPresent() && isActivePlatformAdminUser(currentUser.get())) {
            Optional<AppUser> platformUser = appUserRepository.findByTenantIdIsNullAndLoginName(loginName.trim());
            if (platformUser.isPresent()) {
                return platformUser;
            }
        }
        return appUserRepository.findUniqueByLoginName(loginName);
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

    private Set<AppRole> resolveStaffRoles(Set<String> roleIds, String tenantId) {
        Set<String> normalizedRoleIds = new LinkedHashSet<>();
        if (roleIds != null) {
            for (String roleId : roleIds) {
                if (roleId == null || roleId.isBlank()) {
                    throw new IllegalArgumentException("Role id must not be blank");
                }
                String normalizedRoleId = roleId.trim();
                if (PLATFORM_ADMIN_ROLE_ID.equalsIgnoreCase(normalizedRoleId)) {
                    throw new IllegalArgumentException("Platform administrator role cannot be assigned to staff");
                }
                normalizedRoleIds.add(normalizedRoleId);
            }
        }

        Set<AppRole> roles = new HashSet<>();
        for (String roleId : normalizedRoleIds) {
            AppRole role = appRoleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
            if (!Objects.equals(tenantId, role.getTenantId())) {
                throw new IllegalArgumentException("Role does not belong to the staff tenant: " + roleId);
            }
            roles.add(role);
        }
        return roles;
    }

    private void validateSelfServicePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password must not be blank");
        }
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("New password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
    }

    private void applyOidcCredentials(AppUser target, String oidcProvider, String oidcSubject) {
        boolean hasProvider = oidcProvider != null && !oidcProvider.isBlank();
        boolean hasSubject = oidcSubject != null && !oidcSubject.isBlank();

        if (hasProvider != hasSubject) {
            throw new IllegalArgumentException("Both oidcProvider and oidcSubject must be provided together");
        }

        if (!hasProvider) {
            target.setOidcProvider(null);
            target.setOidcSubject(null);
            return;
        }

        String normalizedProvider = oidcProvider.trim().toLowerCase(Locale.ROOT);
        String normalizedSubject = oidcSubject.trim();

        appUserRepository.findByOidcProviderAndOidcSubject(normalizedProvider, normalizedSubject)
                .filter(existing -> target.getUserId() == null || !existing.getUserId().equals(target.getUserId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("OIDC identity is already assigned to another user");
                });

        target.setOidcProvider(normalizedProvider);
        target.setOidcSubject(normalizedSubject);
    }

    private boolean isCurrentUserPlatformAdmin() {
        return currentUser().map(this::isActivePlatformAdminUser).orElse(false);
    }

    private boolean isActivePlatformAdminUser(AppUser user) {
        return user != null && user.isActive() && user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role != null && role.isActive() && isPlatformAdminRole(role.getId()));
    }

    private boolean isPlatformAdminUser(AppUser user) {
        return user != null && user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role != null && isPlatformAdminRole(role.getId()));
    }

    private boolean isPlatformAdminRole(String roleId) {
        return roleId != null && PLATFORM_ADMIN_ROLE_ID.equalsIgnoreCase(roleId.trim());
    }
}

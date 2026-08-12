package com.practical.leavemaster.user;

import com.practical.leavemaster.tenant.TenantActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantActivityService tenantActivityService;

    public List<AppUser> findAll() {
        return appUserRepository.findAll();
    }

    public Optional<AppUser> findByLoginName(String loginName) {
        return appUserRepository.findById(loginName);
    }

    public AppUser save(AppUser user) {
        if (appUserRepository.existsById(user.getLoginName())) {
            throw new DuplicateLoginNameException(user.getLoginName());
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        applyOidcCredentials(user, user.getOidcProvider(), user.getOidcSubject());
        AppUser saved = appUserRepository.save(user);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public AppUser update(String loginName, AppUser updated) {
        AppUser existing = appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        existing.setActive(updated.isActive());
        applyOidcCredentials(existing, updated.getOidcProvider(), updated.getOidcSubject());
        AppUser saved = appUserRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public AppUser changePassword(String loginName, String newPassword) {
        validateNewPassword(newPassword);
        AppUser existing = appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        existing.setPassword(passwordEncoder.encode(newPassword));
        AppUser saved = appUserRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public void changeOwnPassword(String loginName, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password must not be blank");
        }
        validateNewPassword(newPassword);

        AppUser existing = appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));

        if (!passwordEncoder.matches(currentPassword, existing.getPassword())) {
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
        AppUser existing = appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        existing.setActive(true);
        AppUser saved = appUserRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public AppUser deactivate(String loginName) {
        AppUser existing = appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        existing.setActive(false);
        AppUser saved = appUserRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public void delete(String loginName) {
        AppUser existing = appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        appUserRepository.deleteById(loginName);
        tenantActivityService.touch(existing.getTenantId());
    }

    public AppUser createForStaff(String staffId, String loginName, String password, boolean active) {
        return createForStaff(staffId, loginName, password, active, null);
    }

    public AppUser createForStaff(String staffId, String loginName, String password, boolean active, String tenantId) {
        if (appUserRepository.existsById(loginName)) {
            throw new DuplicateLoginNameException(loginName);
        }
        AppUser user = AppUser.builder()
                .loginName(loginName)
                .password(passwordEncoder.encode(password))
                .active(active)
                .staffId(staffId)
                .oidcProvider(null)
                .oidcSubject(null)
                .tenantId(tenantId)
                .build();
        AppUser saved = appUserRepository.save(user);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public void deactivateByStaffId(String staffId) {
        appUserRepository.findByStaffId(staffId).ifPresent(user -> {
            user.setActive(false);
            AppUser saved = appUserRepository.save(user);
            tenantActivityService.touch(saved.getTenantId());
        });
    }

    public AppUser login(String loginName, String password) {
        AppUser user = appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        if (!user.isActive()) {
            throw new IllegalStateException("User account is not active");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return user;
    }

    private void validateNewPassword(String newPassword) {
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
                .filter(existing -> !existing.getLoginName().equals(target.getLoginName()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("OIDC identity is already assigned to another user");
                });

        target.setOidcProvider(normalizedProvider);
        target.setOidcSubject(normalizedSubject);
    }
}

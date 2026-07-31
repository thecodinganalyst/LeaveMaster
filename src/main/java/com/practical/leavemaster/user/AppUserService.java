package com.practical.leavemaster.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

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
        return appUserRepository.save(user);
    }

    public AppUser update(String loginName, AppUser updated) {
        AppUser existing = appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        existing.setActive(updated.isActive());
        applyOidcCredentials(existing, updated.getOidcProvider(), updated.getOidcSubject());
        return appUserRepository.save(existing);
    }

    public AppUser changePassword(String loginName, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password must not be blank");
        }
        AppUser existing = appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        existing.setPassword(passwordEncoder.encode(newPassword));
        return appUserRepository.save(existing);
    }

    public AppUser activate(String loginName) {
        AppUser existing = appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        existing.setActive(true);
        return appUserRepository.save(existing);
    }

    public AppUser deactivate(String loginName) {
        AppUser existing = appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        existing.setActive(false);
        return appUserRepository.save(existing);
    }

    public void delete(String loginName) {
        appUserRepository.findById(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        appUserRepository.deleteById(loginName);
    }

    public AppUser createForStaff(String staffId, String loginName, String password, boolean active) {
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
                .build();
        return appUserRepository.save(user);
    }

    public void deactivateByStaffId(String staffId) {
        appUserRepository.findByStaffId(staffId).ifPresent(user -> {
            user.setActive(false);
            appUserRepository.save(user);
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

package com.practical.leavemaster.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;

    public List<AppUser> findAll() {
        return appUserRepository.findAll();
    }

    public Optional<AppUser> findById(String id) {
        return appUserRepository.findById(id);
    }

    public Optional<AppUser> findByLoginName(String loginName) {
        return appUserRepository.findByLoginName(loginName);
    }

    public AppUser save(AppUser user) {
        if (appUserRepository.existsByLoginName(user.getLoginName())) {
            throw new DuplicateLoginNameException(user.getLoginName());
        }
        if (user.getId() == null || user.getId().isBlank()) {
            user.setId(UUID.randomUUID().toString());
        }
        return appUserRepository.save(user);
    }

    public AppUser update(String id, AppUser updated) {
        AppUser existing = appUserRepository.findById(id)
                .orElseThrow(() -> new AppUserNotFoundException(id));
        if (!existing.getLoginName().equals(updated.getLoginName())
                && appUserRepository.existsByLoginName(updated.getLoginName())) {
            throw new DuplicateLoginNameException(updated.getLoginName());
        }
        existing.setLoginName(updated.getLoginName());
        existing.setActive(updated.isActive());
        return appUserRepository.save(existing);
    }

    public AppUser changePassword(String id, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password must not be blank");
        }
        AppUser existing = appUserRepository.findById(id)
                .orElseThrow(() -> new AppUserNotFoundException(id));
        existing.setPassword(newPassword);
        return appUserRepository.save(existing);
    }

    public AppUser activate(String id) {
        AppUser existing = appUserRepository.findById(id)
                .orElseThrow(() -> new AppUserNotFoundException(id));
        existing.setActive(true);
        return appUserRepository.save(existing);
    }

    public AppUser deactivate(String id) {
        AppUser existing = appUserRepository.findById(id)
                .orElseThrow(() -> new AppUserNotFoundException(id));
        existing.setActive(false);
        return appUserRepository.save(existing);
    }

    public void delete(String id) {
        appUserRepository.findById(id)
                .orElseThrow(() -> new AppUserNotFoundException(id));
        appUserRepository.deleteById(id);
    }

    public AppUser createForStaff(String staffId, String loginName, String password, boolean active) {
        if (appUserRepository.existsByLoginName(loginName)) {
            throw new DuplicateLoginNameException(loginName);
        }
        AppUser user = AppUser.builder()
                .id(UUID.randomUUID().toString())
                .loginName(loginName)
                .password(password)
                .active(active)
                .staffId(staffId)
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
        AppUser user = appUserRepository.findByLoginName(loginName)
                .orElseThrow(() -> new AppUserNotFoundException(loginName));
        if (!user.isActive()) {
            throw new IllegalStateException("User account is not active");
        }
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return user;
    }
}

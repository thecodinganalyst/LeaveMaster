package com.practical.leavemaster.location;

import com.practical.leavemaster.staff.StaffRepository;
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
public class LocationService {

    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";

    private final LocationRepository locationRepository;
    private final StaffRepository staffRepository;
    private final TenantActivityService tenantActivityService;
    private final AppUserRepository appUserRepository;

    public List<Location> findAll() {
        Optional<AppUser> currentUser = currentUser();
        if (currentUser.isPresent() && !isPlatformAdmin(currentUser.get())) {
            String tenantId = currentUser.get().getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                return List.of();
            }
            return locationRepository.findAllByTenantId(tenantId);
        }
        return locationRepository.findAll();
    }

    public Optional<Location> findById(String id) {
        return locationRepository.findById(id)
                .filter(this::isAccessibleToCurrentUser);
    }

    public Location save(Location location) {
        applyCurrentUsersTenant(location);
        Location saved = locationRepository.save(location);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public Location update(String id, Location updated) {
        Location existing = findById(id)
                .orElseThrow(() -> new LocationNotFoundException(id));
        existing.setLocationName(updated.getLocationName());
        existing.setCountry(updated.getCountry());
        existing.setState(updated.getState());
        Location saved = locationRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    public void delete(String id) {
        Location existing = findById(id)
                .orElseThrow(() -> new LocationNotFoundException(id));
        if (staffRepository.existsByLocationId(id)) {
            throw new LocationInUseException(id);
        }
        locationRepository.deleteById(id);
        tenantActivityService.touch(existing.getTenantId());
    }

    private boolean isAccessibleToCurrentUser(Location location) {
        Optional<AppUser> currentUser = currentUser();
        if (currentUser.isEmpty() || isPlatformAdmin(currentUser.get())) {
            return true;
        }
        String tenantId = currentUser.get().getTenantId();
        return tenantId != null && !tenantId.isBlank() && Objects.equals(tenantId, location.getTenantId());
    }

    private void applyCurrentUsersTenant(Location location) {
        Optional<AppUser> currentUser = currentUser();
        if (currentUser.isEmpty() || isPlatformAdmin(currentUser.get())) {
            return;
        }
        String tenantId = currentUser.get().getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Authenticated tenant user does not have a tenant id");
        }
        location.setTenantId(tenantId);
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

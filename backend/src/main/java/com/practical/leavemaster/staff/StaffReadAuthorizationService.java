package com.practical.leavemaster.staff;

import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("staffReadAuthorization")
@RequiredArgsConstructor
public class StaffReadAuthorizationService {

    private static final String PLATFORM_ADMIN_AUTHORITY = "ROLE_PLATFORM_ADMIN";

    private final AppUserRepository appUserRepository;
    private final StaffRepository staffRepository;

    public boolean canRead(String staffId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || staffId == null || staffId.isBlank()) {
            return false;
        }

        if (hasAuthority(authentication, PLATFORM_ADMIN_AUTHORITY)) {
            return true;
        }

        AppUser user = appUserRepository.findById(authentication.getName()).orElse(null);
        if (user == null || user.getTenantId() == null) {
            return false;
        }

        Staff requestedStaff = staffRepository.findById(staffId).orElse(null);
        if (requestedStaff == null || !user.getTenantId().equals(requestedStaff.getTenantId())) {
            return false;
        }

        if (hasAuthority(authentication, RbacPermissions.STAFF_READ)) {
            return true;
        }

        return staffId.equals(user.getStaffId());
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}

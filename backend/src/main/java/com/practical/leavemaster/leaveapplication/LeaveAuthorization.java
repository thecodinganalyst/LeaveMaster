package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.leaveapprover.LeaveApprover;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component("leaveAuthorization")
@RequiredArgsConstructor
public class LeaveAuthorization {

    private final AppUserRepository appUserRepository;
    private final StaffRepository staffRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveApproverRepository leaveApproverRepository;

    /**
     * Allows an authenticated user to use a staff-scoped endpoint only for their own staff record.
     * Administrative users without a staff link retain their existing RBAC-based access, but remain
     * tenant-scoped when a tenant is assigned to the account.
     */
    public boolean canAccessStaff(Authentication authentication, String staffId) {
        Optional<AppUser> user = currentUser(authentication);
        if (user.isEmpty()) {
            return false;
        }

        Optional<Staff> staff = staffRepository.findById(staffId);
        if (staff.isEmpty()) {
            return true; // Let the controller preserve its existing 404 response.
        }

        if (!sameTenant(user.get(), staff.get().getTenantId())) {
            return false;
        }

        return isAdministrativeAccount(user.get()) || staffId.equals(user.get().getStaffId());
    }

    public boolean canReadApplication(Authentication authentication, String applicationId) {
        Optional<AppUser> user = currentUser(authentication);
        if (user.isEmpty()) {
            return false;
        }

        Optional<LeaveApplication> application = leaveApplicationRepository.findById(applicationId);
        if (application.isEmpty()) {
            return true; // Preserve 404 for unknown IDs rather than converting them to 403.
        }

        return canReadApplication(user.get(), application.get());
    }

    public boolean canWriteApplication(Authentication authentication, String applicationId) {
        Optional<AppUser> user = currentUser(authentication);
        if (user.isEmpty()) {
            return false;
        }

        Optional<LeaveApplication> application = leaveApplicationRepository.findById(applicationId);
        if (application.isEmpty()) {
            return true;
        }

        LeaveApplication leave = application.get();
        if (!sameTenant(user.get(), tenantIdOf(leave))) {
            return false;
        }

        return isAdministrativeAccount(user.get())
                || (leave.getStaff() != null && user.get().getStaffId().equals(leave.getStaff().getId()));
    }

    public boolean canApplyForStaff(Authentication authentication, String staffId) {
        return canAccessStaff(authentication, staffId);
    }

    public boolean canActAsApprover(Authentication authentication, String approverId) {
        Optional<AppUser> user = currentUser(authentication);
        if (user.isEmpty() || user.get().getStaffId() == null) {
            return false;
        }
        return user.get().getStaffId().equals(approverId);
    }

    public boolean canApproveAs(Authentication authentication, String applicationId, String approverId) {
        Optional<AppUser> user = currentUser(authentication);
        if (user.isEmpty() || user.get().getStaffId() == null || !user.get().getStaffId().equals(approverId)) {
            return false;
        }

        Optional<LeaveApplication> application = leaveApplicationRepository.findById(applicationId);
        if (application.isEmpty()) {
            return true;
        }

        return isAssignedApprover(user.get(), application.get());
    }

    public boolean canApproveApplication(Authentication authentication, String applicationId) {
        Optional<AppUser> user = currentUser(authentication);
        if (user.isEmpty() || user.get().getStaffId() == null) {
            return false;
        }

        Optional<LeaveApplication> application = leaveApplicationRepository.findById(applicationId);
        if (application.isEmpty()) {
            return true;
        }

        return isAssignedApprover(user.get(), application.get());
    }

    private boolean canReadApplication(AppUser user, LeaveApplication application) {
        if (!sameTenant(user, tenantIdOf(application))) {
            return false;
        }
        if (isAdministrativeAccount(user)) {
            return true;
        }
        if (application.getStaff() != null && user.getStaffId().equals(application.getStaff().getId())) {
            return true;
        }
        return isAssignedApprover(user, application);
    }

    private boolean isAssignedApprover(AppUser user, LeaveApplication application) {
        if (application.getStaff() == null || application.getLeaveDate() == null) {
            return false;
        }
        if (!sameTenant(user, tenantIdOf(application))) {
            return false;
        }
        return leaveApproverRepository.findActiveApproversForStaff(application.getStaff(), application.getLeaveDate())
                .stream()
                .map(LeaveApprover::getApprover)
                .filter(approver -> approver != null)
                .anyMatch(approver -> user.getStaffId().equals(approver.getId()));
    }

    private Optional<AppUser> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            Map<String, Object> attributes = oauth2Authentication.getPrincipal().getAttributes();
            Object subject = attributes.get("sub");
            if (subject == null) {
                subject = attributes.get("id");
            }
            if (subject == null) {
                return Optional.empty();
            }
            return appUserRepository.findByOidcProviderAndOidcSubject(
                    oauth2Authentication.getAuthorizedClientRegistrationId(), subject.toString());
        }

        return appUserRepository.findById(authentication.getName());
    }

    private boolean isAdministrativeAccount(AppUser user) {
        return user.getStaffId() == null || user.getStaffId().isBlank();
    }

    private boolean sameTenant(AppUser user, String resourceTenantId) {
        String userTenantId = user.getTenantId();
        if (userTenantId == null || userTenantId.isBlank()) {
            return true; // Platform-scoped account; RBAC remains the capability gate.
        }
        return userTenantId.equals(resourceTenantId);
    }

    private String tenantIdOf(LeaveApplication application) {
        if (application.getTenantId() != null && !application.getTenantId().isBlank()) {
            return application.getTenantId();
        }
        return application.getStaff() != null ? application.getStaff().getTenantId() : null;
    }
}

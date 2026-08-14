package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveEntitlementPolicyService {
    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";

    private final LeaveEntitlementPolicyRepository policyRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final TenantActivityService tenantActivityService;
    private final AppUserRepository appUserRepository;

    public List<LeaveEntitlementPolicy> findAll() {
        Optional<AppUser> user = currentUser();
        if (user.isPresent() && !isPlatformAdmin(user.get())) {
            String tenantId = requiredTenantId(user.get());
            return policyRepository.findAllByTenantId(tenantId);
        }
        return policyRepository.findAll();
    }

    public Optional<LeaveEntitlementPolicy> findById(String id) {
        return policyRepository.findById(id).filter(this::isAccessibleToCurrentUser);
    }

    @Transactional
    public LeaveEntitlementPolicy create(LeaveEntitlementPolicy policy) {
        applyCurrentUsersTenant(policy);
        validate(policy);
        LeaveEntitlementPolicy saved = policyRepository.save(policy);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    @Transactional
    public LeaveEntitlementPolicy update(String id, LeaveEntitlementPolicy requested) {
        LeaveEntitlementPolicy existing = findById(id)
                .orElseThrow(() -> new LeaveEntitlementPolicyNotFoundException(id));
        existing.setLeaveTypeId(requested.getLeaveTypeId());
        existing.setName(requested.getName());
        existing.setActive(requested.isActive());
        existing.setPriority(requested.getPriority());
        existing.setEntitlementUnit(requested.getEntitlementUnit());
        existing.setEntitlementAmount(requested.getEntitlementAmount());
        existing.setAccrualMethod(requested.getAccrualMethod());
        existing.setAccrualRate(requested.getAccrualRate());
        existing.setProrationMethod(requested.getProrationMethod());
        existing.setCarryForwardAllowed(requested.isCarryForwardAllowed());
        existing.setCarryForwardLimit(requested.getCarryForwardLimit());
        existing.setCarryForwardExpiryMonths(requested.getCarryForwardExpiryMonths());
        existing.setEffectiveFrom(requested.getEffectiveFrom());
        existing.setEffectiveTo(requested.getEffectiveTo());
        validate(existing);
        LeaveEntitlementPolicy saved = policyRepository.save(existing);
        tenantActivityService.touch(saved.getTenantId());
        return saved;
    }

    @Transactional
    public void delete(String id) {
        LeaveEntitlementPolicy existing = findById(id)
                .orElseThrow(() -> new LeaveEntitlementPolicyNotFoundException(id));
        policyRepository.delete(existing);
        tenantActivityService.touch(existing.getTenantId());
    }

    private void validate(LeaveEntitlementPolicy policy) {
        if (policy.getTenantId() == null || policy.getTenantId().isBlank()) {
            throw new LeaveEntitlementPolicyValidationException("tenantId is required");
        }
        if (policy.getLeaveTypeId() == null || policy.getLeaveTypeId().isBlank()) {
            throw new LeaveEntitlementPolicyValidationException("leaveTypeId is required");
        }
        LeaveType leaveType = leaveTypeRepository.findById(policy.getLeaveTypeId())
                .orElseThrow(() -> new LeaveEntitlementPolicyValidationException("Unknown leaveTypeId: " + policy.getLeaveTypeId()));
        if (!Objects.equals(policy.getTenantId(), leaveType.getTenantId())) {
            throw new LeaveEntitlementPolicyValidationException("Policy tenant must match leave type tenant");
        }
        if (policy.getName() == null || policy.getName().isBlank()) {
            throw new LeaveEntitlementPolicyValidationException("name is required");
        }
        if (policy.getEntitlementUnit() == null || policy.getAccrualMethod() == null || policy.getProrationMethod() == null) {
            throw new LeaveEntitlementPolicyValidationException("entitlementUnit, accrualMethod and prorationMethod are required");
        }
        requireNonNegative(policy.getEntitlementAmount(), "entitlementAmount", true);
        requireNonNegative(policy.getAccrualRate(), "accrualRate", false);
        requireNonNegative(policy.getCarryForwardLimit(), "carryForwardLimit", false);
        if (policy.getCarryForwardExpiryMonths() != null && policy.getCarryForwardExpiryMonths() < 0) {
            throw new LeaveEntitlementPolicyValidationException("carryForwardExpiryMonths cannot be negative");
        }
        if (policy.getEffectiveFrom() == null) {
            throw new LeaveEntitlementPolicyValidationException("effectiveFrom is required");
        }
        if (policy.getEffectiveTo() != null && policy.getEffectiveTo().isBefore(policy.getEffectiveFrom())) {
            throw new LeaveEntitlementPolicyValidationException("effectiveTo cannot be before effectiveFrom");
        }
        if (!policy.isCarryForwardAllowed() && (positive(policy.getCarryForwardLimit()) || (policy.getCarryForwardExpiryMonths() != null && policy.getCarryForwardExpiryMonths() > 0))) {
            throw new LeaveEntitlementPolicyValidationException("carry-forward limit/expiry require carryForwardAllowed=true");
        }
    }

    private void requireNonNegative(BigDecimal value, String field, boolean required) {
        if (required && value == null) {
            throw new LeaveEntitlementPolicyValidationException(field + " is required");
        }
        if (value != null && value.signum() < 0) {
            throw new LeaveEntitlementPolicyValidationException(field + " cannot be negative");
        }
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private boolean isAccessibleToCurrentUser(LeaveEntitlementPolicy policy) {
        Optional<AppUser> user = currentUser();
        return user.isEmpty() || isPlatformAdmin(user.get()) || Objects.equals(requiredTenantId(user.get()), policy.getTenantId());
    }

    private void applyCurrentUsersTenant(LeaveEntitlementPolicy policy) {
        Optional<AppUser> user = currentUser();
        if (user.isPresent() && !isPlatformAdmin(user.get())) {
            policy.setTenantId(requiredTenantId(user.get()));
        }
    }

    private Optional<AppUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return Optional.empty();
        }
        return appUserRepository.findById(authentication.getName());
    }

    private String requiredTenantId(AppUser user) {
        if (user.getTenantId() == null || user.getTenantId().isBlank()) {
            throw new IllegalStateException("Authenticated tenant user does not have a tenant id");
        }
        return user.getTenantId();
    }

    private boolean isPlatformAdmin(AppUser user) {
        return user != null && user.isActive() && user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role != null && role.isActive() && PLATFORM_ADMIN_ROLE_ID.equalsIgnoreCase(role.getId()));
    }
}

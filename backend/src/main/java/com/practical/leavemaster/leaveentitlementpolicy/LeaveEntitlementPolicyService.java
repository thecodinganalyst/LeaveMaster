package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
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
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveEntitlementPolicyService {
    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    private final LeaveEntitlementPolicyRepository policyRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final TenantActivityService tenantActivityService;
    private final AppUserRepository appUserRepository;

    public List<LeaveEntitlementPolicy> findAll() {
        Optional<AppUser> user = currentUser();
        if (user.isEmpty()) {
            return policyRepository.findAll();
        }
        if (isPlatformAdmin(user.get())) {
            return policyRepository.findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE);
        }
        return policyRepository.findAllByTenantId(requiredTenantId(user.get())).stream()
                .filter(policy -> policy.getScope() == ConfigurationScope.TENANT)
                .toList();
    }

    public Optional<LeaveEntitlementPolicy> findById(String id) {
        return policyRepository.findById(id).filter(this::isAccessibleToCurrentUser);
    }

    @Transactional
    public LeaveEntitlementPolicy create(LeaveEntitlementPolicy policy) {
        applyCurrentUsersScope(policy);
        normalisePolicyModel(policy);
        normaliseEventConfiguration(policy);
        normaliseAccrualConfiguration(policy, false);
        validate(policy);
        if (policy.getId() != null && !policy.getId().isBlank() && policyRepository.existsById(policy.getId())) {
            throw new LeaveEntitlementPolicyValidationException("Entitlement policy ID already exists: " + policy.getId());
        }
        LeaveEntitlementPolicy saved = policyRepository.save(policy);
        touchTenant(saved);
        return saved;
    }

    @Transactional
    public LeaveEntitlementPolicy update(String id, LeaveEntitlementPolicy requested) {
        LeaveEntitlementPolicy existing = findById(id)
                .orElseThrow(() -> new LeaveEntitlementPolicyNotFoundException(id));
        existing.setLeaveTypeId(requested.getLeaveTypeId());
        existing.setJurisdictionLeaveTypeId(requested.getJurisdictionLeaveTypeId());
        existing.setJurisdictionId(requested.getJurisdictionId());
        existing.setName(requested.getName());
        existing.setActive(requested.isActive());
        existing.setPriority(requested.getPriority());
        existing.setPolicyModel(requested.getPolicyModel());
        existing.setQualifyingEventTypeCode(requested.getQualifyingEventTypeCode());
        existing.setEventRequiresVerification(requested.isEventRequiresVerification());
        existing.setEventValidityDaysBefore(requested.getEventValidityDaysBefore());
        existing.setEventValidityDaysAfter(requested.getEventValidityDaysAfter());
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
        normalisePolicyModel(existing);
        normaliseEventConfiguration(existing);
        normaliseAccrualConfiguration(existing, true);
        validate(existing);
        LeaveEntitlementPolicy saved = policyRepository.save(existing);
        touchTenant(saved);
        return saved;
    }

    @Transactional
    public void delete(String id) {
        LeaveEntitlementPolicy existing = findById(id)
                .orElseThrow(() -> new LeaveEntitlementPolicyNotFoundException(id));
        policyRepository.delete(existing);
        touchTenant(existing);
    }

    private void normalisePolicyModel(LeaveEntitlementPolicy policy) {
        if (policy.getPolicyModel() == null) {
            policy.setPolicyModel(LeavePolicyModel.ANNUAL_ENTITLEMENT);
        }
    }

    private void normaliseEventConfiguration(LeaveEntitlementPolicy policy) {
        if (policy.getPolicyModel() != LeavePolicyModel.EVENT_BASED) {
            policy.setQualifyingEventTypeCode(null);
            policy.setEventRequiresVerification(false);
            policy.setEventValidityDaysBefore(null);
            policy.setEventValidityDaysAfter(null);
            return;
        }
        if (policy.getQualifyingEventTypeCode() != null) {
            String normalized = policy.getQualifyingEventTypeCode().trim().toUpperCase(Locale.ROOT);
            policy.setQualifyingEventTypeCode(normalized.isBlank() ? null : normalized);
        }
    }

    private void normaliseAccrualConfiguration(LeaveEntitlementPolicy policy, boolean migrateLegacyAnnual) {
        if (policy.getAccrualMethod() == null) {
            return;
        }
        if (policy.getAccrualMethod() == AccrualMethod.PER_PAY_PERIOD) {
            throw new LeaveEntitlementPolicyValidationException("PER_PAY_PERIOD accrual is not supported until payroll schedules are implemented");
        }
        if (policy.getAccrualMethod() == AccrualMethod.ANNUAL) {
            if (!migrateLegacyAnnual) {
                throw new LeaveEntitlementPolicyValidationException("ANNUAL accrual is no longer configurable; use NONE for front-loaded entitlement");
            }
            policy.setAccrualMethod(AccrualMethod.NONE);
        }
        if (policy.getAccrualMethod() == AccrualMethod.NONE) {
            policy.setAccrualRate(null);
            return;
        }
        if (policy.getAccrualMethod() == AccrualMethod.MONTHLY && policy.getEntitlementAmount() != null) {
            policy.setAccrualRate(policy.getEntitlementAmount().divide(MONTHS_PER_YEAR, 8, RoundingMode.HALF_UP));
        }
    }

    private void validate(LeaveEntitlementPolicy policy) {
        if (policy.getScope() == null) {
            throw new LeaveEntitlementPolicyValidationException("scope is required");
        }
        if (policy.getScope() == ConfigurationScope.PLATFORM_TEMPLATE) {
            validateTemplateScope(policy);
        } else {
            validateTenantScope(policy);
        }
        if (policy.getName() == null || policy.getName().isBlank()) {
            throw new LeaveEntitlementPolicyValidationException("name is required");
        }
        if (policy.getPolicyModel() == null) {
            throw new LeaveEntitlementPolicyValidationException("policyModel is required");
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
        validatePolicyModelConfiguration(policy);
    }

    private void validatePolicyModelConfiguration(LeaveEntitlementPolicy policy) {
        if (policy.getPolicyModel() != LeavePolicyModel.EVENT_BASED) {
            return;
        }
        if (policy.getAccrualMethod() != AccrualMethod.NONE) {
            throw new LeaveEntitlementPolicyValidationException("EVENT_BASED policies cannot use recurring accrual");
        }
        if (policy.getProrationMethod() != ProrationMethod.NONE) {
            throw new LeaveEntitlementPolicyValidationException("EVENT_BASED policies cannot use annual proration");
        }
        if (policy.isCarryForwardAllowed() || positive(policy.getCarryForwardLimit())
                || (policy.getCarryForwardExpiryMonths() != null && policy.getCarryForwardExpiryMonths() > 0)) {
            throw new LeaveEntitlementPolicyValidationException("EVENT_BASED policies cannot carry forward an annual balance");
        }
        if (policy.getQualifyingEventTypeCode() == null || policy.getQualifyingEventTypeCode().isBlank()) {
            throw new LeaveEntitlementPolicyValidationException("qualifyingEventTypeCode is required for EVENT_BASED policies");
        }
        if (policy.getEntitlementAmount() == null || policy.getEntitlementAmount().signum() <= 0) {
            throw new LeaveEntitlementPolicyValidationException("EVENT_BASED policies require a positive entitlementAmount");
        }
        requireNonNegativeInteger(policy.getEventValidityDaysBefore(), "eventValidityDaysBefore");
        requireNonNegativeInteger(policy.getEventValidityDaysAfter(), "eventValidityDaysAfter");
    }

    private void validateTemplateScope(LeaveEntitlementPolicy policy) {
        if (policy.getTenantId() != null) {
            throw new LeaveEntitlementPolicyValidationException("Platform templates must not have a tenantId");
        }
        if (policy.getJurisdictionId() == null || policy.getJurisdictionId().isBlank()) {
            throw new LeaveEntitlementPolicyValidationException("jurisdictionId is required for platform templates");
        }
        if (policy.getJurisdictionLeaveTypeId() == null || policy.getJurisdictionLeaveTypeId().isBlank()) {
            throw new LeaveEntitlementPolicyValidationException("jurisdictionLeaveTypeId is required for platform templates");
        }
        if (policy.getLeaveTypeId() != null) {
            throw new LeaveEntitlementPolicyValidationException("Platform templates must not reference a tenant leaveTypeId");
        }
    }

    private void validateTenantScope(LeaveEntitlementPolicy policy) {
        if (policy.getTenantId() == null || policy.getTenantId().isBlank()) {
            throw new LeaveEntitlementPolicyValidationException("tenantId is required");
        }
        if (policy.getLeaveTypeId() == null || policy.getLeaveTypeId().isBlank()) {
            throw new LeaveEntitlementPolicyValidationException("leaveTypeId is required");
        }
        if (policy.getJurisdictionId() != null || policy.getJurisdictionLeaveTypeId() != null) {
            throw new LeaveEntitlementPolicyValidationException("Tenant policies must not contain platform template jurisdiction references");
        }
        LeaveType leaveType = leaveTypeRepository.findById(policy.getLeaveTypeId())
                .orElseThrow(() -> new LeaveEntitlementPolicyValidationException("Unknown leaveTypeId: " + policy.getLeaveTypeId()));
        if (!Objects.equals(policy.getTenantId(), leaveType.getTenantId())) {
            throw new LeaveEntitlementPolicyValidationException("Policy tenant must match leave type tenant");
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

    private void requireNonNegativeInteger(Integer value, String field) {
        if (value != null && value < 0) {
            throw new LeaveEntitlementPolicyValidationException(field + " cannot be negative");
        }
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private boolean isAccessibleToCurrentUser(LeaveEntitlementPolicy policy) {
        Optional<AppUser> user = currentUser();
        if (user.isEmpty()) {
            return true;
        }
        if (isPlatformAdmin(user.get())) {
            return policy.getScope() == ConfigurationScope.PLATFORM_TEMPLATE && policy.getTenantId() == null;
        }
        return policy.getScope() == ConfigurationScope.TENANT
                && Objects.equals(requiredTenantId(user.get()), policy.getTenantId());
    }

    private void applyCurrentUsersScope(LeaveEntitlementPolicy policy) {
        Optional<AppUser> user = currentUser();
        if (user.isEmpty()) {
            return;
        }
        if (isPlatformAdmin(user.get())) {
            policy.setScope(ConfigurationScope.PLATFORM_TEMPLATE);
            policy.setTenantId(null);
            policy.setLeaveTypeId(null);
            policy.setSourceTemplateId(null);
        } else {
            policy.setScope(ConfigurationScope.TENANT);
            policy.setTenantId(requiredTenantId(user.get()));
            policy.setJurisdictionId(null);
            policy.setJurisdictionLeaveTypeId(null);
            policy.setSourceTemplateId(null);
        }
    }

    private void touchTenant(LeaveEntitlementPolicy policy) {
        if (policy.getScope() == ConfigurationScope.TENANT && policy.getTenantId() != null) {
            tenantActivityService.touch(policy.getTenantId());
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

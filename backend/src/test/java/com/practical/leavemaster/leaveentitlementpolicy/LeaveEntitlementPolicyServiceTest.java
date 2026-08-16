package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveEntitlementPolicyServiceTest {
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private TenantActivityService tenantActivityService;
    @Mock private AppUserRepository appUserRepository;
    @InjectMocks private LeaveEntitlementPolicyService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantUserOnlySeesOwnTenantPolicies() {
        authenticateTenantUser("hr", "tenant-a");
        when(policyRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(validPolicy("tenant-a", "annual")));

        assertThat(service.findAll()).hasSize(1).allMatch(policy -> policy.getTenantId().equals("tenant-a"));
        verify(policyRepository, never()).findAll();
    }

    @Test
    void platformAdminOnlySeesPlatformTemplates() {
        authenticatePlatformAdmin("platform");
        LeaveEntitlementPolicy template = validTemplate("SG", "SG:ANNUAL_LEAVE");
        when(policyRepository.findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE)).thenReturn(List.of(template));

        assertThat(service.findAll()).containsExactly(template);
        verify(policyRepository).findAllByScope(ConfigurationScope.PLATFORM_TEMPLATE);
        verify(policyRepository, never()).findAll();
        verify(policyRepository, never()).findAllByTenantId(any());
    }

    @Test
    void platformAdminCreatesOnlyPlatformTemplate() {
        authenticatePlatformAdmin("platform");
        LeaveEntitlementPolicy requested = validTemplate("SG", "SG:ANNUAL_LEAVE");
        requested.setTenantId("tenant-a");
        requested.setLeaveTypeId("annual");
        when(policyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveEntitlementPolicy saved = service.create(requested);

        assertThat(saved.getScope()).isEqualTo(ConfigurationScope.PLATFORM_TEMPLATE);
        assertThat(saved.getTenantId()).isNull();
        assertThat(saved.getLeaveTypeId()).isNull();
        assertThat(saved.getJurisdictionId()).isEqualTo("SG");
        verify(tenantActivityService, never()).touch(any());
    }

    @Test
    void tenantUserCannotReadAnotherTenantsPolicy() {
        authenticateTenantUser("hr", "tenant-a");
        when(policyRepository.findById("policy-b"))
                .thenReturn(Optional.of(validPolicy("tenant-b", "annual-b")));

        assertThat(service.findById("policy-b")).isEmpty();
    }

    @Test
    void platformAdminCannotReadTenantPolicy() {
        authenticatePlatformAdmin("platform");
        when(policyRepository.findById("policy-a")).thenReturn(Optional.of(validPolicy("tenant-a", "annual")));
        assertThat(service.findById("policy-a")).isEmpty();
    }

    @Test
    void tenantUserWithoutTenantIdIsRejected() {
        authenticateTenantUser("hr", null);
        assertThatThrownBy(() -> service.findAll())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenant id");
    }

    @Test
    void tenantUserCannotOverrideTenantOnCreate() {
        authenticateTenantUser("hr", "tenant-a");
        LeaveEntitlementPolicy policy = validPolicy("tenant-b", "annual");
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-a")));
        when(policyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveEntitlementPolicy saved = service.create(policy);

        assertThat(saved.getTenantId()).isEqualTo("tenant-a");
        assertThat(saved.getScope()).isEqualTo(ConfigurationScope.TENANT);
        verify(tenantActivityService).touch("tenant-a");
    }

    @Test
    void derivesMonthlyRateAndIgnoresClientSuppliedRate() {
        LeaveEntitlementPolicy policy = validPolicy("tenant-a", "annual");
        policy.setAccrualMethod(AccrualMethod.MONTHLY);
        policy.setAccrualRate(new BigDecimal("99"));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-a")));
        when(policyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveEntitlementPolicy saved = service.create(policy);

        assertThat(saved.getAccrualRate()).isEqualByComparingTo("1.16666667");
    }

    @Test
    void clearsAccrualRateForFrontLoadedPolicy() {
        LeaveEntitlementPolicy policy = validPolicy("tenant-a", "annual");
        policy.setAccrualRate(new BigDecimal("3"));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-a")));
        when(policyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create(policy).getAccrualRate()).isNull();
    }

    @Test
    void rejectsUnsupportedAccrualMethodsOnCreate() {
        LeaveEntitlementPolicy annual = validPolicy("tenant-a", "annual");
        annual.setAccrualMethod(AccrualMethod.ANNUAL);
        assertThatThrownBy(() -> service.create(annual))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class)
                .hasMessageContaining("ANNUAL accrual is no longer configurable");

        LeaveEntitlementPolicy payPeriod = validPolicy("tenant-a", "annual");
        payPeriod.setAccrualMethod(AccrualMethod.PER_PAY_PERIOD);
        assertThatThrownBy(() -> service.create(payPeriod))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class)
                .hasMessageContaining("PER_PAY_PERIOD accrual is not supported");
    }

    @Test
    void migratesLegacyAnnualPolicyToFrontLoadedWhenUpdated() {
        LeaveEntitlementPolicy existing = validPolicy("tenant-a", "annual");
        existing.setId("policy-1");
        existing.setAccrualMethod(AccrualMethod.ANNUAL);
        existing.setAccrualRate(new BigDecimal("14"));
        LeaveEntitlementPolicy requested = validPolicy("tenant-a", "annual");
        requested.setAccrualMethod(AccrualMethod.ANNUAL);
        requested.setAccrualRate(new BigDecimal("14"));
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(existing));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-a")));
        when(policyRepository.save(existing)).thenReturn(existing);

        LeaveEntitlementPolicy saved = service.update("policy-1", requested);

        assertThat(saved.getAccrualMethod()).isEqualTo(AccrualMethod.NONE);
        assertThat(saved.getAccrualRate()).isNull();
    }

    @Test
    void rejectsMissingRequiredFieldsAndUnknownLeaveType() {
        LeaveEntitlementPolicy missingTenant = validPolicy(null, "annual");
        assertThatThrownBy(() -> service.create(missingTenant)).isInstanceOf(LeaveEntitlementPolicyValidationException.class).hasMessageContaining("tenantId");

        LeaveEntitlementPolicy missingLeaveType = validPolicy("tenant-a", null);
        assertThatThrownBy(() -> service.create(missingLeaveType)).isInstanceOf(LeaveEntitlementPolicyValidationException.class).hasMessageContaining("leaveTypeId");

        LeaveEntitlementPolicy unknownLeaveType = validPolicy("tenant-a", "missing");
        when(leaveTypeRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(unknownLeaveType)).isInstanceOf(LeaveEntitlementPolicyValidationException.class).hasMessageContaining("Unknown leaveTypeId");
    }

    @Test
    void rejectsInvalidPlatformTemplateShape() {
        LeaveEntitlementPolicy missingJurisdiction = validTemplate(null, "SG:ANNUAL_LEAVE");
        assertThatThrownBy(() -> service.create(missingJurisdiction)).hasMessageContaining("jurisdictionId");

        LeaveEntitlementPolicy missingJurisdictionLeaveType = validTemplate("SG", null);
        assertThatThrownBy(() -> service.create(missingJurisdictionLeaveType)).hasMessageContaining("jurisdictionLeaveTypeId");
    }

    @Test
    void rejectsLeaveTypeFromAnotherTenant() {
        LeaveEntitlementPolicy policy = validPolicy("tenant-a", "annual");
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-b")));
        assertThatThrownBy(() -> service.create(policy)).isInstanceOf(LeaveEntitlementPolicyValidationException.class).hasMessageContaining("tenant must match");
    }

    @Test
    void rejectsMissingNameEnumsEntitlementAndEffectiveDate() {
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-a")));
        LeaveEntitlementPolicy missingName = validPolicy("tenant-a", "annual");
        missingName.setName(" ");
        assertThatThrownBy(() -> service.create(missingName)).hasMessageContaining("name is required");
        LeaveEntitlementPolicy missingUnit = validPolicy("tenant-a", "annual");
        missingUnit.setEntitlementUnit(null);
        assertThatThrownBy(() -> service.create(missingUnit)).hasMessageContaining("entitlementUnit");
        LeaveEntitlementPolicy missingAmount = validPolicy("tenant-a", "annual");
        missingAmount.setEntitlementAmount(null);
        assertThatThrownBy(() -> service.create(missingAmount)).hasMessageContaining("entitlementAmount is required");
        LeaveEntitlementPolicy missingEffectiveFrom = validPolicy("tenant-a", "annual");
        missingEffectiveFrom.setEffectiveFrom(null);
        assertThatThrownBy(() -> service.create(missingEffectiveFrom)).hasMessageContaining("effectiveFrom is required");
    }

    @Test
    void rejectsNegativeEntitlementCarryForwardAndExpiry() {
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-a")));
        LeaveEntitlementPolicy negativeEntitlement = validPolicy("tenant-a", "annual");
        negativeEntitlement.setEntitlementAmount(new BigDecimal("-1"));
        assertThatThrownBy(() -> service.create(negativeEntitlement)).hasMessageContaining("entitlementAmount");
        LeaveEntitlementPolicy negativeCarryForward = validPolicy("tenant-a", "annual");
        negativeCarryForward.setCarryForwardAllowed(true);
        negativeCarryForward.setCarryForwardLimit(new BigDecimal("-1"));
        assertThatThrownBy(() -> service.create(negativeCarryForward)).hasMessageContaining("carryForwardLimit");
        LeaveEntitlementPolicy negativeExpiry = validPolicy("tenant-a", "annual");
        negativeExpiry.setCarryForwardAllowed(true);
        negativeExpiry.setCarryForwardExpiryMonths(-1);
        assertThatThrownBy(() -> service.create(negativeExpiry)).hasMessageContaining("carryForwardExpiryMonths");
    }

    @Test
    void rejectsInvalidEffectiveRange() {
        LeaveEntitlementPolicy policy = validPolicy("tenant-a", "annual");
        policy.setEffectiveTo(policy.getEffectiveFrom().minusDays(1));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-a")));
        assertThatThrownBy(() -> service.create(policy)).isInstanceOf(LeaveEntitlementPolicyValidationException.class).hasMessageContaining("effectiveTo");
    }

    @Test
    void rejectsCarryForwardConfigurationWhenDisabled() {
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-a")));
        LeaveEntitlementPolicy carryForwardLimit = validPolicy("tenant-a", "annual");
        carryForwardLimit.setCarryForwardAllowed(false);
        carryForwardLimit.setCarryForwardLimit(new BigDecimal("5"));
        assertThatThrownBy(() -> service.create(carryForwardLimit)).isInstanceOf(LeaveEntitlementPolicyValidationException.class).hasMessageContaining("carryForwardAllowed");
        LeaveEntitlementPolicy carryForwardExpiry = validPolicy("tenant-a", "annual");
        carryForwardExpiry.setCarryForwardExpiryMonths(12);
        assertThatThrownBy(() -> service.create(carryForwardExpiry)).isInstanceOf(LeaveEntitlementPolicyValidationException.class).hasMessageContaining("carryForwardAllowed");
    }

    @Test
    void updatesPolicyWithoutChangingTenant() {
        LeaveEntitlementPolicy existing = validPolicy("tenant-a", "annual");
        existing.setId("policy-1");
        LeaveEntitlementPolicy update = validPolicy("tenant-b", "annual");
        update.setName("Senior annual leave");
        update.setEntitlementAmount(new BigDecimal("18"));
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(existing));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-a")));
        when(policyRepository.save(existing)).thenReturn(existing);
        LeaveEntitlementPolicy saved = service.update("policy-1", update);
        assertThat(saved.getTenantId()).isEqualTo("tenant-a");
        assertThat(saved.getName()).isEqualTo("Senior annual leave");
        assertThat(saved.getEntitlementAmount()).isEqualByComparingTo("18");
    }

    @Test
    void updateAndDeleteThrowWhenPolicyIsMissing() {
        when(policyRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update("missing", validPolicy("tenant-a", "annual"))).isInstanceOf(LeaveEntitlementPolicyNotFoundException.class);
        assertThatThrownBy(() -> service.delete("missing")).isInstanceOf(LeaveEntitlementPolicyNotFoundException.class);
    }

    @Test
    void deletesAccessiblePolicy() {
        LeaveEntitlementPolicy existing = validPolicy("tenant-a", "annual");
        existing.setId("policy-1");
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(existing));
        service.delete("policy-1");
        verify(policyRepository).delete(existing);
        verify(tenantActivityService).touch("tenant-a");
    }

    private void authenticateTenantUser(String login, String tenantId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, "n/a", List.of()));
        when(appUserRepository.findById(login)).thenReturn(Optional.of(AppUser.builder().loginName(login).active(true).tenantId(tenantId).roles(Set.of()).build()));
    }

    private void authenticatePlatformAdmin(String login) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, "n/a", List.of()));
        when(appUserRepository.findById(login)).thenReturn(Optional.of(AppUser.builder().loginName(login).active(true)
                .roles(Set.of(AppRole.builder().id("PLATFORM_ADMIN").active(true).description("Platform admin").build())).build()));
    }

    private LeaveType leaveType(String id, String tenantId) {
        return LeaveType.builder().id(id).name("Annual Leave").tenantId(tenantId).build();
    }

    private LeaveEntitlementPolicy validPolicy(String tenantId, String leaveTypeId) {
        return LeaveEntitlementPolicy.builder().tenantId(tenantId).scope(ConfigurationScope.TENANT).leaveTypeId(leaveTypeId)
                .name("Annual leave policy").active(true).priority(10).entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(new BigDecimal("14")).accrualMethod(AccrualMethod.NONE).prorationMethod(ProrationMethod.MONTHS)
                .carryForwardAllowed(false).effectiveFrom(LocalDate.of(2026, 1, 1)).build();
    }

    private LeaveEntitlementPolicy validTemplate(String jurisdictionId, String jurisdictionLeaveTypeId) {
        return LeaveEntitlementPolicy.builder().scope(ConfigurationScope.PLATFORM_TEMPLATE).jurisdictionId(jurisdictionId)
                .jurisdictionLeaveTypeId(jurisdictionLeaveTypeId).name("Annual leave policy").active(true).priority(10)
                .entitlementUnit(EntitlementUnit.DAYS).entitlementAmount(new BigDecimal("14")).accrualMethod(AccrualMethod.NONE)
                .prorationMethod(ProrationMethod.MONTHS).carryForwardAllowed(false).effectiveFrom(LocalDate.of(2026, 1, 1)).build();
    }
}

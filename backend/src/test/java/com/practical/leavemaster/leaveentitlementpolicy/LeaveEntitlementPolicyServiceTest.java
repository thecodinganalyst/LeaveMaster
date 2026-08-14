package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
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
    void tenantUserCannotOverrideTenantOnCreate() {
        authenticateTenantUser("hr", "tenant-a");
        LeaveEntitlementPolicy policy = validPolicy("tenant-b", "annual");
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-a")));
        when(policyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveEntitlementPolicy saved = service.create(policy);

        assertThat(saved.getTenantId()).isEqualTo("tenant-a");
        verify(tenantActivityService).touch("tenant-a");
    }

    @Test
    void rejectsLeaveTypeFromAnotherTenant() {
        LeaveEntitlementPolicy policy = validPolicy("tenant-a", "annual");
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-b")));

        assertThatThrownBy(() -> service.create(policy))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class)
                .hasMessageContaining("tenant must match");
    }

    @Test
    void rejectsNegativeEntitlementAndInvalidEffectiveRange() {
        LeaveEntitlementPolicy policy = validPolicy("tenant-a", "annual");
        policy.setEntitlementAmount(new BigDecimal("-1"));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-a")));

        assertThatThrownBy(() -> service.create(policy))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class)
                .hasMessageContaining("entitlementAmount");

        policy.setEntitlementAmount(new BigDecimal("14"));
        policy.setEffectiveTo(policy.getEffectiveFrom().minusDays(1));
        assertThatThrownBy(() -> service.create(policy))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class)
                .hasMessageContaining("effectiveTo");
    }

    @Test
    void rejectsCarryForwardConfigurationWhenDisabled() {
        LeaveEntitlementPolicy policy = validPolicy("tenant-a", "annual");
        policy.setCarryForwardAllowed(false);
        policy.setCarryForwardLimit(new BigDecimal("5"));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType("annual", "tenant-a")));

        assertThatThrownBy(() -> service.create(policy))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class)
                .hasMessageContaining("carryForwardAllowed");
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
        when(appUserRepository.findById(login)).thenReturn(Optional.of(AppUser.builder()
                .loginName(login)
                .active(true)
                .tenantId(tenantId)
                .roles(Set.of())
                .build()));
    }

    private LeaveType leaveType(String id, String tenantId) {
        return LeaveType.builder().id(id).name("Annual Leave").tenantId(tenantId).build();
    }

    private LeaveEntitlementPolicy validPolicy(String tenantId, String leaveTypeId) {
        return LeaveEntitlementPolicy.builder()
                .tenantId(tenantId)
                .leaveTypeId(leaveTypeId)
                .name("Annual leave policy")
                .active(true)
                .priority(10)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(new BigDecimal("14"))
                .accrualMethod(AccrualMethod.ANNUAL)
                .prorationMethod(ProrationMethod.MONTHS)
                .carryForwardAllowed(false)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();
    }
}

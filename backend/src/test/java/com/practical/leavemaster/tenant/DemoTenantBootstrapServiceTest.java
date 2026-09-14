package com.practical.leavemaster.tenant;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoTenantBootstrapServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private DemoTenantSeedService demoTenantSeedService;
    @Mock private PasswordEncoder passwordEncoder;

    private DemoTenantBootstrapService service;

    @BeforeEach
    void setUp() {
        service = new DemoTenantBootstrapService(
                tenantRepository,
                appUserRepository,
                demoTenantSeedService,
                passwordEncoder);
        ReflectionTestUtils.setField(service, "configuredTenantId", "DEMO");
        ReflectionTestUtils.setField(service, "demoPassword", "Demo123!");
    }

    @Test
    void createsConfiguredDemoTenantWhenMissing() {
        when(tenantRepository.findById("DEMO")).thenReturn(Optional.empty());
        when(demoTenantSeedService.resetConfiguredDemoTenant()).thenReturn(seedResult("DEMO"));

        DemoTenantBootstrapService.DemoBootstrapResult result = service.ensureConfiguredDemoTenantReady();

        assertThat(result.tenantId()).isEqualTo("DEMO");
        assertThat(result.action()).isEqualTo(DemoTenantBootstrapService.DemoBootstrapAction.CREATED);
        verify(demoTenantSeedService).resetConfiguredDemoTenant();
    }

    @Test
    void leavesCompleteDemoTenantUntouched() {
        Tenant tenant = Tenant.builder().id("DEMO").type(TenantType.DEMO).build();
        when(tenantRepository.findById("DEMO")).thenReturn(Optional.of(tenant));
        stubUsablePersona("demo.staff", "staff-hash");
        stubUsablePersona("demo.manager", "manager-hash");
        stubUsablePersona("demo.hr", "hr-hash");

        DemoTenantBootstrapService.DemoBootstrapResult result = service.ensureConfiguredDemoTenantReady();

        assertThat(result.action()).isEqualTo(DemoTenantBootstrapService.DemoBootstrapAction.ALREADY_READY);
        verify(demoTenantSeedService, never()).resetConfiguredDemoTenant();
        verify(demoTenantSeedService, never()).resetExistingDemoTenant("DEMO");
    }

    @Test
    void repairsDemoTenantWhenRequiredPersonaIsMissing() {
        Tenant tenant = Tenant.builder().id("DEMO").type(TenantType.DEMO).build();
        when(tenantRepository.findById("DEMO")).thenReturn(Optional.of(tenant));
        stubUsablePersona("demo.staff", "staff-hash");
        when(appUserRepository.findByTenantIdAndLoginName("DEMO", "demo.manager"))
                .thenReturn(Optional.empty());
        when(demoTenantSeedService.resetExistingDemoTenant("DEMO")).thenReturn(seedResult("DEMO"));

        DemoTenantBootstrapService.DemoBootstrapResult result = service.ensureConfiguredDemoTenantReady();

        assertThat(result.action()).isEqualTo(DemoTenantBootstrapService.DemoBootstrapAction.REPAIRED);
        verify(demoTenantSeedService).resetExistingDemoTenant("DEMO");
    }

    @Test
    void repairsDemoTenantWhenRequiredPersonaPasswordIsStale() {
        Tenant tenant = Tenant.builder().id("DEMO").type(TenantType.DEMO).build();
        when(tenantRepository.findById("DEMO")).thenReturn(Optional.of(tenant));
        AppUser staff = personaUser("demo.staff", "stale-hash", true);
        when(appUserRepository.findByTenantIdAndLoginName("DEMO", "demo.staff"))
                .thenReturn(Optional.of(staff));
        when(passwordEncoder.matches("Demo123!", "stale-hash")).thenReturn(false);
        when(demoTenantSeedService.resetExistingDemoTenant("DEMO")).thenReturn(seedResult("DEMO"));

        DemoTenantBootstrapService.DemoBootstrapResult result = service.ensureConfiguredDemoTenantReady();

        assertThat(result.action()).isEqualTo(DemoTenantBootstrapService.DemoBootstrapAction.REPAIRED);
        verify(demoTenantSeedService).resetExistingDemoTenant("DEMO");
    }

    @Test
    void repairsDemoTenantWhenRequiredPersonaIsInactive() {
        Tenant tenant = Tenant.builder().id("DEMO").type(TenantType.DEMO).build();
        when(tenantRepository.findById("DEMO")).thenReturn(Optional.of(tenant));
        AppUser staff = personaUser("demo.staff", "staff-hash", false);
        when(appUserRepository.findByTenantIdAndLoginName("DEMO", "demo.staff"))
                .thenReturn(Optional.of(staff));
        when(demoTenantSeedService.resetExistingDemoTenant("DEMO")).thenReturn(seedResult("DEMO"));

        DemoTenantBootstrapService.DemoBootstrapResult result = service.ensureConfiguredDemoTenantReady();

        assertThat(result.action()).isEqualTo(DemoTenantBootstrapService.DemoBootstrapAction.REPAIRED);
        verify(demoTenantSeedService).resetExistingDemoTenant("DEMO");
        verify(passwordEncoder, never()).matches("Demo123!", "staff-hash");
    }

    @Test
    void refusesToBootstrapOverStandardTenant() {
        Tenant tenant = Tenant.builder().id("DEMO").type(TenantType.STANDARD).build();
        when(tenantRepository.findById("DEMO")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.ensureConfiguredDemoTenantReady())
                .isInstanceOf(DemoTenantOperationException.class)
                .hasMessageContaining("bootstrap non-DEMO tenant DEMO");

        verify(demoTenantSeedService, never()).resetConfiguredDemoTenant();
        verify(demoTenantSeedService, never()).resetExistingDemoTenant("DEMO");
    }

    private void stubUsablePersona(String loginName, String passwordHash) {
        AppUser user = personaUser(loginName, passwordHash, true);
        when(appUserRepository.findByTenantIdAndLoginName("DEMO", loginName)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Demo123!", passwordHash)).thenReturn(true);
    }

    private AppUser personaUser(String loginName, String passwordHash, boolean active) {
        return AppUser.builder()
                .tenantId("DEMO")
                .loginName(loginName)
                .password(passwordHash)
                .active(active)
                .build();
    }

    private DemoTenantSeedService.DemoSeedResult seedResult(String tenantId) {
        return new DemoTenantSeedService.DemoSeedResult(
                tenantId,
                TenantType.DEMO,
                4,
                4,
                4,
                LocalDate.now());
    }
}

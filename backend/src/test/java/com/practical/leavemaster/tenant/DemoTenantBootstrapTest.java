package com.practical.leavemaster.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoTenantBootstrapTest {

    private DemoTenantBootstrapService bootstrapService;
    private DemoTenantBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        bootstrapService = mock(DemoTenantBootstrapService.class);
        bootstrap = new DemoTenantBootstrap(bootstrapService);
    }

    @Test
    void doesNothingWhenBootstrapIsDisabled() {
        ReflectionTestUtils.setField(bootstrap, "bootstrapEnabled", false);

        bootstrap.bootstrapDemoTenant();

        verify(bootstrapService, never()).ensureConfiguredDemoTenantReady();
    }

    @Test
    void ensuresDemoTenantWhenBootstrapIsEnabled() {
        ReflectionTestUtils.setField(bootstrap, "bootstrapEnabled", true);
        when(bootstrapService.ensureConfiguredDemoTenantReady()).thenReturn(
                new DemoTenantBootstrapService.DemoBootstrapResult(
                        "DEMO",
                        DemoTenantBootstrapService.DemoBootstrapAction.ALREADY_READY));

        bootstrap.bootstrapDemoTenant();

        verify(bootstrapService).ensureConfiguredDemoTenantReady();
    }
}

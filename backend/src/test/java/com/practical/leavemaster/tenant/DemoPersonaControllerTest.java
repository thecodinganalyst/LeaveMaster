package com.practical.leavemaster.tenant;

import com.practical.leavemaster.user.TenantAuthenticationProvider;
import com.practical.leavemaster.user.TenantAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoPersonaControllerTest {

    private TenantAuthenticationProvider authenticationProvider;
    private DemoTenantPolicy demoTenantPolicy;
    private DemoPersonaController controller;

    @BeforeEach
    void setUp() {
        authenticationProvider = mock(TenantAuthenticationProvider.class);
        demoTenantPolicy = mock(DemoTenantPolicy.class);
        controller = new DemoPersonaController(authenticationProvider, demoTenantPolicy);
        ReflectionTestUtils.setField(controller, "demoTenantId", "DEMO");
        ReflectionTestUtils.setField(controller, "demoPassword", "Demo123!");
    }

    @Test
    void employeePersonaCreatesAuthenticatedDemoSession() {
        Authentication authentication = mock(Authentication.class);
        when(demoTenantPolicy.isDemoTenant("DEMO")).thenReturn(true);
        when(authenticationProvider.authenticate(org.mockito.ArgumentMatchers.any(TenantAuthenticationToken.class)))
                .thenReturn(authentication);
        MockHttpServletRequest request = new MockHttpServletRequest();

        var response = controller.login(new DemoPersonaController.DemoLoginRequest("employee"), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().persona()).isEqualTo("employee");
        assertThat(response.getBody().tenantId()).isEqualTo("DEMO");
        assertThat(response.getBody().loginName()).isEqualTo("demo.staff");

        ArgumentCaptor<TenantAuthenticationToken> tokenCaptor = ArgumentCaptor.forClass(TenantAuthenticationToken.class);
        verify(authenticationProvider).authenticate(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTenantId()).isEqualTo("DEMO");
        assertThat(tokenCaptor.getValue().getLoginName()).isEqualTo("demo.staff");

        Object storedContext = request.getSession(false).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(storedContext).isNotNull();
    }

    @Test
    void rejectsPublicDemoLoginWhenConfiguredTenantIsNotDemo() {
        when(demoTenantPolicy.isDemoTenant("DEMO")).thenReturn(false);

        assertThatThrownBy(() -> controller.login(
                new DemoPersonaController.DemoLoginRequest("manager"), new MockHttpServletRequest()))
                .isInstanceOf(DemoTenantOperationException.class);
    }

    @Test
    void rejectsUnsupportedPersona() {
        when(demoTenantPolicy.isDemoTenant("DEMO")).thenReturn(true);

        assertThatThrownBy(() -> controller.login(
                new DemoPersonaController.DemoLoginRequest("platform-admin"), new MockHttpServletRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported demo persona");
    }
}

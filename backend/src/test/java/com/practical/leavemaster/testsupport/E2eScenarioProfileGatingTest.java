package com.practical.leavemaster.testsupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class E2eScenarioProfileGatingTest {

    @Autowired private ApplicationContext applicationContext;

    @Test
    void e2eControllerAndBootstrapServiceAreNotRegisteredWithoutE2eProfile() {
        assertThat(applicationContext.getBeansOfType(E2eScenarioController.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(E2eScenarioBootstrapService.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(E2eScenarioSecurityConfig.class)).isEmpty();
    }
}

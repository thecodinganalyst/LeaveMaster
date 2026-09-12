package com.practical.leavemaster.testsupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class E2eScenarioControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private E2eScenarioBootstrapService bootstrapService;

    @AfterEach
    void cleanup() {
        bootstrapService.deleteScenario("controller-test");
    }

    @Test
    void createsAndDeletesScenarioWithoutProductionAuthentication() throws Exception {
        mockMvc.perform(post("/test/scenarios/standard-sg-company")
                        .param("scenarioId", "controller-test")
                        .param("referenceDate", "2026-09-12"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scenarioId").value("controller-test"))
                .andExpect(jsonPath("$.tenantId").value("E2E-controller-test"))
                .andExpect(jsonPath("$.users.staff001.loginName").value("staff001"))
                .andExpect(jsonPath("$.password").value("e2e-password"));

        mockMvc.perform(delete("/test/scenarios/controller-test"))
                .andExpect(status().isNoContent());
    }
}

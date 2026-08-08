package com.practical.leavemaster.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigOidcLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"google", "microsoft", "github", "facebook"})
    void shouldExposeOauth2AuthorizationEndpointForConfiguredProvider(String provider) throws Exception {
        mockMvc.perform(get("/oauth2/authorization/" + provider))
                .andExpect(status().is3xxRedirection());
    }
}

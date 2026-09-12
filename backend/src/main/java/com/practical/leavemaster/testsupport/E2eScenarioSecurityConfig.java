package com.practical.leavemaster.testsupport;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("e2e")
public class E2eScenarioSecurityConfig {

    /**
     * This chain exists only when the explicit e2e profile is active and only matches the
     * scenario bootstrap path. The normal application security chain continues to protect all
     * other endpoints.
     */
    @Bean
    @Order(0)
    SecurityFilterChain e2eScenarioSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/test/scenarios/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}

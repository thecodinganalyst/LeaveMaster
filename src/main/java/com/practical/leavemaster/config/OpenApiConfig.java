package com.practical.leavemaster.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI leaveApiDoc() {
        return new OpenAPI()
                .info(new Info()
                        .title("LeaveMaster API")
                        .description("REST API for managing employee leave applications, approvals, and entitlements")
                        .version("1.0.0"));
    }
}

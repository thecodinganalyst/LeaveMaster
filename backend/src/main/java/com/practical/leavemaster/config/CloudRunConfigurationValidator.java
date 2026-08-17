package com.practical.leavemaster.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@Profile("cloudrun")
public class CloudRunConfigurationValidator implements ApplicationRunner {

    private final String publicAppUrl;
    private final String allowedOrigins;

    public CloudRunConfigurationValidator(
        @Value("${app.public-url}") String publicAppUrl,
        @Value("${app.cors.allowed-origins:}") String allowedOrigins
    ) {
        this.publicAppUrl = publicAppUrl;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void run(ApplicationArguments args) {
        validateHttpsOrigin("APP_PUBLIC_URL", publicAppUrl);

        for (String origin : SecurityConfig.parseAllowedOrigins(allowedOrigins)) {
            if (origin.contains("*")) {
                throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS must not contain wildcard origins");
            }
            validateHttpsOrigin("APP_CORS_ALLOWED_ORIGINS", origin);
        }
    }

    private static void validateHttpsOrigin(String settingName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(settingName + " must be configured for the cloudrun profile");
        }

        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(settingName + " must be a valid HTTPS origin", exception);
        }

        boolean hasOnlyOrigin = uri.getHost() != null
            && "https".equalsIgnoreCase(uri.getScheme())
            && (uri.getPath() == null || uri.getPath().isEmpty() || "/".equals(uri.getPath()))
            && uri.getQuery() == null
            && uri.getFragment() == null
            && uri.getUserInfo() == null;

        if (!hasOnlyOrigin) {
            throw new IllegalStateException(settingName + " must be an HTTPS origin without path, query, or fragment");
        }
    }
}

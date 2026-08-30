package com.practical.leavemaster.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("cloudrun")
public class OAuthProductionConfigurationValidator implements ApplicationRunner {

    private static final String PLACEHOLDER = "replace-me";

    private final String githubClientId;
    private final String githubClientSecret;
    private final String googleClientId;
    private final String googleClientSecret;

    public OAuthProductionConfigurationValidator(
            @Value("${spring.security.oauth2.client.registration.github.client-id:}") String githubClientId,
            @Value("${spring.security.oauth2.client.registration.github.client-secret:}") String githubClientSecret,
            @Value("${spring.security.oauth2.client.registration.google.client-id:}") String googleClientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret:}") String googleClientSecret) {
        this.githubClientId = githubClientId;
        this.githubClientSecret = githubClientSecret;
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
    }

    @Override
    public void run(ApplicationArguments args) {
        validateProvider("GitHub", "GH_CLIENT_ID", githubClientId, "GH_CLIENT_SECRET", githubClientSecret);
        validateProvider("Google", "GOOGLE_CLIENT_ID", googleClientId, "GOOGLE_CLIENT_SECRET", googleClientSecret);
    }

    private static void validateProvider(
            String provider,
            String clientIdName,
            String clientId,
            String clientSecretName,
            String clientSecret) {
        if (isMissing(clientId) || isMissing(clientSecret)) {
            throw new IllegalStateException(
                    provider + " OAuth is not configured for Cloud Run. Configure "
                            + clientIdName + " and " + clientSecretName
                            + " with non-placeholder production credentials.");
        }
    }

    private static boolean isMissing(String value) {
        return value == null || value.isBlank() || PLACEHOLDER.equalsIgnoreCase(value.trim());
    }
}

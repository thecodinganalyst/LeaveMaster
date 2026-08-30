package com.practical.leavemaster.email;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
@Slf4j
public class TransactionalEmailProviderReporter {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("disabled", "resend");

    private final String provider;

    public TransactionalEmailProviderReporter(@Value("${app.email.provider:disabled}") String provider) {
        this.provider = provider == null ? "disabled" : provider.trim().toLowerCase(Locale.ROOT);
    }

    @PostConstruct
    void reportProvider() {
        if (!SUPPORTED_PROVIDERS.contains(provider)) {
            throw new IllegalStateException("Unsupported transactional email provider: " + provider);
        }
        log.info("Transactional email provider: {}", provider);
    }
}

package com.practical.leavemaster.jurisdiction;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class JurisdictionHierarchyReconciliationInitializer implements ApplicationRunner {
    private final JurisdictionRepository jurisdictionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Jurisdiction jurisdiction : jurisdictionRepository.findAll()) {
            if (jurisdiction == null || jurisdiction.getJurisdictionType() == JurisdictionType.COUNTRY) {
                continue;
            }

            String countryCode = resolveCountryCode(jurisdiction);
            if (countryCode == null || !jurisdictionRepository.existsById(countryCode)) {
                continue;
            }

            boolean changed = false;
            if (jurisdiction.getCountryCode() == null || jurisdiction.getCountryCode().isBlank()) {
                jurisdiction.setCountryCode(countryCode);
                changed = true;
            }
            if ((jurisdiction.getSubdivisionCode() == null || jurisdiction.getSubdivisionCode().isBlank())
                    && jurisdiction.getId() != null && jurisdiction.getId().contains("-")) {
                jurisdiction.setSubdivisionCode(jurisdiction.getId());
                changed = true;
            }
            if (jurisdiction.getParentId() == null || jurisdiction.getParentId().isBlank()) {
                jurisdiction.setParentId(countryCode);
                changed = true;
            }

            if (changed) {
                jurisdictionRepository.save(jurisdiction);
            }
        }
    }

    private String resolveCountryCode(Jurisdiction jurisdiction) {
        String existingCountryCode = jurisdiction.getCountryCode();
        if (existingCountryCode != null && !existingCountryCode.isBlank()) {
            return existingCountryCode.trim().toUpperCase(Locale.ROOT);
        }

        String id = jurisdiction.getId();
        if (id == null || id.isBlank()) {
            return null;
        }
        int separator = id.indexOf('-');
        if (separator <= 0) {
            return null;
        }
        return id.substring(0, separator).trim().toUpperCase(Locale.ROOT);
    }
}

package com.practical.leavemaster.jurisdiction;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JurisdictionHierarchyReconciliationInitializer implements ApplicationRunner {
    private final JurisdictionRepository jurisdictionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Jurisdiction jurisdiction : jurisdictionRepository.findAll()) {
            if (!needsCountryParent(jurisdiction)) {
                continue;
            }
            if (!jurisdictionRepository.existsById(jurisdiction.getCountryCode())) {
                continue;
            }
            jurisdiction.setParentId(jurisdiction.getCountryCode());
            jurisdictionRepository.save(jurisdiction);
        }
    }

    private boolean needsCountryParent(Jurisdiction jurisdiction) {
        if (jurisdiction == null || jurisdiction.getJurisdictionType() == JurisdictionType.COUNTRY) {
            return false;
        }
        if (jurisdiction.getParentId() != null && !jurisdiction.getParentId().isBlank()) {
            return false;
        }
        String countryCode = jurisdiction.getCountryCode();
        return countryCode != null
                && !countryCode.isBlank()
                && !countryCode.equals(jurisdiction.getId());
    }
}

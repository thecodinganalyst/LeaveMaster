package com.practical.leavemaster.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TenantActivityService {

    private final TenantRepository tenantRepository;

    @Transactional
    public void touch(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return;
        }
        tenantRepository.updateLastModified(tenantId, LocalDateTime.now());
    }
}

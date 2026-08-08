package com.practical.leavemaster.tenant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TenantActivityServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantActivityService tenantActivityService;

    @Test
    void shouldTouchTenantWhenTenantIdIsPresent() {
        tenantActivityService.touch("tenant-1");

        verify(tenantRepository).updateLastModified(eq("tenant-1"), any(LocalDateTime.class));
    }

    @Test
    void shouldIgnoreBlankTenantId() {
        tenantActivityService.touch(" ");

        verify(tenantRepository, never()).updateLastModified(any(), any(LocalDateTime.class));
    }
}

package com.practical.leavemaster.assistant;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/platform/assistant-quality")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
class AssistantQualityDashboardController {
    private final AssistantQualityDashboardService service;

    @GetMapping
    AssistantQualityDashboardService.Dashboard dashboard(
            @RequestParam(defaultValue="7") int days,
            @RequestParam(required=false) String outcome,
            @RequestParam(required=false) String failureCategory,
            @RequestParam(required=false) String provider,
            @RequestParam(required=false) String model) {
        int safeDays=Math.max(1,Math.min(days,90));
        return service.dashboard(Instant.now().minus(safeDays, ChronoUnit.DAYS), outcome, failureCategory, provider, model);
    }
}

package com.practical.leavemaster.leaveentitlement;

import com.practical.leavemaster.leaveeligibility.LeaveEligibilityFactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/staff/{staffId}/event-entitlements", "/api/staff/{staffId}/event-entitlements"})
@RequiredArgsConstructor
public class EventLeaveEntitlementController {

    private final EventLeaveEntitlementService service;
    private final LeaveEligibilityFactService factService;

    @GetMapping
    public ResponseEntity<?> findAll(
            @PathVariable String staffId,
            @RequestParam String leaveTypeId) {
        try {
            factService.findEvents(staffId);
            return ResponseEntity.ok(service.findForStaff(staffId, leaveTypeId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(
            @PathVariable String staffId,
            @RequestBody EventEntitlementGenerationRequest request) {
        try {
            if (request == null || request.leaveTypeId() == null || request.qualifyingEventId() == null) {
                throw new IllegalArgumentException("leaveTypeId and qualifyingEventId are required");
            }
            return ResponseEntity.ok(service.generate(staffId, request.leaveTypeId(), request.qualifyingEventId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

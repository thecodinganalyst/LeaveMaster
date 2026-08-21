package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/staff/entitlement-proposals", "/api/staff/entitlement-proposals"})
@RequiredArgsConstructor
public class StaffEntitlementProposalController {
    private final StaffEntitlementProposalService proposalService;

    @PostMapping
    public ResponseEntity<?> propose(@RequestBody StaffEntitlementProposalRequest request) {
        try {
            List<LeaveEntitlement> proposals = proposalService.propose(request);
            return ResponseEntity.ok(proposals);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/analysis")
    public ResponseEntity<?> analyze(@RequestBody StaffEntitlementProposalRequest request) {
        try {
            return ResponseEntity.ok(proposalService.analyze(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

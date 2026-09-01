package com.practical.leavemaster.leavetype;

import com.practical.leavemaster.leaveentitlementpolicy.EligibilityCriterionType;
import com.practical.leavemaster.leaveentitlementpolicy.EligibilityOperator;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRule;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityService;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/leave-types", "/api/leave-types"})
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;
    private final LeaveEntitlementPolicyService entitlementPolicyService;
    private final LeaveEntitlementPolicyEligibilityService eligibilityService;

    @GetMapping
    public List<LeaveType> getAll() {
        return leaveTypeService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveType> getById(@PathVariable String id) {
        return leaveTypeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/entitlements")
    public ResponseEntity<List<LeaveTypeEntitlementView>> getEntitlements(@PathVariable String id) {
        if (leaveTypeService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<LeaveTypeEntitlementView> entitlements = entitlementPolicyService.findAll().stream()
                .filter(policy -> id.equals(policy.getLeaveTypeId()))
                .map(policy -> new LeaveTypeEntitlementView(
                        policy.getId(),
                        policy.getEntitlementAmount(),
                        policy.getEntitlementUnit(),
                        policy.getEffectiveFrom(),
                        policy.getEffectiveTo(),
                        policy.isActive(),
                        eligibilityService.findAll(policy.getId()).stream()
                                .map(LeaveTypeEligibilityRuleView::from)
                                .toList()))
                .toList();
        return ResponseEntity.ok(entitlements);
    }

    @PostMapping
    public ResponseEntity<LeaveType> create(@RequestBody LeaveType leaveType) {
        LeaveType saved = leaveTypeService.save(leaveType);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeaveType> update(@PathVariable String id, @RequestBody LeaveType leaveType) {
        try {
            LeaveType updated = leaveTypeService.update(id, leaveType);
            return ResponseEntity.ok(updated);
        } catch (LeaveTypeNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            leaveTypeService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (LeaveTypeNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (LeaveTypeInUseException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    public record LeaveTypeEntitlementView(
            String id,
            BigDecimal entitlementAmount,
            EntitlementUnit entitlementUnit,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean active,
            List<LeaveTypeEligibilityRuleView> eligibilityRules) {
    }

    public record LeaveTypeEligibilityRuleView(
            EligibilityCriterionType criterionType,
            EligibilityOperator operator,
            String value,
            boolean active,
            int sortOrder) {
        static LeaveTypeEligibilityRuleView from(LeaveEntitlementPolicyEligibilityRule rule) {
            return new LeaveTypeEligibilityRuleView(
                    rule.getCriterionType(),
                    rule.getOperator(),
                    rule.getValue(),
                    rule.isActive(),
                    rule.getSortOrder());
        }
    }
}

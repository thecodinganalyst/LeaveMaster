package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveapprover.LeaveApprover;
import com.practical.leavemaster.leaveapprover.LeaveApproverRequest;
import com.practical.leavemaster.leaveapprover.LeaveApproverService;
import com.practical.leavemaster.leaveeligibility.LeaveEligibilityFactService;
import com.practical.leavemaster.leaveeligibility.StaffDependantWriteRequest;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StaffWriteService {

    private static final BigDecimal HALF_DAY = new BigDecimal("0.5");

    private final StaffService staffService;
    private final LeaveApproverService leaveApproverService;
    private final LeaveEligibilityFactService leaveEligibilityFactService;
    private final StaffEntitlementProposalService entitlementProposalService;

    @Transactional
    public Staff create(StaffWriteRequest request) {
        Staff staff = request.toStaff();
        StaffEntitlementProposalAnalysis authoritative = entitlementProposalService.analyze(
                new StaffEntitlementProposalRequest(
                        null,
                        request.jurisdictionId(),
                        request.joinDate(),
                        request.termDate(),
                        request.employmentType(),
                        request.dependants()));
        staff.setLeaveEntitlements(applyReviewedEntitlements(authoritative.proposals(), request.leaveEntitlements()));

        Staff saved = staffService.save(staff);
        syncLeaveApprovers(saved.getId(), request.leaveApprovers());
        createDependants(saved.getId(), request.dependants());
        return saved;
    }

    @Transactional
    public Staff update(String id, StaffWriteRequest request) {
        Staff saved = staffService.update(id, request.toStaff());
        syncLeaveApprovers(saved.getId(), request.leaveApprovers());
        return saved;
    }

    private List<LeaveEntitlement> applyReviewedEntitlements(
            List<LeaveEntitlement> authoritative,
            List<StaffWriteRequest.EntitlementInput> reviewed) {
        if (reviewed == null) return authoritative;
        if (reviewed.size() != authoritative.size()) {
            throw new IllegalArgumentException("Leave entitlement review is stale; regenerate entitlements before creating staff");
        }

        Map<String, StaffWriteRequest.EntitlementInput> reviewedByIdentity = new LinkedHashMap<>();
        for (StaffWriteRequest.EntitlementInput input : reviewed) {
            if (input == null) {
                throw new IllegalArgumentException("Leave entitlement review contains an invalid entry");
            }
            String key = entitlementIdentity(input.leaveTypeId(), input.policyId(), input.from(), input.to());
            if (reviewedByIdentity.putIfAbsent(key, input) != null) {
                throw new IllegalArgumentException("Leave entitlement review contains duplicate entries");
            }
        }

        for (LeaveEntitlement proposal : authoritative) {
            String leaveTypeId = proposal.getLeaveType() == null ? null : proposal.getLeaveType().getId();
            String key = entitlementIdentity(leaveTypeId, proposal.getPolicyId(), proposal.getFrom(), proposal.getTo());
            StaffWriteRequest.EntitlementInput input = reviewedByIdentity.remove(key);
            if (input == null) {
                throw new IllegalArgumentException("Leave entitlement review is stale or does not match the generated policy result");
            }

            BigDecimal base = proposal.getBaseEntitlementAmount() == null
                    ? proposal.getEntitlement()
                    : proposal.getBaseEntitlementAmount();
            BigDecimal finalAmount = input.entitlement() == null ? base : input.entitlement();
            validateReviewedAmount(finalAmount);

            proposal.setBaseEntitlementAmount(base.setScale(2, RoundingMode.HALF_UP));
            proposal.setEntitlement(finalAmount.setScale(2, RoundingMode.HALF_UP));
            proposal.setAdjustmentAmount(finalAmount.subtract(base).setScale(2, RoundingMode.HALF_UP));
        }

        if (!reviewedByIdentity.isEmpty()) {
            throw new IllegalArgumentException("Leave entitlement review contains entries that are no longer generated");
        }
        return authoritative;
    }

    private void validateReviewedAmount(BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Final leave entitlement must not be negative");
        }
        if (amount.remainder(HALF_DAY).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Final leave entitlement must use 0.5-day increments");
        }
    }

    private String entitlementIdentity(Object leaveTypeId, Object policyId, Object from, Object to) {
        return String.valueOf(leaveTypeId) + "|" + String.valueOf(policyId) + "|" + String.valueOf(from) + "|" + String.valueOf(to);
    }

    private void createDependants(String staffId, List<StaffDependantWriteRequest> inputs) {
        if (inputs == null) return;
        for (StaffDependantWriteRequest input : inputs) {
            if (input != null) leaveEligibilityFactService.createDependant(staffId, input);
        }
    }

    private void syncLeaveApprovers(String staffId, List<StaffWriteRequest.LeaveApproverInput> inputs) {
        if (inputs == null) return;

        List<LeaveApprover> existing = leaveApproverService.findByStaffId(staffId);
        Set<String> existingIds = existing.stream().map(LeaveApprover::getId).collect(java.util.stream.Collectors.toSet());
        Set<String> retainedIds = new HashSet<>();

        for (StaffWriteRequest.LeaveApproverInput input : inputs) {
            if (input == null) continue;
            LeaveApproverRequest leaveApproverRequest = LeaveApproverRequest.builder()
                    .staffId(staffId)
                    .approverId(input.approverId())
                    .effectiveFrom(input.effectiveFrom())
                    .effectiveTo(input.effectiveTo())
                    .build();

            if (input.id() == null || input.id().isBlank()) {
                LeaveApprover created = leaveApproverService.create(leaveApproverRequest);
                retainedIds.add(created.getId());
                continue;
            }

            if (!existingIds.contains(input.id())) {
                throw new IllegalArgumentException("Leave approver record does not belong to the staff member being edited");
            }
            leaveApproverService.update(input.id(), leaveApproverRequest);
            retainedIds.add(input.id());
        }

        for (LeaveApprover record : existing) {
            if (!retainedIds.contains(record.getId())) leaveApproverService.delete(record.getId());
        }
    }
}

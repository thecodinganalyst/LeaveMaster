package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveapprover.LeaveApprover;
import com.practical.leavemaster.leaveapprover.LeaveApproverRequest;
import com.practical.leavemaster.leaveapprover.LeaveApproverService;
import com.practical.leavemaster.leaveeligibility.LeaveEligibilityFactService;
import com.practical.leavemaster.leaveeligibility.StaffDependantWriteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StaffWriteService {

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
        staff.setLeaveEntitlements(authoritative.proposals());

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

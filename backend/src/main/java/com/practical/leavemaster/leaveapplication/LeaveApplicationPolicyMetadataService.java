package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyResolutionService;
import com.practical.leavemaster.leaveentitlementpolicy.LeavePolicyModel;
import com.practical.leavemaster.leaveentitlementpolicy.PolicyResolutionResult;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeNotFoundException;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffNotFoundException;
import com.practical.leavemaster.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LeaveApplicationPolicyMetadataService {

    private final StaffRepository staffRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveEntitlementPolicyRepository policyRepository;
    private final LeaveEntitlementPolicyResolutionService resolutionService;

    public LeaveApplicationPolicyMetadata resolve(String staffId, String leaveTypeId, LocalDate effectiveDate) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException(staffId));
        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new LeaveTypeNotFoundException(leaveTypeId));
        if (leaveType.getTenantId() != null && !Objects.equals(staff.getTenantId(), leaveType.getTenantId())) {
            throw new IllegalArgumentException("Leave type does not belong to the staff tenant");
        }

        PolicyResolutionResult resolution = resolutionService.resolve(staff, leaveTypeId, effectiveDate);
        if (resolution.ambiguous()) {
            throw new IllegalArgumentException("Multiple leave policies match with the same priority");
        }
        if (resolution.selectedPolicyId() == null) {
            return new LeaveApplicationPolicyMetadata(null, false, false);
        }

        LeaveEntitlementPolicy policy = policyRepository.findById(resolution.selectedPolicyId())
                .orElseThrow(() -> new IllegalStateException("Resolved policy no longer exists"));
        boolean eventBased = policy.getPolicyModel() == LeavePolicyModel.EVENT_BASED;
        return new LeaveApplicationPolicyMetadata(
                policy.getPolicyModel(),
                eventBased,
                eventBased && policy.isEventRequiresVerification());
    }

    public void validateAttachmentRequirement(LeaveApplicationRequest request, boolean hasAttachment) {
        if (request == null || request.getStaffId() == null || request.getLeaveTypeId() == null) {
            return;
        }
        LocalDate effectiveDate = request.getEventDate() != null ? request.getEventDate() : request.getFromDate();
        LeaveApplicationPolicyMetadata metadata = resolve(request.getStaffId(), request.getLeaveTypeId(), effectiveDate);
        if (metadata.eventBased() && metadata.eventRequiresVerification() && !hasAttachment) {
            throw new IllegalArgumentException("Attachment is required for this event-based leave because the qualifying event requires verification");
        }
    }
}

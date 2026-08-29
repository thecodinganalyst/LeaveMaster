package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveeligibility.StaffDependantWriteRequest;

import java.time.LocalDate;
import java.util.List;

public record StaffEntitlementProposalRequest(
        String staffId,
        String jurisdictionId,
        LocalDate joinDate,
        LocalDate termDate,
        EmploymentType employmentType,
        List<StaffDependantWriteRequest> dependants
) {
    public StaffEntitlementProposalRequest(
            String staffId,
            String jurisdictionId,
            LocalDate joinDate,
            LocalDate termDate) {
        this(staffId, jurisdictionId, joinDate, termDate, null, null);
    }
}

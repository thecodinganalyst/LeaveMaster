package com.practical.leavemaster.staff;

import java.time.LocalDate;

public record StaffEntitlementProposalRequest(
        String staffId,
        String jurisdictionId,
        LocalDate joinDate,
        LocalDate termDate
) {
}

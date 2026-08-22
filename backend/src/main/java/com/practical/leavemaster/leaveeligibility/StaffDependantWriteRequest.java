package com.practical.leavemaster.leaveeligibility;

import java.time.LocalDate;

public record StaffDependantWriteRequest(
        String name,
        String relationshipCode,
        LocalDate dateOfBirth,
        String citizenshipCode,
        String residencyCode,
        LocalDate adoptionDate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Boolean active
) {
}

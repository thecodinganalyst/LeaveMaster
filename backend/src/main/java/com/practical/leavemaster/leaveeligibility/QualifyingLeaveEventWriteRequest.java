package com.practical.leavemaster.leaveeligibility;

import java.time.LocalDate;

public record QualifyingLeaveEventWriteRequest(
        String dependantId,
        String eventTypeCode,
        LocalDate eventDate,
        LocalDate startDate,
        LocalDate endDate,
        String externalReference,
        String supportingDocumentReference,
        QualifyingEventStatus status
) {
}

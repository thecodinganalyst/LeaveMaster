package com.practical.leavemaster.leaveeligibility;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QualifyingLeaveEventWriteRequest(
        String dependantId,
        String eventTypeCode,
        LocalDate eventDate,
        LocalDate startDate,
        LocalDate endDate,
        String externalReference,
        String supportingDocumentReference,
        QualifyingEventStatus status,
        BigDecimal approvedEntitlementAmount
) {
    public QualifyingLeaveEventWriteRequest(
            String dependantId,
            String eventTypeCode,
            LocalDate eventDate,
            LocalDate startDate,
            LocalDate endDate,
            String externalReference,
            String supportingDocumentReference,
            QualifyingEventStatus status) {
        this(dependantId, eventTypeCode, eventDate, startDate, endDate,
                externalReference, supportingDocumentReference, status, null);
    }
}

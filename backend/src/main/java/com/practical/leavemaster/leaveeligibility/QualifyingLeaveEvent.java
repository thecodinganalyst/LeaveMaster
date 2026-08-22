package com.practical.leavemaster.leaveeligibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "qualifying_leave_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualifyingLeaveEvent {

    @Id
    @Column(nullable = false, unique = true, length = 36)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "staff_id", nullable = false)
    private String staffId;

    @Column(name = "dependant_id", length = 36)
    private String dependantId;

    @Column(name = "event_type_code", nullable = false, length = 100)
    private String eventTypeCode;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "supporting_document_reference")
    private String supportingDocumentReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QualifyingEventStatus status;
}

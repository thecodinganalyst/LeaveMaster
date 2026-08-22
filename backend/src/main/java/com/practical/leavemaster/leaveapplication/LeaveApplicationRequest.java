package com.practical.leavemaster.leaveapplication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApplicationRequest {

    private String staffId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String leaveTypeId;
    private LeaveDuration leaveDuration;
    private LeaveStatus status;

    private String qualifyingEventId;
    private String eventTypeCode;
    private LocalDate eventDate;
    private LocalDate eventStartDate;
    private LocalDate eventEndDate;
    private String dependantId;
    private String eventExternalReference;
    private String eventSupportingDocumentReference;
}

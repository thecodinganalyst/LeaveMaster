package com.practical.leavemaster.leaveentitlement;

import com.practical.leavemaster.leaveeligibility.LeaveEligibilityFactService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventLeaveEntitlementControllerTest {

    @Mock EventLeaveEntitlementService service;
    @Mock LeaveEligibilityFactService factService;
    @InjectMocks EventLeaveEntitlementController controller;

    @Test
    void listsOnlyAfterStaffAccessValidation() {
        EventLeaveEntitlement entitlement = EventLeaveEntitlement.builder()
                .id("e1").tenantId("t1").staffId("s1").leaveTypeId("lt1")
                .policyId("p1").qualifyingEventId("q1")
                .validFrom(LocalDate.of(2026, 1, 1)).validTo(LocalDate.of(2026, 1, 2))
                .grantedAmount(BigDecimal.ONE).usedAmount(BigDecimal.ZERO)
                .status(EventLeaveEntitlementStatus.ACTIVE).build();
        when(service.findForStaff("s1", "lt1")).thenReturn(List.of(entitlement));

        ResponseEntity<?> response = controller.findAll("s1", "lt1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(factService).findEvents("s1");
    }

    @Test
    void returnsBadRequestWhenStaffIsNotAccessible() {
        doThrow(new IllegalArgumentException("Staff does not belong to the current tenant"))
                .when(factService).findEvents("other");

        ResponseEntity<?> response = controller.findAll("other", "lt1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void validatesGenerateRequestAndDelegatesValidRequest() {
        ResponseEntity<?> invalid = controller.generate("s1", new EventEntitlementGenerationRequest(null, null));
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        EventLeaveEntitlement entitlement = EventLeaveEntitlement.builder().id("e1").build();
        when(service.generate("s1", "lt1", "q1")).thenReturn(entitlement);
        ResponseEntity<?> valid = controller.generate("s1", new EventEntitlementGenerationRequest("lt1", "q1"));
        assertThat(valid.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(valid.getBody()).isSameAs(entitlement);
    }
}

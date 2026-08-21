package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StaffEntitlementProposalControllerTest {

    private final StaffEntitlementProposalService proposalService = mock(StaffEntitlementProposalService.class);
    private final StaffEntitlementProposalController controller = new StaffEntitlementProposalController(proposalService);
    private final StaffEntitlementProposalRequest request = new StaffEntitlementProposalRequest(
            null, "SG", LocalDate.of(2026, 1, 1), null);

    @Test
    void shouldReturnProposals() {
        LeaveEntitlement entitlement = LeaveEntitlement.builder().build();
        when(proposalService.propose(request)).thenReturn(List.of(entitlement));

        ResponseEntity<?> response = controller.propose(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(List.of(entitlement));
    }

    @Test
    void shouldReturnProposalAnalysis() {
        StaffEntitlementProposalAnalysis analysis = new StaffEntitlementProposalAnalysis(
                List.of(), StaffEntitlementProposalAnalysis.Status.NOT_ELIGIBLE_IN_PERIOD);
        when(proposalService.analyze(request)).thenReturn(analysis);

        ResponseEntity<?> response = controller.analyze(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(analysis);
    }

    @Test
    void shouldReturnBadRequestForValidationFailure() {
        when(proposalService.propose(request)).thenThrow(new IllegalArgumentException("invalid jurisdiction"));

        ResponseEntity<?> response = controller.propose(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(java.util.Map.of("error", "invalid jurisdiction"));
    }

    @Test
    void shouldReturnBadRequestForInvalidResolvedState() {
        when(proposalService.propose(request)).thenThrow(new IllegalStateException("policy missing"));

        ResponseEntity<?> response = controller.propose(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnBadRequestForAnalysisFailure() {
        when(proposalService.analyze(request)).thenThrow(new IllegalStateException("policy missing"));

        ResponseEntity<?> response = controller.analyze(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(java.util.Map.of("error", "policy missing"));
    }
}

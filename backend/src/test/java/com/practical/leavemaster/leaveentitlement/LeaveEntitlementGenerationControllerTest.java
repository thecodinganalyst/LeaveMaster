package com.practical.leavemaster.leaveentitlement;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeaveEntitlementGenerationControllerTest {
    private final LeaveEntitlementGenerationService service = mock(LeaveEntitlementGenerationService.class);
    private final LeaveEntitlementGenerationController controller = new LeaveEntitlementGenerationController(service);

    @Test
    void delegatesStaffGeneration() {
        LocalDate start = LocalDate.of(2027, 1, 1);
        LocalDate end = LocalDate.of(2027, 12, 31);
        when(service.generateForStaff("s1", start, end)).thenReturn(List.of());

        assertThat(controller.generateForStaff(
                new LeaveEntitlementGenerationController.StaffGenerationRequest("s1", start, end))).isEmpty();
        verify(service).generateForStaff("s1", start, end);
    }

    @Test
    void delegatesTenantGeneration() {
        LocalDate start = LocalDate.of(2027, 1, 1);
        LocalDate end = LocalDate.of(2027, 12, 31);
        when(service.generateForTenant("t1", start, end)).thenReturn(List.of());

        assertThat(controller.generateForTenant(
                new LeaveEntitlementGenerationController.TenantGenerationRequest("t1", start, end))).isEmpty();
        verify(service).generateForTenant("t1", start, end);
    }
}

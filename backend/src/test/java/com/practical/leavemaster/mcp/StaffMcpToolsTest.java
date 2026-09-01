package com.practical.leavemaster.mcp;

import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffService;
import com.practical.leavemaster.staff.TerminationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffMcpToolsTest {

    @Mock
    private StaffService staffService;

    @Mock
    private StaffAssistantReadService staffAssistantReadService;

    @InjectMocks
    private StaffMcpTools staffMcpTools;

    @Test
    void shouldGetAllStaff() {
        var staff = new StaffAssistantReadService.StaffResult(
                "s1", "Alice", null, LocalDate.of(2025, 1, 1), null, "SG", "tenant-1", List.of(), List.of());
        when(staffAssistantReadService.findAll()).thenReturn(List.of(staff));

        List<StaffAssistantReadService.StaffResult> result = staffMcpTools.getAllStaff();

        assertThat(result).singleElement().extracting(StaffAssistantReadService.StaffResult::id).isEqualTo("s1");
        verify(staffAssistantReadService).findAll();
    }

    @Test
    void shouldGetStaffById() {
        var staff = new StaffAssistantReadService.StaffResult(
                "s1", "Alice", null, LocalDate.of(2025, 1, 1), null, "SG", "tenant-1", List.of(), List.of());
        when(staffAssistantReadService.findById("s1")).thenReturn(Optional.of(staff));

        Optional<StaffAssistantReadService.StaffResult> result = staffMcpTools.getStaffById("s1");

        assertThat(result).isPresent().get().extracting(StaffAssistantReadService.StaffResult::name).isEqualTo("Alice");
        verify(staffAssistantReadService).findById("s1");
    }

    @Test
    void shouldGetFocusedStaffLeaveEntitlement() {
        var entitlement = new StaffAssistantReadService.StaffLeaveEntitlementResult(
                "Alice", LocalDate.of(2026, 8, 3), "SG", "Annual Leave",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                new BigDecimal("6.00"), new BigDecimal("6.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "annual-policy", "Singapore Annual Leave - less than 2 years service",
                new BigDecimal("14.00"), "DAYS", "NONE", "CALENDAR_DAYS",
                false, null, null, 151L, 365L, new BigDecimal("5.79178082"),
                new BigDecimal("0.50"), "NEAREST_HALF_DAY", true);
        when(staffAssistantReadService.findLeaveEntitlement("001", "Annual Leave", 2026))
                .thenReturn(Optional.of(entitlement));

        Optional<StaffAssistantReadService.StaffLeaveEntitlementResult> result =
                staffMcpTools.getStaffLeaveEntitlement("001", "Annual Leave", 2026);

        assertThat(result).isPresent().get().satisfies(value -> {
            assertThat(value.entitlement()).isEqualByComparingTo("6.00");
            assertThat(value.configuredEntitlementAmount()).isEqualByComparingTo("14.00");
            assertThat(value.prorationMethod()).isEqualTo("CALENDAR_DAYS");
            assertThat(value.prorationDenominationDays()).isEqualByComparingTo("0.50");
            assertThat(value.prorationRoundingRule()).isEqualTo("NEAREST_HALF_DAY");
            assertThat(value.sourcePolicyResolved()).isTrue();
        });
        verify(staffAssistantReadService).findLeaveEntitlement("001", "Annual Leave", 2026);
    }

    @Test
    void shouldCreateStaff() {
        Staff staff = Staff.builder().id("s1").name("Alice").joinDate(LocalDate.now()).build();
        when(staffService.save(staff)).thenReturn(staff);

        Staff result = staffMcpTools.createStaff(staff);

        assertThat(result.getId()).isEqualTo("s1");
        verify(staffService).save(staff);
    }

    @Test
    void shouldUpdateStaff() {
        Staff staff = Staff.builder().id("s1").name("Alice Updated").joinDate(LocalDate.now()).build();
        when(staffService.update("s1", staff)).thenReturn(staff);

        Staff result = staffMcpTools.updateStaff("s1", staff);

        assertThat(result.getName()).isEqualTo("Alice Updated");
        verify(staffService).update("s1", staff);
    }

    @Test
    void shouldDeleteStaff() {
        doNothing().when(staffService).delete("s1");

        staffMcpTools.deleteStaff("s1");

        verify(staffService).delete("s1");
    }

    @Test
    void shouldTerminateStaff() {
        LocalDate termDate = LocalDate.now();
        Staff staff = Staff.builder().id("s1").name("Alice").joinDate(LocalDate.now()).build();
        TerminationResult terminationResult = new TerminationResult(staff, List.of());
        when(staffService.terminate("s1", termDate)).thenReturn(terminationResult);

        TerminationResult result = staffMcpTools.terminateStaff("s1", termDate);

        assertThat(result.getStaff().getId()).isEqualTo("s1");
        verify(staffService).terminate("s1", termDate);
    }
}

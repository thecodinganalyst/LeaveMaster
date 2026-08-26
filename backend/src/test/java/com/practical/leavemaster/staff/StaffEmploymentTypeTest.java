package com.practical.leavemaster.staff;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StaffEmploymentTypeTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldMapEmploymentTypeFromWriteRequest() {
        StaffWriteRequest request = new StaffWriteRequest(
                "staff-1", "Alex", "alex@example.com", LocalDate.of(2026, 1, 5),
                null, null, "SG", null, "alex", Set.of("ROLE"), EmploymentType.FULL_TIME);

        Staff staff = request.toStaff();

        assertThat(staff.getEmploymentType()).isEqualTo(EmploymentType.FULL_TIME);
    }

    @Test
    void shouldPreserveNullEmploymentTypeForLegacyWriteCallers() {
        StaffWriteRequest request = new StaffWriteRequest(
                "staff-1", "Alex", "alex@example.com", LocalDate.of(2026, 1, 5),
                null, null, "SG", null, "alex", Set.of("ROLE"));

        assertThat(request.toStaff().getEmploymentType()).isNull();
    }

    @Test
    void shouldRejectUnsupportedEmploymentTypeDuringJsonDeserialization() {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{\"id\":\"staff-1\",\"name\":\"Alex\",\"joinDate\":\"2026-01-05\",\"employmentType\":\"PERMANENT\"}";

        assertThatThrownBy(() -> objectMapper.readValue(json, StaffWriteRequest.class))
                .hasMessageContaining("EmploymentType");
    }

    @Test
    void shouldUpdateEmploymentTypeAndAllowClearingIt() {
        StaffRepository staffRepository = mock(StaffRepository.class);
        LeaveCalendarService leaveCalendarService = mock(LeaveCalendarService.class);
        LeaveTypeRepository leaveTypeRepository = mock(LeaveTypeRepository.class);
        LeaveApproverRepository leaveApproverRepository = mock(LeaveApproverRepository.class);
        LeaveApplicationRepository leaveApplicationRepository = mock(LeaveApplicationRepository.class);
        AppUserService appUserService = mock(AppUserService.class);
        TenantActivityService tenantActivityService = mock(TenantActivityService.class);
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        StaffService service = new StaffService(staffRepository, leaveCalendarService, leaveTypeRepository,
                leaveApproverRepository, leaveApplicationRepository, appUserService,
                tenantActivityService, appUserRepository);

        Staff existing = Staff.builder()
                .id("staff-1")
                .name("Alex")
                .joinDate(LocalDate.of(2026, 1, 5))
                .jurisdictionId("SG")
                .employmentType(EmploymentType.FULL_TIME)
                .build();
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(existing));
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appUserService.findRoleIdsByStaffId("staff-1")).thenReturn(Set.of());

        Staff updated = Staff.builder()
                .name("Alex")
                .joinDate(LocalDate.of(2026, 1, 5))
                .jurisdictionId("SG")
                .employmentType(EmploymentType.PART_TIME)
                .build();
        assertThat(service.update("staff-1", updated).getEmploymentType()).isEqualTo(EmploymentType.PART_TIME);

        Staff cleared = Staff.builder()
                .name("Alex")
                .joinDate(LocalDate.of(2026, 1, 5))
                .jurisdictionId("SG")
                .employmentType(null)
                .build();
        assertThat(service.update("staff-1", cleared).getEmploymentType()).isNull();
    }

    @Test
    void shouldExposeExpectedStableEmploymentTypeCodes() {
        assertThat(EmploymentType.values()).containsExactly(
                EmploymentType.FULL_TIME,
                EmploymentType.PART_TIME,
                EmploymentType.CASUAL,
                EmploymentType.CONTRACT,
                EmploymentType.INTERN);
    }
}

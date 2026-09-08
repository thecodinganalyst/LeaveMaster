package com.practical.leavemaster.leavetype;

import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaveTypeJurisdictionAttributionTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPreserveApplicableChildJurisdictionForInheritedParentSource() {
        LeaveTypeRepository leaveTypeRepository = mock(LeaveTypeRepository.class);
        JurisdictionLeaveTypeRepository sourceRepository = mock(JurisdictionLeaveTypeRepository.class);
        TenantActivityService activityService = mock(TenantActivityService.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        LeaveTypeService service = new LeaveTypeService(
                leaveTypeRepository, sourceRepository, activityService, userRepository);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tenant-admin", "n/a", List.of()));
        when(userRepository.findById("tenant-admin")).thenReturn(Optional.of(
                AppUser.builder().loginName("tenant-admin").tenantId("TENANT_A").active(true).build()));

        LeaveType inheritedAnnual = LeaveType.builder()
                .id("TENANT_A:AU-NSW:ANNUAL_LEAVE")
                .tenantId("TENANT_A")
                .name("Annual Leave")
                .jurisdictionId("AU-NSW")
                .sourceJurisdictionLeaveTypeId("AU:ANNUAL_LEAVE")
                .build();
        when(leaveTypeRepository.findAllByTenantId("TENANT_A")).thenReturn(List.of(inheritedAnnual));

        List<LeaveType> result = service.findAll();

        assertThat(result).singleElement().satisfies(leaveType -> {
            assertThat(leaveType.getJurisdictionId()).isEqualTo("AU-NSW");
            assertThat(leaveType.getSourceJurisdictionLeaveTypeId()).isEqualTo("AU:ANNUAL_LEAVE");
        });
    }
}

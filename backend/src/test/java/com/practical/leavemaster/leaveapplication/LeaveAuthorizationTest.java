package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.leaveapprover.LeaveApprover;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveAuthorizationTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private LeaveApproverRepository leaveApproverRepository;

    private LeaveAuthorization authorization;
    private Authentication authentication;
    private Staff alice;
    private Staff bob;
    private Staff manager;

    @BeforeEach
    void setUp() {
        authorization = new LeaveAuthorization(
                appUserRepository,
                staffRepository,
                leaveApplicationRepository,
                leaveApproverRepository);
        authentication = new UsernamePasswordAuthenticationToken("alice-login", "n/a", List.of());
        alice = Staff.builder().id("S001").tenantId("tenant-a").name("Alice").build();
        bob = Staff.builder().id("S002").tenantId("tenant-a").name("Bob").build();
        manager = Staff.builder().id("S003").tenantId("tenant-a").name("Manager").build();
    }

    @Test
    void staffCanAccessOwnStaffScopedEndpoints() {
        mockUser("alice-login", "S001", "tenant-a");
        when(staffRepository.findById("S001")).thenReturn(Optional.of(alice));

        assertThat(authorization.canAccessStaff(authentication, "S001")).isTrue();
    }

    @Test
    void staffCannotForgeAnotherStaffId() {
        mockUser("alice-login", "S001", "tenant-a");
        when(staffRepository.findById("S002")).thenReturn(Optional.of(bob));

        assertThat(authorization.canAccessStaff(authentication, "S002")).isFalse();
        assertThat(authorization.canApplyForStaff(authentication, "S002")).isFalse();
    }

    @Test
    void staffCanReadOwnLeaveApplication() {
        mockUser("alice-login", "S001", "tenant-a");
        when(leaveApplicationRepository.findById("leave-1"))
                .thenReturn(Optional.of(application("leave-1", alice)));

        assertThat(authorization.canReadApplication(authentication, "leave-1")).isTrue();
    }

    @Test
    void staffCannotReadOrWriteUnrelatedLeaveApplication() {
        mockUser("alice-login", "S001", "tenant-a");
        LeaveApplication bobsLeave = application("leave-2", bob);
        when(leaveApplicationRepository.findById("leave-2")).thenReturn(Optional.of(bobsLeave));
        when(leaveApproverRepository.findActiveApproversForStaff(bob, bobsLeave.getLeaveDate()))
                .thenReturn(List.of());

        assertThat(authorization.canReadApplication(authentication, "leave-2")).isFalse();
        assertThat(authorization.canWriteApplication(authentication, "leave-2")).isFalse();
    }

    @Test
    void effectiveApproverCanReadStaffLeaveButCannotModifyIt() {
        authentication = new UsernamePasswordAuthenticationToken("manager-login", "n/a", List.of());
        mockUser("manager-login", "S003", "tenant-a");
        LeaveApplication bobsLeave = application("leave-2", bob);
        when(leaveApplicationRepository.findById("leave-2")).thenReturn(Optional.of(bobsLeave));
        when(leaveApproverRepository.findActiveApproversForStaff(bob, bobsLeave.getLeaveDate()))
                .thenReturn(List.of(LeaveApprover.builder().staff(bob).approver(manager).build()));

        assertThat(authorization.canReadApplication(authentication, "leave-2")).isTrue();
        assertThat(authorization.canWriteApplication(authentication, "leave-2")).isFalse();
        assertThat(authorization.canApproveApplication(authentication, "leave-2")).isTrue();
        assertThat(authorization.canApproveAs(authentication, "leave-2", "S003")).isTrue();
    }

    @Test
    void approverCannotApproveUsingAnotherApproverId() {
        authentication = new UsernamePasswordAuthenticationToken("manager-login", "n/a", List.of());
        mockUser("manager-login", "S003", "tenant-a");

        assertThat(authorization.canActAsApprover(authentication, "S999")).isFalse();
        assertThat(authorization.canApproveAs(authentication, "leave-2", "S999")).isFalse();
    }

    @Test
    void tenantAdminWithoutStaffLinkRetainsAccessWithinOwnTenant() {
        mockUser("alice-login", null, "tenant-a");
        when(staffRepository.findById("S002")).thenReturn(Optional.of(bob));

        assertThat(authorization.canAccessStaff(authentication, "S002")).isTrue();
    }

    @Test
    void tenantAdminCannotCrossTenantBoundary() {
        mockUser("alice-login", null, "tenant-a");
        Staff otherTenantStaff = Staff.builder().id("S900").tenantId("tenant-b").name("Other").build();
        when(staffRepository.findById("S900")).thenReturn(Optional.of(otherTenantStaff));

        assertThat(authorization.canAccessStaff(authentication, "S900")).isFalse();
    }

    @Test
    void platformAccountWithoutTenantOrStaffLinkReliesOnRbacForRowAccess() {
        mockUser("alice-login", null, null);
        when(staffRepository.findById("S002")).thenReturn(Optional.of(bob));

        assertThat(authorization.canAccessStaff(authentication, "S002")).isTrue();
    }

    @Test
    void missingApplicationIsAllowedThroughSoControllerCanReturn404() {
        mockUser("alice-login", "S001", "tenant-a");
        when(leaveApplicationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThat(authorization.canReadApplication(authentication, "missing")).isTrue();
        assertThat(authorization.canWriteApplication(authentication, "missing")).isTrue();
    }

    private void mockUser(String loginName, String staffId, String tenantId) {
        AppUser user = AppUser.builder()
                .loginName(loginName)
                .password("password")
                .active(true)
                .staffId(staffId)
                .tenantId(tenantId)
                .build();
        when(appUserRepository.findById(loginName)).thenReturn(Optional.of(user));
    }

    private LeaveApplication application(String id, Staff staff) {
        return LeaveApplication.builder()
                .id(id)
                .staff(staff)
                .tenantId(staff.getTenantId())
                .leaveDate(LocalDate.of(2026, 8, 20))
                .status(LeaveStatus.PENDING)
                .build();
    }
}

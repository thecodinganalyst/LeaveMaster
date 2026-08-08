package com.practical.leavemaster.mcp;

import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.tenant.Tenant;
import com.practical.leavemaster.tenant.TenantService;
import com.practical.leavemaster.tenant.TenantStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(McpAuthorizationTest.TestConfig.class)
class McpAuthorizationTest {

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        TenantService tenantService() {
            return mock(TenantService.class);
        }

        @Bean
        TenantMcpTools tenantMcpTools(TenantService tenantService) {
            return new TenantMcpTools(tenantService);
        }
    }

    @Autowired
    private TenantMcpTools tenantMcpTools;

    @Autowired
    private TenantService tenantService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldEnforceTenantReadAndWritePermissions() {
        authenticateWith(RbacPermissions.TENANT_READ);
        when(tenantService.findAll()).thenReturn(java.util.List.of());

        tenantMcpTools.getAllTenants();
        verify(tenantService).findAll();

        Tenant tenant = Tenant.builder().id("t1").name("Tenant One").status(TenantStatus.ACTIVE).build();
        assertThatThrownBy(() -> tenantMcpTools.createTenant(tenant))
            .isInstanceOf(AccessDeniedException.class);

        authenticateWith(RbacPermissions.TENANT_WRITE);
        when(tenantService.save(tenant)).thenReturn(tenant);

        assertThat(tenantMcpTools.createTenant(tenant)).isSameAs(tenant);
        verify(tenantService).save(tenant);
    }

    @Test
    void shouldDeclareAuthorizationForEveryMcpTool() {
        assertPermissions(TenantMcpTools.class, Map.of(
            RbacPermissions.TENANT_READ, new String[]{"getAllTenants", "getTenantById"},
            RbacPermissions.TENANT_WRITE, new String[]{"createTenant", "updateTenant", "deleteTenant"}
        ));
        assertPermissions(StaffMcpTools.class, Map.of(
            RbacPermissions.STAFF_READ, new String[]{"getAllStaff", "getStaffById"},
            RbacPermissions.STAFF_WRITE, new String[]{"createStaff", "updateStaff", "deleteStaff", "terminateStaff"}
        ));
        assertPermissions(AppUserMcpTools.class, Map.of(
            RbacPermissions.USER_READ, new String[]{"getAllUsers", "getUserByLoginName"},
            RbacPermissions.USER_WRITE, new String[]{"createUser", "updateUser", "changePassword", "activateUser", "deactivateUser", "deleteUser"}
        ));
        assertPermissions(AppRoleMcpTools.class, Map.of(
            RbacPermissions.ROLE_MANAGE, new String[]{"getAllRoles", "getRolesByTenantId", "getAllPermissions", "getRoleById", "createRole", "updateRole", "disableRole", "enableRole", "addUserToRole", "removeUserFromRole"}
        ));
        assertPermissions(LocationMcpTools.class, Map.of(
            RbacPermissions.LOCATION_READ, new String[]{"getAllLocations", "getLocationById"},
            RbacPermissions.LOCATION_WRITE, new String[]{"createLocation", "updateLocation", "deleteLocation"}
        ));
        assertPermissions(LeaveTypeMcpTools.class, Map.of(
            RbacPermissions.LEAVE_TYPE_READ, new String[]{"getAllLeaveTypes", "getLeaveTypeById"},
            RbacPermissions.LEAVE_TYPE_WRITE, new String[]{"createLeaveType", "updateLeaveType", "deleteLeaveType"}
        ));
        assertPermissions(LeaveCalendarMcpTools.class, Map.of(
            RbacPermissions.LEAVE_CALENDAR_READ, new String[]{"getAllLeaveCalendars", "getLeaveCalendarById"},
            RbacPermissions.LEAVE_CALENDAR_WRITE, new String[]{"createLeaveCalendar"}
        ));
        assertPermissions(LeaveApproverMcpTools.class, Map.of(
            RbacPermissions.LEAVE_APPROVER_READ, new String[]{"getAllLeaveApprovers", "getLeaveApproversByStaffId", "getLeaveApproverById"},
            RbacPermissions.LEAVE_APPROVER_WRITE, new String[]{"createLeaveApprover", "updateLeaveApprover", "deleteLeaveApprover"}
        ));
        assertPermissions(LeaveApplicationMcpTools.class, Map.of(
            RbacPermissions.LEAVE_APPLICATION_READ, new String[]{"getAllLeaveApplications", "getLeaveApplicationById", "getLeaveApplicationsByStaffId", "getVisibleLeaveApplicationsForStaff", "getPendingLeaveApplicationsByApproverId", "getLeaveBalances"},
            RbacPermissions.LEAVE_APPLICATION_WRITE, new String[]{"applyForLeave", "updateLeaveApplication", "deleteLeaveApplication"},
            RbacPermissions.LEAVE_APPLICATION_APPROVE, new String[]{"approveLeaveApplication", "rejectLeaveApplication", "approveCancellation", "rejectCancellation"}
        ));
    }

    private void authenticateWith(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "mcp-user",
                "n/a",
                java.util.List.of(new SimpleGrantedAuthority(authority))
            )
        );
    }

    private void assertPermissions(Class<?> toolClass, Map<String, String[]> expectedPermissions) {
        Map<String, String> actualPermissions = Arrays.stream(toolClass.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(org.springframework.ai.tool.annotation.Tool.class))
            .collect(java.util.stream.Collectors.toMap(Method::getName, method -> {
                PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
                assertThat(annotation)
                    .as("%s.%s must declare @PreAuthorize", toolClass.getSimpleName(), method.getName())
                    .isNotNull();
                return annotation.value();
            }));

        int expectedMethodCount = expectedPermissions.values().stream().mapToInt(methods -> methods.length).sum();
        assertThat(actualPermissions).hasSize(expectedMethodCount);

        expectedPermissions.forEach((permission, methods) -> {
            String expression = "hasAuthority('" + permission + "')";
            for (String method : methods) {
                assertThat(actualPermissions)
                    .as("%s.%s permission", toolClass.getSimpleName(), method)
                    .containsEntry(method, expression);
            }
        });
    }
}

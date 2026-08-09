package com.practical.leavemaster.assistant;

import com.practical.leavemaster.rbac.RbacPermissions;

import java.util.Map;
import java.util.Set;

final class AssistantToolPolicy {
    private AssistantToolPolicy() {
    }

    static final Map<String, String> REQUIRED_AUTHORITY = Map.ofEntries(
            Map.entry("getAllTenants", RbacPermissions.TENANT_READ), Map.entry("getTenantById", RbacPermissions.TENANT_READ),
            Map.entry("createTenant", RbacPermissions.TENANT_WRITE), Map.entry("updateTenant", RbacPermissions.TENANT_WRITE), Map.entry("deleteTenant", RbacPermissions.TENANT_WRITE),
            Map.entry("getAllStaff", RbacPermissions.STAFF_READ), Map.entry("getStaffById", RbacPermissions.STAFF_READ),
            Map.entry("createStaff", RbacPermissions.STAFF_WRITE), Map.entry("updateStaff", RbacPermissions.STAFF_WRITE), Map.entry("deleteStaff", RbacPermissions.STAFF_WRITE), Map.entry("terminateStaff", RbacPermissions.STAFF_WRITE),
            Map.entry("getAllUsers", RbacPermissions.USER_READ), Map.entry("getUserByLoginName", RbacPermissions.USER_READ),
            Map.entry("createUser", RbacPermissions.USER_WRITE), Map.entry("updateUser", RbacPermissions.USER_WRITE), Map.entry("changePassword", RbacPermissions.USER_WRITE), Map.entry("activateUser", RbacPermissions.USER_WRITE), Map.entry("deactivateUser", RbacPermissions.USER_WRITE), Map.entry("deleteUser", RbacPermissions.USER_WRITE),
            Map.entry("getAllRoles", RbacPermissions.ROLE_MANAGE), Map.entry("getRolesByTenantId", RbacPermissions.ROLE_MANAGE), Map.entry("getAllPermissions", RbacPermissions.ROLE_MANAGE), Map.entry("getRoleById", RbacPermissions.ROLE_MANAGE),
            Map.entry("createRole", RbacPermissions.ROLE_MANAGE), Map.entry("updateRole", RbacPermissions.ROLE_MANAGE), Map.entry("disableRole", RbacPermissions.ROLE_MANAGE), Map.entry("enableRole", RbacPermissions.ROLE_MANAGE), Map.entry("addUserToRole", RbacPermissions.ROLE_MANAGE), Map.entry("removeUserFromRole", RbacPermissions.ROLE_MANAGE),
            Map.entry("getAllLocations", RbacPermissions.LOCATION_READ), Map.entry("getLocationById", RbacPermissions.LOCATION_READ), Map.entry("createLocation", RbacPermissions.LOCATION_WRITE), Map.entry("updateLocation", RbacPermissions.LOCATION_WRITE), Map.entry("deleteLocation", RbacPermissions.LOCATION_WRITE),
            Map.entry("getAllLeaveTypes", RbacPermissions.LEAVE_TYPE_READ), Map.entry("getLeaveTypeById", RbacPermissions.LEAVE_TYPE_READ), Map.entry("createLeaveType", RbacPermissions.LEAVE_TYPE_WRITE), Map.entry("updateLeaveType", RbacPermissions.LEAVE_TYPE_WRITE), Map.entry("deleteLeaveType", RbacPermissions.LEAVE_TYPE_WRITE),
            Map.entry("getAllLeaveCalendars", RbacPermissions.LEAVE_CALENDAR_READ), Map.entry("getLeaveCalendarById", RbacPermissions.LEAVE_CALENDAR_READ), Map.entry("createLeaveCalendar", RbacPermissions.LEAVE_CALENDAR_WRITE),
            Map.entry("getAllLeaveApprovers", RbacPermissions.LEAVE_APPROVER_READ), Map.entry("getLeaveApproversByStaffId", RbacPermissions.LEAVE_APPROVER_READ), Map.entry("getLeaveApproverById", RbacPermissions.LEAVE_APPROVER_READ), Map.entry("createLeaveApprover", RbacPermissions.LEAVE_APPROVER_WRITE), Map.entry("updateLeaveApprover", RbacPermissions.LEAVE_APPROVER_WRITE), Map.entry("deleteLeaveApprover", RbacPermissions.LEAVE_APPROVER_WRITE),
            Map.entry("getAllLeaveApplications", RbacPermissions.LEAVE_APPLICATION_READ), Map.entry("getLeaveApplicationById", RbacPermissions.LEAVE_APPLICATION_READ), Map.entry("getLeaveApplicationsByStaffId", RbacPermissions.LEAVE_APPLICATION_READ), Map.entry("getVisibleLeaveApplicationsForStaff", RbacPermissions.LEAVE_APPLICATION_READ), Map.entry("getPendingLeaveApplicationsByApproverId", RbacPermissions.LEAVE_APPLICATION_READ), Map.entry("getLeaveBalances", RbacPermissions.LEAVE_APPLICATION_READ),
            Map.entry("applyForLeave", RbacPermissions.LEAVE_APPLICATION_WRITE), Map.entry("updateLeaveApplication", RbacPermissions.LEAVE_APPLICATION_WRITE), Map.entry("deleteLeaveApplication", RbacPermissions.LEAVE_APPLICATION_WRITE),
            Map.entry("approveLeaveApplication", RbacPermissions.LEAVE_APPLICATION_APPROVE), Map.entry("rejectLeaveApplication", RbacPermissions.LEAVE_APPLICATION_APPROVE), Map.entry("approveCancellation", RbacPermissions.LEAVE_APPLICATION_APPROVE), Map.entry("rejectCancellation", RbacPermissions.LEAVE_APPLICATION_APPROVE)
    );

    static final Set<String> WRITE_TOOLS = Set.of(
            "createTenant", "updateTenant", "deleteTenant", "createStaff", "updateStaff", "deleteStaff", "terminateStaff",
            "createUser", "updateUser", "changePassword", "activateUser", "deactivateUser", "deleteUser",
            "createRole", "updateRole", "disableRole", "enableRole", "addUserToRole", "removeUserFromRole",
            "createLocation", "updateLocation", "deleteLocation", "createLeaveType", "updateLeaveType", "deleteLeaveType", "createLeaveCalendar",
            "createLeaveApprover", "updateLeaveApprover", "deleteLeaveApprover", "applyForLeave", "updateLeaveApplication", "deleteLeaveApplication",
            "approveLeaveApplication", "rejectLeaveApplication", "approveCancellation", "rejectCancellation"
    );
}

package com.practical.leavemaster.testsupport;

import com.practical.leavemaster.leaveapprover.LeaveApprover;
import com.practical.leavemaster.leaveeligibility.StaffDependant;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.staff.DaySchedule;
import com.practical.leavemaster.staff.EmploymentType;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.WorkScheduleDay;
import com.practical.leavemaster.tenant.Tenant;
import com.practical.leavemaster.tenant.TenantStatus;
import com.practical.leavemaster.user.AppUser;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reusable, persistence-agnostic object factory for automated business scenarios.
 *
 * <p>The factory lives in shared source so the E2E-only bootstrap service can reuse exactly the
 * same fixture definition as backend tests. Nothing here registers a Spring bean or exposes an
 * endpoint; runtime exposure is controlled separately by the {@code e2e} profile.</p>
 */
public final class ScenarioDataFactory {

    public static final String SG = "SG";
    public static final String ANNUAL_LEAVE = "ANNUAL_LEAVE";

    private ScenarioDataFactory() {
    }

    public static Scenario standardSingaporeScenario(String scenarioId, LocalDate referenceDate) {
        String key = requireScenarioId(scenarioId);
        String tenantId = "E2E-" + key;
        LocalDate yearStart = referenceDate.withDayOfYear(1);
        LocalDate yearEnd = referenceDate.withMonth(12).withDayOfMonth(31);

        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .name("E2E Singapore " + key)
                .jurisdictionId(SG)
                .jurisdictionIds(List.of(SG))
                .startDate(yearStart.minusYears(3))
                .status(TenantStatus.ACTIVE)
                .lastModified(LocalDateTime.of(referenceDate, java.time.LocalTime.MIDNIGHT))
                .build();

        Map<String, AppRole> roles = new LinkedHashMap<>();
        roles.put("admin", role(tenantId, tenantId + "_Admin", "Tenant administrator"));
        roles.put("hr", role(tenantId, tenantId + "_HR", "Human resources"));
        roles.put("manager", role(tenantId, tenantId + "_Manager", "Leave manager"));
        roles.put("staff", role(tenantId, tenantId + "_Staff", "Staff"));

        Map<String, Staff> staff = new LinkedHashMap<>();
        staff.put("admin", staff(tenantId, "admin", "E2E Admin", yearStart.minusYears(2), SG, Set.of(roles.get("admin").getId())));
        staff.put("hr", staff(tenantId, "hr", "E2E HR", yearStart.minusYears(2), SG, Set.of(roles.get("hr").getId())));
        staff.put("manager01", staff(tenantId, "manager01", "E2E Manager 01", yearStart.minusYears(2), SG, Set.of(roles.get("manager").getId())));
        staff.put("manager02", staff(tenantId, "manager02", "E2E Manager 02", yearStart.minusYears(1), SG, Set.of(roles.get("manager").getId())));
        staff.put("staff001", staff(tenantId, "staff001", "E2E Normal Staff", yearStart.minusYears(2), SG, Set.of(roles.get("staff").getId())));
        staff.put("staff002", staff(tenantId, "staff002", "E2E Mid-year Joiner", yearStart.plusMonths(6), SG, Set.of(roles.get("staff").getId())));
        staff.put("staff003", staff(tenantId, "staff003", "E2E Recent Joiner", referenceDate.minusDays(14), SG, Set.of(roles.get("staff").getId())));
        staff.put("staff004", staff(tenantId, "staff004", "E2E Jurisdiction Override", yearStart.minusYears(1), SG, Set.of(roles.get("staff").getId())));
        staff.put("staff005", staff(tenantId, "staff005", "E2E Missing Approver", yearStart.minusYears(1), SG, Set.of(roles.get("staff").getId())));

        Map<String, AppUser> users = new LinkedHashMap<>();
        users.put("admin", user(tenantId, "admin", staff.get("admin"), roles.get("admin")));
        users.put("hr", user(tenantId, "hr", staff.get("hr"), roles.get("hr")));
        users.put("manager01", user(tenantId, "manager01", staff.get("manager01"), roles.get("manager")));
        users.put("manager02", user(tenantId, "manager02", staff.get("manager02"), roles.get("manager")));
        for (int i = 1; i <= 5; i++) {
            String alias = "staff%03d".formatted(i);
            users.put(alias, user(tenantId, alias, staff.get(alias), roles.get("staff")));
        }

        LeaveType annualLeave = LeaveType.builder()
                .id(tenantId + ":" + SG + ":" + ANNUAL_LEAVE)
                .name("Annual Leave")
                .used(true)
                .active(true)
                .statutory(true)
                .paid(true)
                .tenantId(tenantId)
                .jurisdictionId(SG)
                .sourceJurisdictionLeaveTypeId(SG + ":" + ANNUAL_LEAVE)
                .effectiveFrom(yearStart.minusYears(10))
                .build();

        Map<String, LeaveEntitlement> entitlements = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            String alias = "staff%03d".formatted(i);
            Staff employee = staff.get(alias);
            LocalDate from = employee.getJoinDate().isAfter(yearStart) ? employee.getJoinDate() : yearStart;
            BigDecimal amount = alias.equals("staff002") ? new BigDecimal("7.00") : new BigDecimal("14.00");
            LeaveEntitlement entitlement = LeaveEntitlement.builder()
                    .id(tenantId + "-entitlement-" + alias)
                    .staff(employee)
                    .leaveType(annualLeave)
                    .from(from)
                    .to(yearEnd)
                    .entitlement(amount)
                    .baseEntitlementAmount(amount)
                    .tenantId(tenantId)
                    .policyId(tenantId + "-annual-policy")
                    .generatedAt(Instant.parse(referenceDate + "T00:00:00Z"))
                    .build();
            employee.getLeaveEntitlements().add(entitlement);
            entitlements.put(alias, entitlement);
        }

        List<LeaveApprover> approvers = new ArrayList<>();
        approvers.add(approver(tenantId, staff.get("staff001"), staff.get("manager01"), staff.get("admin"), yearStart));
        approvers.add(approver(tenantId, staff.get("staff002"), staff.get("manager01"), staff.get("admin"), yearStart));
        approvers.add(approver(tenantId, staff.get("staff003"), staff.get("manager02"), staff.get("admin"), yearStart));
        approvers.add(approver(tenantId, staff.get("staff004"), staff.get("manager02"), staff.get("admin"), yearStart));

        StaffDependant dependant = dependant(tenantId, staff.get("staff001"), "child01", referenceDate.minusYears(2));
        staff.get("staff001").setPreviewDependants(List.of(dependant));

        return new Scenario(key, tenant, roles, users, staff, List.of(annualLeave), entitlements,
                approvers, List.of(dependant), referenceDate);
    }

    public static Staff withJurisdiction(Staff source, String jurisdictionId) {
        return source.toBuilder().jurisdictionId(jurisdictionId).build();
    }

    public static StaffDependant dependant(String tenantId, Staff staff, String alias, LocalDate dateOfBirth) {
        return StaffDependant.builder()
                .id(stableUuid(tenantId + ":dependant:" + alias))
                .tenantId(tenantId)
                .staffId(staff.getId())
                .name("E2E " + alias)
                .relationshipCode("CHILD")
                .dateOfBirth(dateOfBirth)
                .citizenshipCode("SG")
                .residencyCode("SG")
                .effectiveFrom(staff.getJoinDate())
                .active(true)
                .build();
    }

    private static Staff staff(String tenantId, String alias, String name, LocalDate joinDate,
                               String jurisdictionId, Set<String> roleIds) {
        return Staff.builder()
                .id(tenantId + "-" + alias)
                .name(name)
                .email(alias + "@example.test")
                .joinDate(joinDate)
                .jurisdictionId(jurisdictionId)
                .employmentType(EmploymentType.FULL_TIME)
                .workSchedule(weekdaySchedule())
                .tenantId(tenantId)
                .loginName(alias)
                .roleIds(new LinkedHashSet<>(roleIds))
                .build();
    }

    private static List<WorkScheduleDay> weekdaySchedule() {
        return List.of(
                workDay(DayOfWeek.MONDAY),
                workDay(DayOfWeek.TUESDAY),
                workDay(DayOfWeek.WEDNESDAY),
                workDay(DayOfWeek.THURSDAY),
                workDay(DayOfWeek.FRIDAY)
        );
    }

    private static WorkScheduleDay workDay(DayOfWeek dayOfWeek) {
        return WorkScheduleDay.builder().dayOfWeek(dayOfWeek).daySchedule(DaySchedule.FULL).build();
    }

    private static AppRole role(String tenantId, String id, String description) {
        return AppRole.builder().id(id).description(description).active(true).tenantId(tenantId).build();
    }

    private static AppUser user(String tenantId, String alias, Staff staff, AppRole role) {
        return AppUser.builder()
                .userId(stableUuid(tenantId + ":user:" + alias))
                .loginName(alias)
                .password("e2e-password")
                .email(staff.getEmail())
                .active(true)
                .staffId(staff.getId())
                .tenantId(tenantId)
                .roles(new LinkedHashSet<>(Set.of(role)))
                .build();
    }

    private static LeaveApprover approver(String tenantId, Staff employee, Staff manager, Staff admin,
                                          LocalDate effectiveFrom) {
        return LeaveApprover.builder()
                .id(tenantId + "-approver-" + employee.getLoginName())
                .staff(employee)
                .approver(manager)
                .effectiveFrom(effectiveFrom)
                .admin(admin)
                .adminLoginName(admin.getLoginName())
                .adminDate(effectiveFrom)
                .tenantId(tenantId)
                .build();
    }

    private static String stableUuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static String normalizeScenarioId(String scenarioId) {
        return requireScenarioId(scenarioId);
    }

    private static String requireScenarioId(String scenarioId) {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId must not be blank");
        }
        String normalized = scenarioId.trim().replaceAll("[^A-Za-z0-9_-]", "-");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("scenarioId must contain at least one letter, number, underscore or dash");
        }
        return normalized;
    }

    public record Scenario(String scenarioId, Tenant tenant, Map<String, AppRole> roles,
                           Map<String, AppUser> users, Map<String, Staff> staff,
                           List<LeaveType> leaveTypes, Map<String, LeaveEntitlement> entitlements,
                           List<LeaveApprover> approvers, List<StaffDependant> dependants,
                           LocalDate referenceDate) {
        public Staff staff(String alias) {
            Staff value = staff.get(alias);
            if (value == null) throw new IllegalArgumentException("Unknown staff alias: " + alias);
            return value;
        }

        public AppUser user(String alias) {
            AppUser value = users.get(alias);
            if (value == null) throw new IllegalArgumentException("Unknown user alias: " + alias);
            return value;
        }
    }
}

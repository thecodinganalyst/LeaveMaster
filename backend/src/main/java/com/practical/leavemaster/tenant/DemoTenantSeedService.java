package com.practical.leavemaster.tenant;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapplication.LeaveDuration;
import com.practical.leavemaster.leaveapplication.LeaveStatus;
import com.practical.leavemaster.leaveapprover.LeaveApprover;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.staff.DaySchedule;
import com.practical.leavemaster.staff.EmploymentType;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.staff.WorkScheduleDay;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DemoTenantSeedService {

    private static final String SG = "SG";
    private static final String ANNUAL_LEAVE_SOURCE_ID = "SG:ANNUAL_LEAVE";

    private final TenantRepository tenantRepository;
    private final TenantService tenantService;
    private final StaffRepository staffRepository;
    private final AppRoleRepository appRoleRepository;
    private final AppUserRepository appUserRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveApproverRepository leaveApproverRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${demo.tenant.id:DEMO}")
    private String configuredTenantId;

    @Value("${demo.tenant.password:Demo123!}")
    private String demoPassword;

    @Transactional
    public DemoSeedResult resetConfiguredDemoTenant() {
        return reset(configuredTenantId, true);
    }

    @Transactional
    public DemoSeedResult resetExistingDemoTenant(String tenantId) {
        return reset(tenantId, false);
    }

    private DemoSeedResult reset(String tenantId, boolean allowCreate) {
        String normalizedTenantId = normalizeTenantId(tenantId);
        Tenant existing = tenantRepository.findById(normalizedTenantId).orElse(null);
        if (existing == null && !allowCreate) {
            throw new TenantNotFoundException(normalizedTenantId);
        }
        if (existing != null && existing.getType() != TenantType.DEMO) {
            throw new DemoTenantOperationException("reset non-DEMO tenant " + normalizedTenantId);
        }

        if (existing != null) {
            tenantService.delete(normalizedTenantId);
        }

        LocalDate today = LocalDate.now();
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate yearEnd = today.withMonth(12).withDayOfMonth(31);

        Tenant tenant = Tenant.builder()
                .id(normalizedTenantId)
                .name("Demo Company Pte Ltd")
                .tenantAdminEmail("admin@demo.invalid")
                .jurisdictionId(SG)
                .jurisdictionIds(List.of(SG))
                .startDate(yearStart.minusYears(5))
                .status(TenantStatus.ACTIVE)
                .type(TenantType.DEMO)
                .lastModified(LocalDateTime.now())
                .build();
        tenantService.save(tenant);

        AppRole hrRole = role(normalizedTenantId, "HR");
        AppRole managerRole = role(normalizedTenantId, "Manager");
        AppRole staffRole = role(normalizedTenantId, "Staff");
        LeaveType annualLeave = annualLeaveType(normalizedTenantId);

        Staff hr = staff(normalizedTenantId, "HR001", "Hannah Lim", "hannah.hr@demo.invalid",
                yearStart.minusYears(4), Set.of(hrRole.getId()));
        Staff manager = staff(normalizedTenantId, "MGR001", "Marcus Tan", "marcus.manager@demo.invalid",
                yearStart.minusYears(3), Set.of(managerRole.getId()));
        Staff alice = staff(normalizedTenantId, "EMP001", "Alice Ong", "alice.staff@demo.invalid",
                yearStart.minusYears(2), Set.of(staffRole.getId()));
        Staff ben = staff(normalizedTenantId, "EMP002", "Ben Lee", "ben.staff@demo.invalid",
                yearStart.minusYears(1), Set.of(staffRole.getId()));

        addAnnualEntitlement(hr, annualLeave, normalizedTenantId, yearStart, yearEnd, new BigDecimal("18.00"), today);
        addAnnualEntitlement(manager, annualLeave, normalizedTenantId, yearStart, yearEnd, new BigDecimal("18.00"), today);
        addAnnualEntitlement(alice, annualLeave, normalizedTenantId, yearStart, yearEnd, new BigDecimal("14.00"), today);
        addAnnualEntitlement(ben, annualLeave, normalizedTenantId, yearStart, yearEnd, new BigDecimal("14.00"), today);
        staffRepository.saveAll(List.of(hr, manager, alice, ben));

        leaveApproverRepository.saveAll(List.of(
                approver(normalizedTenantId, alice, manager, hr, yearStart),
                approver(normalizedTenantId, ben, manager, hr, yearStart)
        ));

        appUserRepository.saveAll(List.of(
                user(normalizedTenantId, "demo.hr", hr, hrRole),
                user(normalizedTenantId, "demo.manager", manager, managerRole),
                user(normalizedTenantId, "demo.staff", alice, staffRole),
                user(normalizedTenantId, "demo.staff2", ben, staffRole)
        ));

        leaveApplicationRepository.saveAll(List.of(
                application(normalizedTenantId, alice, annualLeave, manager, today.plusDays(7), LeaveStatus.PENDING, today.minusDays(1), null),
                application(normalizedTenantId, alice, annualLeave, manager, today.minusDays(14), LeaveStatus.APPROVED, today.minusDays(21), today.minusDays(20)),
                application(normalizedTenantId, ben, annualLeave, manager, today.minusDays(30), LeaveStatus.DENIED, today.minusDays(35), today.minusDays(34)),
                application(normalizedTenantId, ben, annualLeave, manager, today.plusDays(21), LeaveStatus.APPROVED, today.minusDays(3), today.minusDays(2))
        ));

        return new DemoSeedResult(normalizedTenantId, TenantType.DEMO, 4, 4, 4, today);
    }

    private AppRole role(String tenantId, String suffix) {
        return appRoleRepository.findById(tenantId + "_" + suffix)
                .orElseThrow(() -> new IllegalStateException("Expected seeded role is missing: " + suffix));
    }

    private LeaveType annualLeaveType(String tenantId) {
        return leaveTypeRepository.findAllByTenantId(tenantId).stream()
                .filter(type -> ANNUAL_LEAVE_SOURCE_ID.equals(type.getSourceJurisdictionLeaveTypeId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Singapore Annual Leave was not provisioned for demo tenant"));
    }

    private Staff staff(String tenantId, String id, String name, String email, LocalDate joinDate, Set<String> roleIds) {
        Staff staff = Staff.builder()
                .id(tenantId + "-" + id)
                .name(name)
                .email(email)
                .joinDate(joinDate)
                .jurisdictionId(SG)
                .employmentType(EmploymentType.FULL_TIME)
                .workSchedule(weekdaySchedule())
                .tenantId(tenantId)
                .loginName(id.toLowerCase())
                .roleIds(new LinkedHashSet<>(roleIds))
                .build();
        return staff;
    }

    private void addAnnualEntitlement(Staff staff, LeaveType leaveType, String tenantId,
                                      LocalDate yearStart, LocalDate yearEnd, BigDecimal amount, LocalDate generatedDate) {
        LeaveEntitlement entitlement = LeaveEntitlement.builder()
                .staff(staff)
                .leaveType(leaveType)
                .from(yearStart.isAfter(staff.getJoinDate()) ? yearStart : staff.getJoinDate())
                .to(yearEnd)
                .entitlement(amount)
                .baseEntitlementAmount(amount)
                .tenantId(tenantId)
                .generatedAt(Instant.parse(generatedDate + "T00:00:00Z"))
                .build();
        staff.getLeaveEntitlements().add(entitlement);
    }

    private AppUser user(String tenantId, String loginName, Staff staff, AppRole role) {
        return AppUser.builder()
                .loginName(loginName)
                .password(passwordEncoder.encode(demoPassword))
                .email(staff.getEmail())
                .active(true)
                .staffId(staff.getId())
                .tenantId(tenantId)
                .roles(Set.of(role))
                .build();
    }

    private LeaveApprover approver(String tenantId, Staff staff, Staff manager, Staff admin, LocalDate effectiveFrom) {
        return LeaveApprover.builder()
                .staff(staff)
                .approver(manager)
                .effectiveFrom(effectiveFrom)
                .admin(admin)
                .adminLoginName("demo.hr")
                .adminDate(effectiveFrom)
                .tenantId(tenantId)
                .build();
    }

    private LeaveApplication application(String tenantId, Staff staff, LeaveType leaveType, Staff approver,
                                         LocalDate leaveDate, LeaveStatus status, LocalDate applicationDate, LocalDate approvalDate) {
        return LeaveApplication.builder()
                .staff(staff)
                .leaveDate(leaveDate)
                .leaveType(leaveType)
                .leaveDuration(LeaveDuration.FULL)
                .status(status)
                .approver(approver)
                .applicationDate(applicationDate)
                .approvalDate(approvalDate)
                .tenantId(tenantId)
                .build();
    }

    private List<WorkScheduleDay> weekdaySchedule() {
        return List.of(
                workDay(DayOfWeek.MONDAY),
                workDay(DayOfWeek.TUESDAY),
                workDay(DayOfWeek.WEDNESDAY),
                workDay(DayOfWeek.THURSDAY),
                workDay(DayOfWeek.FRIDAY)
        );
    }

    private WorkScheduleDay workDay(DayOfWeek dayOfWeek) {
        return WorkScheduleDay.builder().dayOfWeek(dayOfWeek).daySchedule(DaySchedule.FULL).build();
    }

    private String normalizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Demo tenant id must not be blank");
        }
        return tenantId.trim();
    }

    public record DemoSeedResult(
            String tenantId,
            TenantType tenantType,
            int staffCount,
            int userCount,
            int leaveApplicationCount,
            LocalDate baselineDate) {
    }
}

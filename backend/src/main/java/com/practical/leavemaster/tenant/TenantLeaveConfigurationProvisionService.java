package com.practical.leavemaster.tenant;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.Jurisdiction;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveType;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeRepository;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeService;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarRepository;
import com.practical.leavemaster.leavecalendar.PublicHoliday;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRule;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantLeaveConfigurationProvisionService {
    private final JurisdictionRepository jurisdictionRepository;
    private final JurisdictionLeaveTypeService jurisdictionLeaveTypeService;
    private final JurisdictionLeaveTypeRepository jurisdictionLeaveTypeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveEntitlementPolicyRepository policyRepository;
    private final LeaveEntitlementPolicyEligibilityRepository eligibilityRepository;
    private final LeaveCalendarRepository leaveCalendarRepository;

    @Transactional
    public void provision(Tenant tenant) {
        if (tenant.getJurisdictionId() == null || tenant.getJurisdictionId().isBlank()) {
            throw new IllegalArgumentException("Tenant jurisdiction is required");
        }
        jurisdictionRepository.findById(tenant.getJurisdictionId())
                .orElseThrow(() -> new IllegalArgumentException("Jurisdiction not found: " + tenant.getJurisdictionId()));

        Map<String, LeaveType> tenantLeaveTypesByCode = seedLeaveTypes(tenant);
        seedPoliciesAndEligibility(tenant, tenantLeaveTypesByCode);
        seedCalendars(tenant);
    }

    private Map<String, LeaveType> seedLeaveTypes(Tenant tenant) {
        Map<String, LeaveType> byCode = new LinkedHashMap<>();
        List<LeaveType> existing = leaveTypeRepository.findAllByTenantId(tenant.getId());
        for (LeaveType leaveType : existing) {
            if (leaveType.getSourceJurisdictionLeaveTypeId() == null) continue;
            jurisdictionLeaveTypeRepository.findById(leaveType.getSourceJurisdictionLeaveTypeId())
                    .ifPresent(source -> byCode.put(source.getCode(), leaveType));
        }

        for (JurisdictionLeaveType source : jurisdictionLeaveTypeService.resolveEffective(tenant.getJurisdictionId())) {
            LeaveType leaveType = byCode.get(source.getCode());
            if (leaveType == null) {
                leaveType = LeaveType.builder()
                        .id(tenant.getId() + ":" + source.getCode())
                        .name(source.getName())
                        .used(false)
                        .tenantId(tenant.getId())
                        .sourceJurisdictionLeaveTypeId(source.getId())
                        .build();
                leaveType = leaveTypeRepository.save(leaveType);
                byCode.put(source.getCode(), leaveType);
            }
        }
        return byCode;
    }

    private void seedPoliciesAndEligibility(Tenant tenant, Map<String, LeaveType> tenantLeaveTypesByCode) {
        for (LeaveEntitlementPolicy template : effectivePolicyTemplates(tenant.getJurisdictionId()).values()) {
            if (policyRepository.existsByTenantIdAndSourceTemplateId(tenant.getId(), template.getId())) continue;

            JurisdictionLeaveType sourceLeaveType = jurisdictionLeaveTypeRepository.findById(template.getJurisdictionLeaveTypeId())
                    .orElseThrow(() -> new IllegalStateException("Policy template references missing jurisdiction leave type: " + template.getJurisdictionLeaveTypeId()));
            LeaveType tenantLeaveType = tenantLeaveTypesByCode.get(sourceLeaveType.getCode());
            if (tenantLeaveType == null) continue;

            LeaveEntitlementPolicy copied = LeaveEntitlementPolicy.builder()
                    .tenantId(tenant.getId())
                    .scope(ConfigurationScope.TENANT)
                    .leaveTypeId(tenantLeaveType.getId())
                    .sourceTemplateId(template.getId())
                    .name(template.getName())
                    .active(template.isActive())
                    .priority(template.getPriority())
                    .entitlementUnit(template.getEntitlementUnit())
                    .entitlementAmount(template.getEntitlementAmount())
                    .accrualMethod(template.getAccrualMethod())
                    .accrualRate(template.getAccrualRate())
                    .prorationMethod(template.getProrationMethod())
                    .carryForwardAllowed(template.isCarryForwardAllowed())
                    .carryForwardLimit(template.getCarryForwardLimit())
                    .carryForwardExpiryMonths(template.getCarryForwardExpiryMonths())
                    .effectiveFrom(template.getEffectiveFrom())
                    .effectiveTo(template.getEffectiveTo())
                    .build();
            copied = policyRepository.save(copied);

            for (LeaveEntitlementPolicyEligibilityRule rule : eligibilityRepository.findAllByPolicyIdOrderBySortOrderAsc(template.getId())) {
                LeaveEntitlementPolicyEligibilityRule copiedRule = LeaveEntitlementPolicyEligibilityRule.builder()
                        .policyId(copied.getId())
                        .criterionType(rule.getCriterionType())
                        .operator(rule.getOperator())
                        .value(rule.getValue())
                        .active(rule.isActive())
                        .sortOrder(rule.getSortOrder())
                        .build();
                eligibilityRepository.save(copiedRule);
            }
        }
    }

    private Map<String, LeaveEntitlementPolicy> effectivePolicyTemplates(String jurisdictionId) {
        Map<String, LeaveEntitlementPolicy> effective = new LinkedHashMap<>();
        String current = jurisdictionId;
        while (current != null && !current.isBlank()) {
            for (LeaveEntitlementPolicy template : policyRepository.findAllByScopeAndJurisdictionIdAndActiveTrue(ConfigurationScope.PLATFORM_TEMPLATE, current)) {
                JurisdictionLeaveType leaveType = jurisdictionLeaveTypeRepository.findById(template.getJurisdictionLeaveTypeId())
                        .orElse(null);
                if (leaveType != null) {
                    effective.putIfAbsent(leaveType.getCode() + "|" + template.getName(), template);
                }
            }
            Jurisdiction jurisdiction = jurisdictionRepository.findById(current)
                    .orElseThrow(() -> new IllegalArgumentException("Jurisdiction not found: " + current));
            current = jurisdiction.getParentId();
        }
        return effective;
    }

    private void seedCalendars(Tenant tenant) {
        Map<String, LeaveCalendar> effective = new LinkedHashMap<>();
        String current = tenant.getJurisdictionId();
        while (current != null && !current.isBlank()) {
            for (LeaveCalendar template : leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, current)) {
                effective.putIfAbsent(template.getStart() + "|" + template.getEnd(), template);
            }
            Jurisdiction jurisdiction = jurisdictionRepository.findById(current)
                    .orElseThrow(() -> new IllegalArgumentException("Jurisdiction not found: " + current));
            current = jurisdiction.getParentId();
        }

        for (LeaveCalendar template : effective.values()) {
            if (leaveCalendarRepository.existsByTenantIdAndSourceTemplateId(tenant.getId(), template.getId())) continue;
            List<PublicHoliday> holidays = template.getPublicHolidays().stream()
                    .map(holiday -> PublicHoliday.builder()
                            .holidayDate(holiday.getHolidayDate())
                            .holidayName(holiday.getHolidayName())
                            .locationId(null)
                            .build())
                    .toList();
            leaveCalendarRepository.save(LeaveCalendar.builder()
                    .id(UUID.randomUUID().toString())
                    .start(template.getStart())
                    .end(template.getEnd())
                    .publicHolidays(holidays)
                    .tenantId(tenant.getId())
                    .scope(ConfigurationScope.TENANT)
                    .sourceTemplateId(template.getId())
                    .build());
        }
    }
}

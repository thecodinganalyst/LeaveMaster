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
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRule;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantLeaveConfigurationProvisionService {
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    private final JurisdictionRepository jurisdictionRepository;
    private final JurisdictionLeaveTypeService jurisdictionLeaveTypeService;
    private final JurisdictionLeaveTypeRepository jurisdictionLeaveTypeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveEntitlementPolicyRepository policyRepository;
    private final LeaveEntitlementPolicyEligibilityRepository eligibilityRepository;
    private final LeaveCalendarRepository leaveCalendarRepository;

    @Transactional
    public void provision(Tenant tenant) {
        String jurisdictionId = requireJurisdiction(tenant.getJurisdictionId());
        Map<String, LeaveType> tenantLeaveTypesByCode = seedLeaveTypes(tenant, jurisdictionId);
        seedPoliciesAndEligibility(tenant, jurisdictionId, tenantLeaveTypesByCode);
        seedLegacyCalendars(tenant, jurisdictionId);
    }

    @Transactional
    public void provision(Tenant tenant, TenantJurisdictionProvisionRequest request) {
        String jurisdictionId = requireJurisdiction(request.jurisdictionId());

        if (request.shouldIncludeLeaveConfiguration()) {
            Map<String, LeaveType> tenantLeaveTypesByCode = seedLeaveTypes(tenant, jurisdictionId);
            seedPoliciesAndEligibility(tenant, jurisdictionId, tenantLeaveTypesByCode);
        }

        if (request.shouldIncludePublicHolidays()) {
            if (request.calendarStart() == null || request.calendarEnd() == null) {
                throw new IllegalArgumentException("Calendar start and end dates are required when importing public holidays");
            }
            seedCalendarRange(tenant, jurisdictionId, request.calendarStart(), request.calendarEnd());
        }
    }

    private String requireJurisdiction(String jurisdictionId) {
        if (jurisdictionId == null || jurisdictionId.isBlank()) {
            throw new IllegalArgumentException("Tenant jurisdiction is required");
        }
        jurisdictionRepository.findById(jurisdictionId)
                .orElseThrow(() -> new IllegalArgumentException("Jurisdiction not found: " + jurisdictionId));
        return jurisdictionId;
    }

    private Map<String, LeaveType> seedLeaveTypes(Tenant tenant, String jurisdictionId) {
        Map<String, LeaveType> byCode = new LinkedHashMap<>();
        for (LeaveType leaveType : leaveTypeRepository.findAllByTenantId(tenant.getId())) {
            if (leaveType.getSourceJurisdictionLeaveTypeId() == null) continue;
            jurisdictionLeaveTypeRepository.findById(leaveType.getSourceJurisdictionLeaveTypeId())
                    .ifPresent(source -> byCode.put(source.getCode(), leaveType));
        }

        for (JurisdictionLeaveType source : jurisdictionLeaveTypeService.resolveEffective(jurisdictionId)) {
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

    private void seedPoliciesAndEligibility(Tenant tenant, String jurisdictionId, Map<String, LeaveType> tenantLeaveTypesByCode) {
        for (LeaveEntitlementPolicy template : effectivePolicyTemplates(jurisdictionId).values()) {
            if (policyRepository.existsByTenantIdAndSourceTemplateId(tenant.getId(), template.getId())) continue;

            JurisdictionLeaveType sourceLeaveType = jurisdictionLeaveTypeRepository.findById(template.getJurisdictionLeaveTypeId())
                    .orElseThrow(() -> new IllegalStateException("Policy template references missing jurisdiction leave type: " + template.getJurisdictionLeaveTypeId()));
            LeaveType tenantLeaveType = tenantLeaveTypesByCode.get(sourceLeaveType.getCode());
            if (tenantLeaveType == null) continue;

            AccrualMethod accrualMethod = template.getAccrualMethod() == AccrualMethod.ANNUAL
                    ? AccrualMethod.NONE
                    : template.getAccrualMethod();
            BigDecimal accrualRate = derivedAccrualRate(accrualMethod, template.getEntitlementAmount());

            LeaveEntitlementPolicy copied = LeaveEntitlementPolicy.builder()
                    .tenantId(tenant.getId())
                    .scope(ConfigurationScope.TENANT)
                    .leaveTypeId(tenantLeaveType.getId())
                    .sourceTemplateId(template.getId())
                    .name(template.getName())
                    .active(template.isActive())
                    .priority(template.getPriority())
                    .policyModel(template.getPolicyModel())
                    .qualifyingEventTypeCode(template.getQualifyingEventTypeCode())
                    .eventRequiresVerification(template.isEventRequiresVerification())
                    .eventValidityDaysBefore(template.getEventValidityDaysBefore())
                    .eventValidityDaysAfter(template.getEventValidityDaysAfter())
                    .eventEntitlementAmountMode(template.getEventEntitlementAmountMode())
                    .entitlementUnit(template.getEntitlementUnit())
                    .entitlementAmount(template.getEntitlementAmount())
                    .accrualMethod(accrualMethod)
                    .accrualRate(accrualRate)
                    .prorationMethod(template.getProrationMethod())
                    .carryForwardAllowed(template.isCarryForwardAllowed())
                    .carryForwardLimit(template.getCarryForwardLimit())
                    .carryForwardExpiryMonths(template.getCarryForwardExpiryMonths())
                    .effectiveFrom(template.getEffectiveFrom())
                    .effectiveTo(template.getEffectiveTo())
                    .build();
            copied = policyRepository.save(copied);

            for (LeaveEntitlementPolicyEligibilityRule rule : eligibilityRepository.findAllByPolicyIdOrderBySortOrderAsc(template.getId())) {
                eligibilityRepository.save(LeaveEntitlementPolicyEligibilityRule.builder()
                        .policyId(copied.getId())
                        .criterionType(rule.getCriterionType())
                        .operator(rule.getOperator())
                        .value(rule.getValue())
                        .active(rule.isActive())
                        .sortOrder(rule.getSortOrder())
                        .build());
            }
        }
    }

    private BigDecimal derivedAccrualRate(AccrualMethod method, BigDecimal entitlementAmount) {
        if (method != AccrualMethod.MONTHLY || entitlementAmount == null) {
            return null;
        }
        return entitlementAmount.divide(MONTHS_PER_YEAR, 8, RoundingMode.HALF_UP);
    }

    private Map<String, LeaveEntitlementPolicy> effectivePolicyTemplates(String jurisdictionId) {
        Map<String, LeaveEntitlementPolicy> effective = new LinkedHashMap<>();
        String current = jurisdictionId;
        while (current != null && !current.isBlank()) {
            String currentId = current;
            for (LeaveEntitlementPolicy template : policyRepository.findAllByScopeAndJurisdictionIdAndActiveTrue(ConfigurationScope.PLATFORM_TEMPLATE, currentId)) {
                JurisdictionLeaveType leaveType = jurisdictionLeaveTypeRepository.findById(template.getJurisdictionLeaveTypeId()).orElse(null);
                if (leaveType != null) {
                    effective.putIfAbsent(leaveType.getCode() + "|" + template.getName(), template);
                }
            }
            Jurisdiction jurisdiction = jurisdictionRepository.findById(currentId)
                    .orElseThrow(() -> new IllegalArgumentException("Jurisdiction not found: " + currentId));
            current = jurisdiction.getParentId();
        }
        return effective;
    }

    private List<LeaveCalendar> effectiveCalendarTemplates(String jurisdictionId) {
        Map<String, LeaveCalendar> effective = new LinkedHashMap<>();
        String current = jurisdictionId;
        while (current != null && !current.isBlank()) {
            String currentId = current;
            for (LeaveCalendar template : leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, currentId)) {
                effective.putIfAbsent(template.getStart() + "|" + template.getEnd(), template);
            }
            Jurisdiction jurisdiction = jurisdictionRepository.findById(currentId)
                    .orElseThrow(() -> new IllegalArgumentException("Jurisdiction not found: " + currentId));
            current = jurisdiction.getParentId();
        }
        return List.copyOf(effective.values());
    }

    private void seedLegacyCalendars(Tenant tenant, String jurisdictionId) {
        for (LeaveCalendar template : effectiveCalendarTemplates(jurisdictionId)) {
            if (leaveCalendarRepository.existsByTenantIdAndSourceTemplateId(tenant.getId(), template.getId())) continue;
            leaveCalendarRepository.save(copyTemplateCalendar(tenant.getId(), jurisdictionId, template));
        }
    }

    private LeaveCalendar copyTemplateCalendar(String tenantId, String jurisdictionId, LeaveCalendar template) {
        List<PublicHoliday> holidays = template.getPublicHolidays().stream()
                .map(this::copyHoliday)
                .toList();
        return LeaveCalendar.builder()
                .id(UUID.randomUUID().toString())
                .start(template.getStart())
                .end(template.getEnd())
                .publicHolidays(holidays)
                .tenantId(tenantId)
                .scope(ConfigurationScope.TENANT)
                .jurisdictionId(jurisdictionId)
                .sourceTemplateId(template.getId())
                .build();
    }

    private void seedCalendarRange(Tenant tenant, String jurisdictionId, LocalDate start, LocalDate end) {
        List<LeaveCalendar> templates = effectiveCalendarTemplates(jurisdictionId).stream()
                .filter(template -> !template.getEnd().isBefore(start) && !template.getStart().isAfter(end))
                .toList();
        if (templates.isEmpty()) {
            throw new IllegalArgumentException("No public holiday template is available for jurisdiction " + jurisdictionId
                    + " between " + start + " and " + end);
        }

        List<PublicHoliday> requestedHolidays = templates.stream()
                .flatMap(template -> template.getPublicHolidays().stream())
                .filter(holiday -> !holiday.getHolidayDate().isBefore(start) && !holiday.getHolidayDate().isAfter(end))
                .map(this::copyHoliday)
                .toList();

        LeaveCalendar calendar = leaveCalendarRepository
                .findByTenantIdAndJurisdictionIdAndStartAndEnd(tenant.getId(), jurisdictionId, start, end)
                .orElseGet(() -> LeaveCalendar.builder()
                        .id(UUID.randomUUID().toString())
                        .start(start)
                        .end(end)
                        .publicHolidays(new ArrayList<>())
                        .tenantId(tenant.getId())
                        .scope(ConfigurationScope.TENANT)
                        .jurisdictionId(jurisdictionId)
                        .sourceTemplateId(sourceTemplateLineage(templates))
                        .build());

        Set<String> existingKeys = calendar.getPublicHolidays().stream()
                .map(this::holidayKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean changed = false;
        for (PublicHoliday holiday : requestedHolidays) {
            if (existingKeys.add(holidayKey(holiday))) {
                calendar.getPublicHolidays().add(holiday);
                changed = true;
            }
        }

        if (calendar.getSourceTemplateId() == null) {
            calendar.setSourceTemplateId(sourceTemplateLineage(templates));
            changed = true;
        }

        if (calendar.getId() == null || changed || leaveCalendarRepository.findById(calendar.getId()).isEmpty()) {
            leaveCalendarRepository.save(calendar);
        }
    }

    private String sourceTemplateLineage(List<LeaveCalendar> templates) {
        String lineage = templates.stream().map(LeaveCalendar::getId).distinct().collect(Collectors.joining(","));
        return lineage.length() <= 255 ? lineage : null;
    }

    private PublicHoliday copyHoliday(PublicHoliday holiday) {
        return PublicHoliday.builder()
                .holidayDate(holiday.getHolidayDate())
                .holidayName(holiday.getHolidayName())
                .build();
    }

    private String holidayKey(PublicHoliday holiday) {
        return holiday.getHolidayDate() + "|" + holiday.getHolidayName();
    }
}

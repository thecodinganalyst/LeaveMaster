package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.staff.EmploymentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveEntitlementPolicyEligibilityService {
    private final LeaveEntitlementPolicyEligibilityRepository ruleRepository;
    private final LeaveEntitlementPolicyService policyService;
    private final JurisdictionRepository jurisdictionRepository;

    public List<LeaveEntitlementPolicyEligibilityRule> findAllAccessible() {
        List<LeaveEntitlementPolicyEligibilityRule> result = new ArrayList<>();
        for (LeaveEntitlementPolicy policy : policyService.findAll()) {
            result.addAll(ruleRepository.findAllByPolicyIdOrderBySortOrderAsc(policy.getId()));
        }
        return result;
    }

    public Optional<LeaveEntitlementPolicyEligibilityRule> findById(String ruleId) {
        return ruleRepository.findById(ruleId)
                .filter(rule -> policyService.findById(rule.getPolicyId()).isPresent());
    }

    public List<LeaveEntitlementPolicyEligibilityRule> findAll(String policyId) {
        requirePolicy(policyId);
        return ruleRepository.findAllByPolicyIdOrderBySortOrderAsc(policyId);
    }

    @Transactional
    public LeaveEntitlementPolicyEligibilityRule create(LeaveEntitlementPolicyEligibilityRule rule) {
        if (rule.getPolicyId() == null || rule.getPolicyId().isBlank()) {
            throw new LeaveEntitlementPolicyValidationException("policyId is required");
        }
        return create(rule.getPolicyId(), rule);
    }

    @Transactional
    public LeaveEntitlementPolicyEligibilityRule create(String policyId, LeaveEntitlementPolicyEligibilityRule rule) {
        LeaveEntitlementPolicy policy = requirePolicy(policyId);
        rule.setId(null);
        rule.setPolicyId(policyId);
        validate(policy, rule);
        return ruleRepository.save(rule);
    }

    @Transactional
    public LeaveEntitlementPolicyEligibilityRule update(String ruleId, LeaveEntitlementPolicyEligibilityRule requested) {
        LeaveEntitlementPolicyEligibilityRule existing = findById(ruleId)
                .orElseThrow(() -> new LeaveEntitlementPolicyNotFoundException(ruleId));
        return update(existing.getPolicyId(), ruleId, requested);
    }

    @Transactional
    public LeaveEntitlementPolicyEligibilityRule update(String policyId, String ruleId, LeaveEntitlementPolicyEligibilityRule requested) {
        LeaveEntitlementPolicy policy = requirePolicy(policyId);
        LeaveEntitlementPolicyEligibilityRule existing = ruleRepository.findById(ruleId)
                .filter(rule -> policyId.equals(rule.getPolicyId()))
                .orElseThrow(() -> new LeaveEntitlementPolicyNotFoundException(ruleId));
        existing.setCriterionType(requested.getCriterionType());
        existing.setOperator(requested.getOperator());
        existing.setValue(requested.getValue());
        existing.setActive(requested.isActive());
        existing.setSortOrder(requested.getSortOrder());
        validate(policy, existing);
        return ruleRepository.save(existing);
    }

    @Transactional
    public void delete(String ruleId) {
        LeaveEntitlementPolicyEligibilityRule existing = findById(ruleId)
                .orElseThrow(() -> new LeaveEntitlementPolicyNotFoundException(ruleId));
        delete(existing.getPolicyId(), ruleId);
    }

    @Transactional
    public void delete(String policyId, String ruleId) {
        requirePolicy(policyId);
        LeaveEntitlementPolicyEligibilityRule existing = ruleRepository.findById(ruleId)
                .filter(rule -> policyId.equals(rule.getPolicyId()))
                .orElseThrow(() -> new LeaveEntitlementPolicyNotFoundException(ruleId));
        ruleRepository.delete(existing);
    }

    private LeaveEntitlementPolicy requirePolicy(String policyId) {
        return policyService.findById(policyId)
                .orElseThrow(() -> new LeaveEntitlementPolicyNotFoundException(policyId));
    }

    private void validate(LeaveEntitlementPolicy policy, LeaveEntitlementPolicyEligibilityRule rule) {
        if (rule.getCriterionType() == null || rule.getOperator() == null) {
            throw new LeaveEntitlementPolicyValidationException("criterionType and operator are required");
        }
        if (rule.getValue() == null || rule.getValue().isBlank()) {
            throw new LeaveEntitlementPolicyValidationException("criterion value is required");
        }

        switch (rule.getCriterionType()) {
            case JURISDICTION_CODE -> validateJurisdictionRule(rule);
            case SERVICE_MONTHS -> validateServiceMonthsRule(rule);
            case EMPLOYMENT_TYPE -> validateEmploymentTypeRule(rule);
            case HAS_DEPENDANT_MATCHING -> validateDependantRule(rule);
        }
    }

    private void validateJurisdictionRule(LeaveEntitlementPolicyEligibilityRule rule) {
        requireSetOperator(rule.getOperator(), "JURISDICTION_CODE");
        for (String code : values(rule.getValue())) {
            jurisdictionRepository.findByCode(code)
                    .orElseThrow(() -> new LeaveEntitlementPolicyValidationException("Unknown jurisdiction code: " + code));
        }
    }

    private void validateServiceMonthsRule(LeaveEntitlementPolicyEligibilityRule rule) {
        for (String value : values(rule.getValue())) {
            try {
                int months = Integer.parseInt(value);
                if (months < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                throw new LeaveEntitlementPolicyValidationException("SERVICE_MONTHS values must be non-negative integers");
            }
        }
        if ((rule.getOperator() == EligibilityOperator.IN || rule.getOperator() == EligibilityOperator.NOT_IN)) {
            return;
        }
        if (values(rule.getValue()).size() != 1) {
            throw new LeaveEntitlementPolicyValidationException("SERVICE_MONTHS comparison operators require one value");
        }
    }

    private void validateEmploymentTypeRule(LeaveEntitlementPolicyEligibilityRule rule) {
        requireSetOperator(rule.getOperator(), "EMPLOYMENT_TYPE");
        List<String> expectedValues = values(rule.getValue());
        if (expectedValues.isEmpty()) {
            throw new LeaveEntitlementPolicyValidationException("EMPLOYMENT_TYPE requires at least one value");
        }
        if ((rule.getOperator() == EligibilityOperator.EQUALS || rule.getOperator() == EligibilityOperator.NOT_EQUALS)
                && expectedValues.size() != 1) {
            throw new LeaveEntitlementPolicyValidationException(
                    "EMPLOYMENT_TYPE EQUALS and NOT_EQUALS require one value");
        }
        for (String value : expectedValues) {
            try {
                EmploymentType.valueOf(value);
            } catch (IllegalArgumentException ex) {
                throw new LeaveEntitlementPolicyValidationException("Unknown employment type: " + value);
            }
        }
    }

    private void validateDependantRule(LeaveEntitlementPolicyEligibilityRule rule) {
        if (rule.getOperator() != EligibilityOperator.EQUALS && rule.getOperator() != EligibilityOperator.NOT_EQUALS) {
            throw new LeaveEntitlementPolicyValidationException(
                    "HAS_DEPENDANT_MATCHING supports only EQUALS and NOT_EQUALS");
        }
        try {
            DependantEligibilityMatcher.parse(rule.getValue());
        } catch (IllegalArgumentException ex) {
            throw new LeaveEntitlementPolicyValidationException(ex.getMessage());
        }
    }

    private void requireSetOperator(EligibilityOperator operator, String criterion) {
        if (operator != EligibilityOperator.EQUALS && operator != EligibilityOperator.NOT_EQUALS
                && operator != EligibilityOperator.IN && operator != EligibilityOperator.NOT_IN) {
            throw new LeaveEntitlementPolicyValidationException(criterion + " supports only EQUALS, NOT_EQUALS, IN and NOT_IN");
        }
    }

    static List<String> values(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}

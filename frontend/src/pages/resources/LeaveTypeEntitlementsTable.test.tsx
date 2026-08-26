import { describe, expect, it } from 'vitest';

import {
  formatEffectivePeriod,
  formatEligibilitySummary,
  formatEntitlement,
  shouldUseCompactEntitlementLayout,
  type EligibilityRuleSummary,
  type LeaveEntitlementPolicySummary,
} from './LeaveTypeEntitlementsTable.tsx';

describe('LeaveTypeEntitlementsTable formatting', () => {
  it('combines entitlement amount and unit into one value', () => {
    const policy: LeaveEntitlementPolicySummary = {
      id: 'policy-1',
      entitlementAmount: 14,
      entitlementUnit: 'DAYS',
    };

    expect(formatEntitlement(policy)).toBe('14 Days');
  });

  it('summarizes multiple eligibility rules in sort order', () => {
    const rules: EligibilityRuleSummary[] = [
      {
        id: 'rule-2',
        policyId: 'policy-1',
        criterionType: 'SERVICE_MONTHS',
        operator: 'LESS_THAN',
        value: '48',
        active: true,
        sortOrder: 2,
      },
      {
        id: 'rule-1',
        policyId: 'policy-1',
        criterionType: 'SERVICE_MONTHS',
        operator: 'GREATER_THAN_OR_EQUAL',
        value: '24',
        active: true,
        sortOrder: 1,
      },
    ];

    expect(formatEligibilitySummary(rules)).toBe('Service ≥ 2 years and Service < 4 years');
  });

  it('shows all staff when a policy has no eligibility rules', () => {
    expect(formatEligibilitySummary([])).toBe('All staff');
  });

  it('marks inactive eligibility rules without dropping them', () => {
    const rules: EligibilityRuleSummary[] = [{
      id: 'rule-1',
      policyId: 'policy-1',
      criterionType: 'JURISDICTION_CODE',
      operator: 'EQUALS',
      value: 'SG',
      active: false,
      sortOrder: 1,
    }];

    expect(formatEligibilitySummary(rules)).toBe('Jurisdiction = SG (inactive)');
  });

  it('formats an open-ended effective period', () => {
    expect(formatEffectivePeriod({ id: 'policy-1', effectiveFrom: '2026-01-01', effectiveTo: null }))
      .toBe('2026-01-01 — onward');
  });
});

describe('LeaveTypeEntitlementsTable responsive layout', () => {
  it('uses the compact layout when the table is wider than its container', () => {
    expect(shouldUseCompactEntitlementLayout(360, 720)).toBe(true);
  });

  it('keeps the table layout when it fits its container', () => {
    expect(shouldUseCompactEntitlementLayout(900, 720)).toBe(false);
    expect(shouldUseCompactEntitlementLayout(720, 720)).toBe(false);
  });

  it('does not switch layouts before the container has a measurable width', () => {
    expect(shouldUseCompactEntitlementLayout(0, 720)).toBe(false);
  });
});

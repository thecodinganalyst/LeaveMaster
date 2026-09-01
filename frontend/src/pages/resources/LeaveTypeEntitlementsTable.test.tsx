import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../../api/http.ts';
import {
  formatEffectivePeriod,
  formatEligibilitySummary,
  formatEntitlement,
  loadLeaveTypeEntitlements,
  shouldUseCompactEntitlementLayout,
  type EligibilityRuleSummary,
  type LeaveEntitlementPolicySummary,
} from './LeaveTypeEntitlementsTable.tsx';

vi.mock('../../api/http.ts', () => ({ apiFetch: vi.fn() }));

const mockedApiFetch = vi.mocked(apiFetch);

describe('LeaveTypeEntitlementsTable loading', () => {
  beforeEach(() => {
    mockedApiFetch.mockReset();
  });

  it('loads the staff-safe entitlement view scoped to the leave type', async () => {
    const response: LeaveEntitlementPolicySummary[] = [{
      id: 'policy-1',
      entitlementAmount: 14,
      entitlementUnit: 'DAYS',
      effectiveFrom: '2026-01-01',
      active: true,
      eligibilityRules: [{
        criterionType: 'SERVICE_MONTHS',
        operator: 'GREATER_THAN_OR_EQUAL',
        value: '3',
        active: true,
        sortOrder: 1,
      }],
    }];
    mockedApiFetch.mockResolvedValue(response);

    await expect(loadLeaveTypeEntitlements('annual leave')).resolves.toEqual(response);
    expect(mockedApiFetch).toHaveBeenCalledWith('/api/leave-types/annual%20leave/entitlements');
  });
});

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
        criterionType: 'SERVICE_MONTHS',
        operator: 'LESS_THAN',
        value: '48',
        active: true,
        sortOrder: 2,
      },
      {
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

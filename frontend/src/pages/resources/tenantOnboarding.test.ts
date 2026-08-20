import { describe, expect, it } from 'vitest';

import { excludeExistingJurisdictions, tenantOnboardingInitialValues } from './tenantOnboarding.ts';

describe('tenant onboarding helpers', () => {
  it('defaults tenant creation to the selected current calendar year with template imports enabled', () => {
    expect(tenantOnboardingInitialValues('tenants', 2026)).toEqual({
      calendarStart: '2026-01-01',
      calendarEnd: '2026-12-31',
      jurisdictions: [{ includePublicHolidays: true, includeLeaveConfiguration: true }],
    });
  });

  it('defaults tenant admin jurisdiction onboarding to the current calendar year', () => {
    expect(tenantOnboardingInitialValues('tenant-jurisdictions', 2027)).toEqual({
      calendarStart: '2027-01-01',
      calendarEnd: '2027-12-31',
      includePublicHolidays: true,
      includeLeaveConfiguration: true,
    });
  });

  it('excludes jurisdictions that are already associated with the tenant', () => {
    const options = [
      { label: 'Singapore', value: 'SG' },
      { label: 'Malaysia', value: 'MY' },
      { label: 'Australia / NSW', value: 'AU-NSW' },
    ];

    expect(excludeExistingJurisdictions(options, [{ jurisdictionId: 'SG' }, { jurisdictionId: 'AU-NSW' }]))
      .toEqual([{ label: 'Malaysia', value: 'MY' }]);
  });
});

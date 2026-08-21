import { describe, expect, it } from 'vitest';

import { getAdminResourceConfig, normaliseFormValues } from './resourceConfigResolver.ts';

describe('resourceConfigResolver staff write normalization', () => {
  it('submits entitlement leaveTypeId without nested leave type persistence fields', () => {
    const config = getAdminResourceConfig('employees');
    expect(config).toBeDefined();

    const result = normaliseFormValues(config!, {
      id: 'S001',
      name: 'Alice',
      leaveEntitlements: [
        {
          id: null,
          leaveType: {
            id: 'LT-ANNUAL',
            name: 'Annual Leave',
            used: null,
            tenantId: 'tenant-a',
          },
          from: '2026-08-03',
          to: '2026-12-31',
          entitlement: 10,
          policyId: 'POLICY-1',
        },
      ],
    });

    expect(result.leaveEntitlements).toEqual([
      {
        id: null,
        leaveTypeId: 'LT-ANNUAL',
        from: '2026-08-03',
        to: '2026-12-31',
        entitlement: 10,
        policyId: 'POLICY-1',
      },
    ]);
    expect(JSON.stringify(result)).not.toContain('"used"');
    expect(JSON.stringify(result)).not.toContain('"tenantId"');
  });

  it('leaves non-staff resources on the base normalization path', () => {
    const config = getAdminResourceConfig('leave-types');
    expect(config).toBeDefined();

    expect(normaliseFormValues(config!, { id: 'LT-1', name: 'Annual', used: false })).toEqual({
      id: 'LT-1',
      name: 'Annual',
      used: false,
    });
  });
});

import { describe, expect, it } from 'vitest';

import { summariseTenants } from './tenantDashboard.ts';

describe('summariseTenants', () => {
  it('counts tenant lifecycle states for the PlatformAdmin dashboard', () => {
    expect(summariseTenants([
      { id: 'acme', name: 'Acme', status: 'ACTIVE' },
      { id: 'globex', name: 'Globex', status: 'ACTIVE' },
      { id: 'initech', name: 'Initech', status: 'DORMANT' },
      { id: 'umbrella', name: 'Umbrella', status: 'TERMINATED' },
    ])).toEqual({
      total: 4,
      active: 2,
      dormant: 1,
      terminated: 1,
    });
  });

  it('does not misclassify unknown statuses', () => {
    expect(summariseTenants([
      { id: 'future', name: 'Future Tenant', status: 'PENDING' },
    ])).toEqual({
      total: 1,
      active: 0,
      dormant: 0,
      terminated: 0,
    });
  });
});

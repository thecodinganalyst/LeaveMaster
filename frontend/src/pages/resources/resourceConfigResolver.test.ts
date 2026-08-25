import { describe, expect, it } from 'vitest';

import { getAdminResourceConfig, isAdminFieldVisible, normaliseFormValues } from './resourceConfigResolver.ts';

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

  it('submits a cleared optional staff date as null so edits remove the persisted date', () => {
    const config = getAdminResourceConfig('employees');
    expect(config).toBeDefined();

    expect(normaliseFormValues(config!, {
      id: 'S001',
      name: 'Alice',
      joinDate: '2025-01-01',
      termDate: '',
    })).toMatchObject({
      termDate: null,
    });
  });

  it('submits cleared optional dates as null for non-staff resources too', () => {
    const config = getAdminResourceConfig('leave-types');
    expect(config).toBeDefined();

    expect(normaliseFormValues(config!, {
      id: 'LT-1',
      name: 'Annual',
      effectiveFrom: '2026-01-01',
      effectiveTo: '',
    })).toMatchObject({
      effectiveTo: null,
    });
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

  it('exposes tenant-maintainable leave type metadata while hiding source lineage', () => {
    const config = getAdminResourceConfig('leave-types');
    expect(config).toBeDefined();

    expect(config!.fields.find((field) => field.name === 'sourceJurisdictionLeaveTypeId')).toMatchObject({
      hidden: true,
      formHidden: true,
    });
    expect(config!.fields.map((field) => field.name)).toEqual(expect.arrayContaining([
      'active',
      'statutory',
      'paid',
      'sourceName',
      'sourceUrl',
      'effectiveFrom',
      'effectiveTo',
    ]));
    expect(config!.fields.find((field) => field.name === 'effectiveFrom')?.type).toBe('date');
    expect(config!.fields.find((field) => field.name === 'effectiveTo')?.type).toBe('date');
  });

  it('uses tenant-friendly entitlement policy fields and ordering', () => {
    const config = getAdminResourceConfig('leave-entitlement-policies');
    expect(config).toBeDefined();

    const scope = config!.fields.find((field) => field.name === 'scope');
    const sourceTemplateId = config!.fields.find((field) => field.name === 'sourceTemplateId');
    const leaveType = config!.fields.find((field) => field.name === 'leaveTypeId');
    expect(scope).toMatchObject({ label: 'Policy type', audience: 'platform' });
    expect(sourceTemplateId).toMatchObject({ label: 'Source template ID', audience: 'platform' });
    expect(isAdminFieldVisible(scope!, false)).toBe(false);
    expect(isAdminFieldVisible(sourceTemplateId!, false)).toBe(false);
    expect(leaveType?.label).toBe('Leave type');

    const visibleOrder = config!.fields.map((field) => field.name);
    expect(visibleOrder.indexOf('entitlementUnit')).toBe(visibleOrder.indexOf('entitlementAmount') + 1);
  });
});

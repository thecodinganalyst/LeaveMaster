import { describe, expect, it } from 'vitest';

import { shouldHideTenantInternalId, shouldShowResourceIdSubtitle } from './tenantInternalIdVisibility.ts';

const tenantResources = [
  'leave-entitlement-policies',
  'leave-entitlement-policy-eligibility-rules',
];

describe('tenant internal id visibility', () => {
  it('hides the leave type id for every user', () => {
    expect(shouldHideTenantInternalId('leave-types', 'id', 'id', false)).toBe(true);
    expect(shouldHideTenantInternalId('leave-types', 'id', 'id', true)).toBe(true);
    expect(shouldShowResourceIdSubtitle('leave-types', false)).toBe(false);
    expect(shouldShowResourceIdSubtitle('leave-types', true)).toBe(false);
  });

  it.each(tenantResources)('hides the own internal id for tenant users on %s', (resourceName) => {
    expect(shouldHideTenantInternalId(resourceName, 'id', 'id', false)).toBe(true);
    expect(shouldShowResourceIdSubtitle(resourceName, false)).toBe(false);
  });

  it.each(tenantResources)('preserves internal id visibility for platform admins on %s', (resourceName) => {
    expect(shouldHideTenantInternalId(resourceName, 'id', 'id', true)).toBe(false);
    expect(shouldShowResourceIdSubtitle(resourceName, true)).toBe(true);
  });

  it('does not hide foreign-key fields or ids for unrelated resources', () => {
    expect(shouldHideTenantInternalId('leave-entitlement-policies', 'id', 'leaveTypeId', false)).toBe(false);
    expect(shouldHideTenantInternalId('employees', 'id', 'id', false)).toBe(false);
    expect(shouldShowResourceIdSubtitle('employees', false)).toBe(true);
  });
});

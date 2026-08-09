import { describe, expect, it } from 'vitest';

import { adminResourceConfigs, normaliseFormValues, toFormValues } from './adminResourceConfig.ts';

describe('adminResourceConfigs', () => {
  it('registers every administration module from issue 111', () => {
    expect(Object.keys(adminResourceConfigs)).toEqual(
      expect.arrayContaining([
        'tenants',
        'employees',
        'users',
        'roles',
        'locations',
        'leave-types',
        'leave-calendars',
        'leave-approvers',
      ]),
    );
  });

  it('marks leave calendars read/create only because the backend has no update or delete endpoints', () => {
    expect(adminResourceConfigs['leave-calendars'].editable).toBe(false);
    expect(adminResourceConfigs['leave-calendars'].deletable).toBe(false);
  });

  it('converts JSON form inputs into API payload values', () => {
    const values = normaliseFormValues(adminResourceConfigs.roles, {
      id: 'MANAGER',
      description: 'Manager',
      permissionCodes: '["STAFF_READ", "LEAVE_APPLICATION_APPROVE"]',
    });

    expect(values.permissionCodes).toEqual(['STAFF_READ', 'LEAVE_APPLICATION_APPROVE']);
  });

  it('maps role permissions into editable permission codes', () => {
    const values = toFormValues(adminResourceConfigs.roles, {
      id: 'MANAGER',
      description: 'Manager',
      permissions: [{ code: 'STAFF_READ' }, { code: 'LEAVE_APPLICATION_READ' }],
    });

    expect(JSON.parse(String(values.permissionCodes))).toEqual(['STAFF_READ', 'LEAVE_APPLICATION_READ']);
  });

  it('flattens leave approver relationships for administration forms and lists', () => {
    const values = toFormValues(adminResourceConfigs['leave-approvers'], {
      id: '1',
      staff: { id: 'S001' },
      approver: { id: 'S002' },
      admin: { id: 'S003' },
    });

    expect(values).toMatchObject({ staffId: 'S001', approverId: 'S002', adminId: 'S003' });
  });
});

import { describe, expect, it } from 'vitest';

import {
  adminResourceConfigs,
  getAdminResourceInitialValues,
  isAdminFieldVisible,
  normaliseFormValues,
  toFormValues,
} from './adminResourceConfig.ts';

describe('adminResourceConfigs', () => {
  it('registers current administration modules without the removed location module', () => {
    expect(Object.keys(adminResourceConfigs)).toEqual(
      expect.arrayContaining([
        'tenants',
        'jurisdictions',
        'jurisdiction-leave-types',
        'employees',
        'users',
        'roles',
        'leave-types',
        'leave-entitlement-policies',
        'leave-entitlement-policy-eligibility-rules',
        'leave-calendars',
        'leave-approvers',
      ]),
    );
    expect(Object.keys(adminResourceConfigs)).not.toContain('locations');
  });

  it('allows leave calendar templates to be edited and deleted now that the backend supports CRUD', () => {
    expect(adminResourceConfigs['leave-calendars'].editable).not.toBe(false);
    expect(adminResourceConfigs['leave-calendars'].deletable).not.toBe(false);
  });

  it('hides internal entitlement policy scope and tenant id from forms', () => {
    const fields = adminResourceConfigs['leave-entitlement-policies'].fields;
    expect(fields.find((field) => field.name === 'scope')).toMatchObject({
      label: 'Policy type',
      formHidden: true,
      list: true,
    });
    expect(fields.find((field) => field.name === 'tenantId')).toMatchObject({ hidden: true });
  });

  it('shows template fields only to platform admin', () => {
    const fields = adminResourceConfigs['leave-entitlement-policies'].fields;
    const visible = fields.filter((field) => !field.hidden && !field.formHidden && isAdminFieldVisible(field, true)).map((field) => field.name);

    expect(visible).toContain('jurisdictionId');
    expect(visible).toContain('jurisdictionLeaveTypeId');
    expect(visible).not.toContain('leaveTypeId');
    expect(visible).not.toContain('tenantId');
    expect(visible).not.toContain('scope');
  });

  it('shows tenant leave type only to tenant administrators', () => {
    const fields = adminResourceConfigs['leave-entitlement-policies'].fields;
    const visible = fields.filter((field) => !field.hidden && !field.formHidden && isAdminFieldVisible(field, false)).map((field) => field.name);

    expect(visible).toContain('leaveTypeId');
    expect(visible).not.toContain('jurisdictionId');
    expect(visible).not.toContain('jurisdictionLeaveTypeId');
    expect(visible).not.toContain('tenantId');
    expect(visible).not.toContain('scope');
  });

  it('configures priority as a non-negative integer with guidance and a default of 10', () => {
    const priority = adminResourceConfigs['leave-entitlement-policies'].fields.find((field) => field.name === 'priority');

    expect(priority).toMatchObject({
      label: 'Priority (higher number wins)',
      type: 'number',
      required: true,
      min: 0,
      step: 1,
      defaultValue: 10,
    });
    expect(priority?.description).toContain('20 or 30');
    expect(priority?.description).toContain('ambiguous');
    expect(getAdminResourceInitialValues(adminResourceConfigs['leave-entitlement-policies'])).toMatchObject({ priority: 10 });
  });

  it('does not inject defaults when converting an existing policy for editing', () => {
    const config = adminResourceConfigs['leave-entitlement-policies'];
    expect(toFormValues(config, { id: 'policy-1', priority: 30 })).toMatchObject({ priority: 30 });
  });

  it('configures role permissions as a checkbox-backed permission field', () => {
    expect(adminResourceConfigs.roles.fields.find((field) => field.name === 'permissionCodes')).toMatchObject({
      label: 'Permissions',
      type: 'permissions',
      required: true,
    });
  });

  it('keeps selected permission codes as the API payload representation', () => {
    const values = normaliseFormValues(adminResourceConfigs.roles, {
      id: 'MANAGER',
      description: 'Manager',
      permissionCodes: ['STAFF_READ', 'LEAVE_APPLICATION_APPROVE'],
    });

    expect(values.permissionCodes).toEqual(['STAFF_READ', 'LEAVE_APPLICATION_APPROVE']);
  });

  it('maps role permissions into selected permission codes', () => {
    const values = toFormValues(adminResourceConfigs.roles, {
      id: 'MANAGER',
      description: 'Manager',
      permissions: [{ code: 'STAFF_READ' }, { code: 'LEAVE_APPLICATION_READ' }],
    });

    expect(values.permissionCodes).toEqual(['STAFF_READ', 'LEAVE_APPLICATION_READ']);
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

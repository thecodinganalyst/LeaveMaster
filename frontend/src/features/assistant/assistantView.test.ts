import { describe, expect, it } from 'vitest';

import {
  actionEntries,
  actionTitle,
  canConfirmAction,
  dataEntries,
  fieldLabel,
  printableValue,
  resultTitle,
  type AssistantActionItem,
} from './assistantView.ts';

const action = (overrides: Partial<AssistantActionItem> = {}): AssistantActionItem => ({
  id: 'a1',
  toolName: 'applyForLeave',
  arguments: { leaveType: 'Annual Leave', fromDate: '2026-08-13', days: 2 },
  requiredAuthority: 'LEAVE_APPLICATION_WRITE',
  actorLoginName: 'dennis',
  actorStaffId: 'S1',
  tenantId: 'T1',
  state: 'pending',
  ...overrides,
});

describe('assistantView', () => {
  it('turns tool names into readable action and result titles', () => {
    expect(actionTitle('approveLeaveApplication')).toBe('Approve Leave Application');
    expect(actionTitle('create_tenant')).toBe('Create tenant');
    expect(resultTitle('getLeaveBalances')).toBe('Leave Balances');
    expect(resultTitle('getLeaveEntitlementConfigurationByJurisdiction')).toBe('Leave entitlement summary');
  });

  it('renders the exact server proposed arguments and omits null values', () => {
    const entries = actionEntries({ ...action(), arguments: { staffId: 'S1', comment: null, days: 2 } });
    expect(entries).toEqual([['staffId', 'S1'], ['days', 2]]);
    expect(printableValue({ from: '2026-08-13' })).toBe('From: 2026-08-13');
  });

  it('turns structured business objects into human-friendly display rows', () => {
    expect(dataEntries({ leaveType: 'Annual Leave', carryForward: 'Not allowed', unused: null })).toEqual([
      ['Leave type', 'Annual Leave'],
      ['Carry forward', 'Not allowed'],
    ]);
    expect(fieldLabel('entitlementAmount')).toBe('Entitlement Amount');
    expect(dataEntries(['not', 'an', 'object-row'])).toEqual([]);
    expect(printableValue(['At least 3 months', 'Singapore employees'])).toBe('At least 3 months; Singapore employees');
  });

  it('formats grouped policy objects without exposing raw JSON', () => {
    expect(printableValue([
      { policyName: '1st year', eligibility: 'At least 3 months of service', entitlement: '7 days' },
      { policyName: '2nd year', eligibility: 'At least 12 months of service', entitlement: '8 days' },
    ])).toBe(
      'Policy: 1st year · Eligibility: At least 3 months of service · Entitlement: 7 days\n' +
      'Policy: 2nd year · Eligibility: At least 12 months of service · Entitlement: 8 days',
    );
  });

  it('only enables confirmation for pending actions carrying a server token', () => {
    expect(canConfirmAction(action())).toBe(false);
    expect(canConfirmAction(action({ confirmationToken: 'opaque' }))).toBe(true);
    expect(canConfirmAction(action({ confirmationToken: 'opaque', state: 'confirmed' }))).toBe(false);
  });
});

import { describe, expect, it, vi } from 'vitest';

import {
  createEntitlementConfiguration,
  EntitlementPartialSaveError,
  type EntitlementRequest,
  normaliseEligibilityRule,
  updateEntitlementConfiguration,
} from './entitlementWorkflow.ts';

const bodyOf = (init?: RequestInit) => JSON.parse(String(init?.body ?? '{}')) as Record<string, unknown>;

describe('entitlement workflow', () => {
  it('normalises eligibility values and default sort order', () => {
    expect(normaliseEligibilityRule({
      criterionType: 'SERVICE_MONTHS',
      operator: 'GREATER_THAN_OR_EQUAL',
      value: 24,
    }, 'policy-1', 1)).toEqual({
      policyId: 'policy-1',
      criterionType: 'SERVICE_MONTHS',
      operator: 'GREATER_THAN_OR_EQUAL',
      value: '24',
      active: true,
      sortOrder: 20,
    });
  });

  it('creates a policy first and then multiple eligibility rules', async () => {
    const mock = vi.fn(async (path: string) => {
      if (path === '/api/leave-entitlement-policies') return { id: 'policy-1' };
      return { id: `rule-${mock.mock.calls.length}` };
    });
    const request = mock as unknown as EntitlementRequest;

    const result = await createEntitlementConfiguration(request, 'annual', { name: 'Standard' }, [
      { criterionType: 'SERVICE_MONTHS', operator: 'GREATER_THAN_OR_EQUAL', value: '24', active: true, sortOrder: 10 },
      { criterionType: 'SERVICE_MONTHS', operator: 'LESS_THAN', value: '48', active: true, sortOrder: 20 },
    ]);

    expect(result).toBe('policy-1');
    expect(mock).toHaveBeenCalledTimes(3);
    expect(bodyOf(mock.mock.calls[0]?.[1])).toMatchObject({ name: 'Standard', leaveTypeId: 'annual' });
    expect(bodyOf(mock.mock.calls[1]?.[1])).toMatchObject({ policyId: 'policy-1', value: '24' });
    expect(bodyOf(mock.mock.calls[2]?.[1])).toMatchObject({ policyId: 'policy-1', value: '48' });
  });

  it('rolls back newly-created records when eligibility creation fails', async () => {
    const mock = vi.fn(async (path: string, init?: RequestInit) => {
      if (init?.method === 'DELETE') return undefined;
      if (path === '/api/leave-entitlement-policies') return { id: 'policy-1' };
      if (mock.mock.calls.filter(([calledPath]) => calledPath === '/api/leave-entitlement-policy-eligibility-rules').length === 1) {
        return { id: 'rule-1' };
      }
      throw new Error('rule failed');
    });
    const request = mock as unknown as EntitlementRequest;

    await expect(createEntitlementConfiguration(request, 'annual', { name: 'Standard' }, [
      { criterionType: 'SERVICE_MONTHS', operator: 'GREATER_THAN_OR_EQUAL', value: '24' },
      { criterionType: 'SERVICE_MONTHS', operator: 'LESS_THAN', value: '48' },
    ])).rejects.toBeInstanceOf(EntitlementPartialSaveError);

    expect(mock).toHaveBeenCalledWith(
      '/api/leave-entitlement-policy-eligibility-rules/rule-1',
      expect.objectContaining({ method: 'DELETE' }),
    );
    expect(mock).toHaveBeenCalledWith(
      '/api/leave-entitlement-policies/policy-1',
      expect.objectContaining({ method: 'DELETE' }),
    );
  });

  it('updates existing rules, creates new rules, and removes deleted rules', async () => {
    const mock = vi.fn(async () => ({ id: 'saved' }));
    const request = mock as unknown as EntitlementRequest;

    await updateEntitlementConfiguration(
      request,
      'policy-1',
      'annual',
      { name: 'Updated' },
      [
        { id: 'rule-1', policyId: 'policy-1', criterionType: 'SERVICE_MONTHS', operator: 'GREATER_THAN_OR_EQUAL', value: '12' },
        { id: 'rule-2', policyId: 'policy-1', criterionType: 'SERVICE_MONTHS', operator: 'LESS_THAN', value: '24' },
      ],
      [
        { id: 'rule-1', policyId: 'policy-1', criterionType: 'SERVICE_MONTHS', operator: 'GREATER_THAN_OR_EQUAL', value: '24' },
        { criterionType: 'JURISDICTION_CODE', operator: 'EQUALS', value: 'SG' },
      ],
    );

    expect(mock).toHaveBeenCalledWith(
      '/api/leave-entitlement-policies/policy-1',
      expect.objectContaining({ method: 'PUT' }),
    );
    expect(mock).toHaveBeenCalledWith(
      '/api/leave-entitlement-policy-eligibility-rules/rule-1',
      expect.objectContaining({ method: 'PUT' }),
    );
    expect(mock).toHaveBeenCalledWith(
      '/api/leave-entitlement-policy-eligibility-rules',
      expect.objectContaining({ method: 'POST' }),
    );
    expect(mock).toHaveBeenCalledWith(
      '/api/leave-entitlement-policy-eligibility-rules/rule-2',
      expect.objectContaining({ method: 'DELETE' }),
    );
  });
});

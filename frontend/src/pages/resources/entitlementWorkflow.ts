export interface EligibilityRuleValues {
  id?: string;
  policyId?: string;
  criterionType?: string;
  operator?: string;
  value?: string | number | string[];
  active?: boolean;
  sortOrder?: number;
}

export interface EntitlementPolicyValues {
  [key: string]: unknown;
}

export type EntitlementRequest = <T>(path: string, init?: RequestInit) => Promise<T>;

interface SavedRecord {
  id?: string;
}

const jsonRequest = (method: string, body?: unknown): RequestInit => ({
  method,
  ...(body === undefined ? {} : { body: JSON.stringify(body) }),
});

export const normaliseEligibilityRule = (
  rule: EligibilityRuleValues,
  policyId: string,
  index: number,
) => ({
  policyId,
  criterionType: rule.criterionType,
  operator: rule.operator,
  value: rule.value === undefined || rule.value === null ? '' : String(rule.value),
  active: rule.active !== false,
  sortOrder: rule.sortOrder ?? (index + 1) * 10,
});

const bestEffortDelete = async (request: EntitlementRequest, path: string) => {
  try {
    await request<void>(path, jsonRequest('DELETE'));
  } catch {
    // Rollback is best effort. The caller surfaces the partial-save warning.
  }
};

export class EntitlementPartialSaveError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'EntitlementPartialSaveError';
  }
}

export const createEntitlementConfiguration = async (
  request: EntitlementRequest,
  leaveTypeId: string,
  policyValues: EntitlementPolicyValues,
  eligibilityRules: EligibilityRuleValues[],
) => {
  const policy = await request<SavedRecord>('/api/leave-entitlement-policies', jsonRequest('POST', {
    ...policyValues,
    leaveTypeId,
  }));
  const policyId = String(policy.id ?? '').trim();
  if (!policyId) throw new Error('The entitlement policy was created without an ID.');

  const createdRuleIds: string[] = [];
  try {
    for (const [index, rule] of eligibilityRules.entries()) {
      const created = await request<SavedRecord>(
        '/api/leave-entitlement-policy-eligibility-rules',
        jsonRequest('POST', normaliseEligibilityRule(rule, policyId, index)),
      );
      const ruleId = String(created.id ?? '').trim();
      if (ruleId) createdRuleIds.push(ruleId);
    }
    return policyId;
  } catch {
    for (const ruleId of createdRuleIds.reverse()) {
      await bestEffortDelete(request, `/api/leave-entitlement-policy-eligibility-rules/${encodeURIComponent(ruleId)}`);
    }
    await bestEffortDelete(request, `/api/leave-entitlement-policies/${encodeURIComponent(policyId)}`);
    throw new EntitlementPartialSaveError(
      'The entitlement could not be saved completely. LeaveMaestro attempted to roll back the new policy and eligibility rules; please reload the leave type before retrying.',
    );
  }
};

export const updateEntitlementConfiguration = async (
  request: EntitlementRequest,
  policyId: string,
  leaveTypeId: string,
  policyValues: EntitlementPolicyValues,
  originalRules: EligibilityRuleValues[],
  eligibilityRules: EligibilityRuleValues[],
) => {
  await request<SavedRecord>(
    `/api/leave-entitlement-policies/${encodeURIComponent(policyId)}`,
    jsonRequest('PUT', { ...policyValues, leaveTypeId }),
  );

  const originalIds = new Set(originalRules.map((rule) => rule.id).filter((id): id is string => Boolean(id)));
  const submittedIds = new Set(eligibilityRules.map((rule) => rule.id).filter((id): id is string => Boolean(id)));

  try {
    for (const [index, rule] of eligibilityRules.entries()) {
      const payload = normaliseEligibilityRule(rule, policyId, index);
      if (rule.id && originalIds.has(rule.id)) {
        await request<SavedRecord>(
          `/api/leave-entitlement-policy-eligibility-rules/${encodeURIComponent(rule.id)}`,
          jsonRequest('PUT', payload),
        );
      } else {
        await request<SavedRecord>(
          '/api/leave-entitlement-policy-eligibility-rules',
          jsonRequest('POST', payload),
        );
      }
    }

    for (const rule of originalRules) {
      if (rule.id && !submittedIds.has(rule.id)) {
        await request<void>(
          `/api/leave-entitlement-policy-eligibility-rules/${encodeURIComponent(rule.id)}`,
          jsonRequest('DELETE'),
        );
      }
    }
  } catch {
    throw new EntitlementPartialSaveError(
      'The entitlement policy was updated, but the eligibility changes could not be saved completely. Reload the leave type before making further changes.',
    );
  }

  return policyId;
};

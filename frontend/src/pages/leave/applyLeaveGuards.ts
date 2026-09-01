import type { LeaveApplicationPolicyMetadata, LeavePolicyModel, LeaveTypeSummary } from '../../features/leave/leaveApi.ts';

const leavePolicyModels: ReadonlySet<LeavePolicyModel> = new Set([
  'ANNUAL_ENTITLEMENT',
  'CONDITIONAL_ANNUAL_ENTITLEMENT',
  'EVENT_BASED',
  'REQUEST_BASED',
]);

export const normalizeLeaveTypes = (value: unknown): LeaveTypeSummary[] => {
  if (!Array.isArray(value)) return [];
  return value.filter((item): item is LeaveTypeSummary => {
    if (!item || typeof item !== 'object') return false;
    const candidate = item as Partial<LeaveTypeSummary>;
    return typeof candidate.id === 'string' && candidate.id.length > 0
      && typeof candidate.name === 'string' && candidate.name.length > 0;
  });
};

export const normalizePolicyMetadata = (value: unknown): LeaveApplicationPolicyMetadata | undefined => {
  if (!value || typeof value !== 'object') return undefined;
  const candidate = value as Partial<LeaveApplicationPolicyMetadata>;
  if (typeof candidate.eventBased !== 'boolean' || typeof candidate.eventRequiresVerification !== 'boolean') {
    return undefined;
  }
  const policyModel = candidate.policyModel;
  if (policyModel !== undefined && policyModel !== null && !leavePolicyModels.has(policyModel)) {
    return undefined;
  }
  return {
    ...(policyModel !== undefined ? { policyModel } : {}),
    eventBased: candidate.eventBased,
    eventRequiresVerification: candidate.eventRequiresVerification,
  };
};

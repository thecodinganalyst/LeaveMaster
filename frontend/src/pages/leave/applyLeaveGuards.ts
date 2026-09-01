import type { LeaveApplicationPolicyMetadata, LeaveTypeSummary } from '../../features/leave/leaveApi.ts';

export const normalizeLeaveTypes = (value: unknown): LeaveTypeSummary[] => {
  if (!Array.isArray(value)) return [];
  return value.filter((item): item is LeaveTypeSummary => {
    if (!item || typeof item !== 'object') return false;
    const candidate = item as Partial<LeaveTypeSummary>;
    return typeof candidate.id === 'string' && typeof candidate.name === 'string';
  });
};

export const normalizePolicyMetadata = (value: unknown): LeaveApplicationPolicyMetadata | undefined => {
  if (!value || typeof value !== 'object') return undefined;
  const candidate = value as Partial<LeaveApplicationPolicyMetadata>;
  if (typeof candidate.eventBased !== 'boolean' || typeof candidate.eventRequiresVerification !== 'boolean') {
    return undefined;
  }
  if (typeof candidate.policyModel !== 'string') return undefined;
  return candidate as LeaveApplicationPolicyMetadata;
};

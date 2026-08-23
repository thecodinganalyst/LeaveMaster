export interface EntitlementPolicyOptionSource {
  id: string;
  name?: string | null;
  leaveTypeId?: string | null;
  entitlementAmount?: number | null;
  entitlementUnit?: string | null;
}

export interface LeaveTypeOptionSource {
  id: string;
  name: string;
}

const titleCaseUnit = (unit: string | null | undefined) => {
  if (!unit) return '';
  return unit.charAt(0) + unit.slice(1).toLowerCase();
};

export const formatEntitlementPolicyLabel = (
  policy: EntitlementPolicyOptionSource,
  leaveTypes: LeaveTypeOptionSource[],
) => {
  const policyName = policy.name?.trim() || 'Unnamed policy';
  const leaveTypeName = leaveTypes.find((leaveType) => leaveType.id === policy.leaveTypeId)?.name ?? 'Unknown leave type';
  const amount = policy.entitlementAmount ?? '—';
  const unit = titleCaseUnit(policy.entitlementUnit) || '—';
  return `${policyName} — ${leaveTypeName} · ${amount} ${unit}`;
};

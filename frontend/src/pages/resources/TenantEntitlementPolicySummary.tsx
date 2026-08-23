import { useQuery } from '@tanstack/react-query';

import { apiFetch } from '../../api/http.ts';
import {
  formatEntitlementPolicyLabel,
  type EntitlementPolicyOptionSource,
  type LeaveTypeOptionSource,
} from './entitlementPolicyPresentation.ts';

interface Props {
  policyId: string;
}

const loadPolicies = () => apiFetch<EntitlementPolicyOptionSource[]>('/api/leave-entitlement-policies');
const loadLeaveTypes = () => apiFetch<LeaveTypeOptionSource[]>('/api/leave-types');

export const TenantEntitlementPolicySummary = ({ policyId }: Props) => {
  const policiesQuery = useQuery({ queryKey: ['leave-entitlement-policies', 'options'], queryFn: loadPolicies, staleTime: 5 * 60 * 1000 });
  const leaveTypesQuery = useQuery({ queryKey: ['leave-types', 'tenant-options'], queryFn: loadLeaveTypes, staleTime: 5 * 60 * 1000 });

  if (policiesQuery.isLoading || leaveTypesQuery.isLoading) return <>Loading…</>;
  if (policiesQuery.isError || leaveTypesQuery.isError) return <>Unavailable</>;

  const policy = (policiesQuery.data ?? []).find((item) => item.id === policyId);
  if (!policy) return <>Unknown entitlement policy</>;
  return <>{formatEntitlementPolicyLabel(policy, leaveTypesQuery.data ?? [])}</>;
};

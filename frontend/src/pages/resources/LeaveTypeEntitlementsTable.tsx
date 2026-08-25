import { useQuery } from '@tanstack/react-query';
import { Alert, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { Link } from 'react-router-dom';

import { apiFetch } from '../../api/http.ts';

export interface LeaveEntitlementPolicySummary {
  id: string;
  leaveTypeId?: string | null;
  entitlementAmount?: number | null;
  entitlementUnit?: string | null;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  active?: boolean | null;
}

export interface EligibilityRuleSummary {
  id: string;
  policyId: string;
  criterionType?: string | null;
  operator?: string | null;
  value?: string | null;
  active?: boolean | null;
  sortOrder?: number | null;
}

interface Props {
  leaveTypeId: string;
  canEdit?: boolean;
}

const operatorLabels: Record<string, string> = {
  EQUALS: '=',
  NOT_EQUALS: '≠',
  IN: 'in',
  NOT_IN: 'not in',
  GREATER_THAN: '>',
  GREATER_THAN_OR_EQUAL: '≥',
  LESS_THAN: '<',
  LESS_THAN_OR_EQUAL: '≤',
};

const loadPolicies = () => apiFetch<LeaveEntitlementPolicySummary[]>('/api/leave-entitlement-policies');
const loadEligibilityRules = () => apiFetch<EligibilityRuleSummary[]>('/api/leave-entitlement-policy-eligibility-rules');

const titleCaseUnit = (unit: string | null | undefined, amount: number | null | undefined) => {
  if (!unit) return '—';
  const singular = unit.toUpperCase() === 'DAYS' ? 'Day' : unit.toUpperCase() === 'HOURS' ? 'Hour' : unit.charAt(0) + unit.slice(1).toLowerCase();
  return amount === 1 ? singular : `${singular}s`;
};

export const formatEntitlement = (policy: LeaveEntitlementPolicySummary) => {
  const amount = policy.entitlementAmount ?? '—';
  return `${amount} ${titleCaseUnit(policy.entitlementUnit, policy.entitlementAmount)}`;
};

const formatServiceValue = (rawValue: string) => {
  const months = Number(rawValue);
  if (!Number.isFinite(months)) return rawValue;
  if (months !== 0 && months % 12 === 0) {
    const years = months / 12;
    return `${years} ${years === 1 ? 'year' : 'years'}`;
  }
  return `${months} ${months === 1 ? 'month' : 'months'}`;
};

export const formatEligibilityRule = (rule: EligibilityRuleSummary) => {
  const operator = operatorLabels[rule.operator ?? ''] ?? String(rule.operator ?? '');
  const value = String(rule.value ?? '—');
  const criterion = rule.criterionType === 'SERVICE_MONTHS'
    ? 'Service'
    : rule.criterionType === 'JURISDICTION_CODE'
      ? 'Jurisdiction'
      : String(rule.criterionType ?? 'Eligibility');
  const displayValue = rule.criterionType === 'SERVICE_MONTHS' ? formatServiceValue(value) : value;
  const inactiveSuffix = rule.active === false ? ' (inactive)' : '';
  return `${criterion} ${operator} ${displayValue}${inactiveSuffix}`.trim();
};

export const formatEligibilitySummary = (rules: EligibilityRuleSummary[]) => {
  if (rules.length === 0) return 'All staff';
  return [...rules]
    .sort((left, right) => (left.sortOrder ?? 0) - (right.sortOrder ?? 0))
    .map(formatEligibilityRule)
    .join(' and ');
};

export const formatEffectivePeriod = (policy: LeaveEntitlementPolicySummary) => {
  const from = policy.effectiveFrom || 'No start date';
  const to = policy.effectiveTo || 'onward';
  return `${from} — ${to}`;
};

export const LeaveTypeEntitlementsTable = ({ leaveTypeId, canEdit = false }: Props) => {
  const policiesQuery = useQuery({
    queryKey: ['leave-entitlement-policies', 'leave-type-details', leaveTypeId],
    queryFn: loadPolicies,
  });
  const eligibilityQuery = useQuery({
    queryKey: ['leave-entitlement-policy-eligibility-rules', 'leave-type-details', leaveTypeId],
    queryFn: loadEligibilityRules,
  });

  if (policiesQuery.isError || eligibilityQuery.isError) {
    return <Alert type="error" showIcon message="Unable to load entitlements" />;
  }

  const rulesByPolicy = new Map<string, EligibilityRuleSummary[]>();
  for (const rule of eligibilityQuery.data ?? []) {
    const rules = rulesByPolicy.get(rule.policyId) ?? [];
    rules.push(rule);
    rulesByPolicy.set(rule.policyId, rules);
  }

  const rows = (policiesQuery.data ?? []).filter((policy) => policy.leaveTypeId === leaveTypeId);
  const columns: ColumnsType<LeaveEntitlementPolicySummary> = [
    {
      title: 'Entitlement',
      key: 'entitlement',
      render: (_, policy) => formatEntitlement(policy),
    },
    {
      title: 'Eligibility',
      key: 'eligibility',
      render: (_, policy) => formatEligibilitySummary(rulesByPolicy.get(policy.id) ?? []),
    },
    {
      title: 'Effective Period',
      key: 'effectivePeriod',
      render: (_, policy) => formatEffectivePeriod(policy),
    },
    {
      title: 'Status',
      key: 'status',
      render: (_, policy) => <Tag>{policy.active === false ? 'Inactive' : 'Active'}</Tag>,
    },
    ...(canEdit ? [{
      title: 'Actions',
      key: 'actions',
      render: (_: unknown, policy: LeaveEntitlementPolicySummary) => (
        <Link to={`/leave-types/${encodeURIComponent(leaveTypeId)}/entitlements/${encodeURIComponent(policy.id)}/edit`}>Edit</Link>
      ),
    }] : []),
  ];

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={rows}
      loading={policiesQuery.isLoading || eligibilityQuery.isLoading}
      pagination={false}
      locale={{ emptyText: 'No entitlements configured for this leave type' }}
    />
  );
};

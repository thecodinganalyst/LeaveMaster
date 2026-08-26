import { useCan } from '@refinedev/core';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, Descriptions, Empty, Space, Spin, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useRef, useState } from 'react';
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

export const shouldUseCompactEntitlementLayout = (containerWidth: number, tableWidth: number) => (
  containerWidth > 0 && tableWidth > containerWidth
);

export const LeaveTypeEntitlementsTable = ({ leaveTypeId, canEdit = false }: Props) => {
  const { data: canCreatePolicy } = useCan({ resource: 'leave-entitlement-policies', action: 'create' });
  const { data: canCreateRule } = useCan({ resource: 'leave-entitlement-policy-eligibility-rules', action: 'create' });
  const policiesQuery = useQuery({
    queryKey: ['leave-entitlement-policies', 'leave-type-details', leaveTypeId],
    queryFn: loadPolicies,
  });
  const eligibilityQuery = useQuery({
    queryKey: ['leave-entitlement-policy-eligibility-rules', 'leave-type-details', leaveTypeId],
    queryFn: loadEligibilityRules,
  });
  const containerRef = useRef<HTMLDivElement>(null);
  const tableRef = useRef<HTMLDivElement>(null);
  const measuredTableWidthRef = useRef(0);
  const [useCompactLayout, setUseCompactLayout] = useState(false);

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
  const loading = policiesQuery.isLoading || eligibilityQuery.isLoading;

  const updateResponsiveLayout = useCallback(() => {
    const container = containerRef.current;
    if (!container) return;

    if (!useCompactLayout) {
      const table = tableRef.current?.querySelector('table');
      const tableWidth = table instanceof HTMLElement ? table.scrollWidth : (tableRef.current?.scrollWidth ?? 0);
      if (tableWidth > 0) measuredTableWidthRef.current = tableWidth;
      setUseCompactLayout(shouldUseCompactEntitlementLayout(container.clientWidth, tableWidth));
      return;
    }

    const measuredTableWidth = measuredTableWidthRef.current;
    if (measuredTableWidth > 0 && container.clientWidth >= measuredTableWidth) {
      setUseCompactLayout(false);
    }
  }, [useCompactLayout]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return undefined;

    updateResponsiveLayout();
    if (typeof ResizeObserver === 'undefined') return undefined;

    const resizeObserver = new ResizeObserver(updateResponsiveLayout);
    resizeObserver.observe(container);
    return () => resizeObserver.disconnect();
  }, [updateResponsiveLayout, rows.length, canEdit, loading]);

  if (policiesQuery.isError || eligibilityQuery.isError) {
    return <Alert type="error" showIcon message="Unable to load entitlements" />;
  }

  return (
    <>
      {canCreatePolicy?.can && canCreateRule?.can ? (
        <Space style={{ width: '100%', justifyContent: 'flex-end', marginBottom: 12 }}>
          <Button type="primary">
            <Link to={`/leave-types/${encodeURIComponent(leaveTypeId)}/entitlements/create`}>Add Entitlement</Link>
          </Button>
        </Space>
      ) : null}
      <div ref={containerRef} data-testid="leave-type-entitlements-container" style={{ width: '100%', minWidth: 0 }}>
        {useCompactLayout ? (
          <Spin spinning={loading}>
            {rows.length === 0 && !loading ? (
              <Empty description="No entitlements configured for this leave type" />
            ) : (
              <Space direction="vertical" size="middle" style={{ width: '100%' }} data-testid="leave-type-entitlements-compact">
                {rows.map((policy) => (
                  <Card key={policy.id} size="small">
                    <Descriptions column={1} size="small" colon={false}>
                      <Descriptions.Item label="Entitlement">{formatEntitlement(policy)}</Descriptions.Item>
                      <Descriptions.Item label="Eligibility">{formatEligibilitySummary(rulesByPolicy.get(policy.id) ?? [])}</Descriptions.Item>
                      <Descriptions.Item label="Effective Period">{formatEffectivePeriod(policy)}</Descriptions.Item>
                      <Descriptions.Item label="Status"><Tag>{policy.active === false ? 'Inactive' : 'Active'}</Tag></Descriptions.Item>
                      {canEdit ? (
                        <Descriptions.Item label="Actions">
                          <Link to={`/leave-types/${encodeURIComponent(leaveTypeId)}/entitlements/${encodeURIComponent(policy.id)}/edit`}>Edit</Link>
                        </Descriptions.Item>
                      ) : null}
                    </Descriptions>
                  </Card>
                ))}
              </Space>
            )}
          </Spin>
        ) : (
          <div ref={tableRef} data-testid="leave-type-entitlements-table">
            <Table
              rowKey="id"
              columns={columns}
              dataSource={rows}
              loading={loading}
              pagination={false}
              locale={{ emptyText: 'No entitlements configured for this leave type' }}
            />
          </div>
        )}
      </div>
    </>
  );
};

import { Card, Space, Table, Tag, Typography, type TableColumnsType } from 'antd';

import type { StructuredResult } from './assistantApi.ts';

type PolicySummary = {
  policyName?: string;
  servicePeriod?: string;
  eligibility?: string | null;
  entitlement?: string;
  accrual?: string | null;
  proration?: string | null;
  carryForward?: string | null;
};

type LeaveTypeSummary = {
  leaveType?: string;
  accrual?: string | null;
  proration?: string | null;
  carryForward?: string | null;
  policies?: PolicySummary[];
};

const isRecord = (value: unknown): value is Record<string, unknown> =>
  Boolean(value) && typeof value === 'object' && !Array.isArray(value);

const stringOrNull = (value: unknown) => typeof value === 'string' ? value : value === null ? null : undefined;

const policySummary = (value: unknown): PolicySummary | undefined => {
  if (!isRecord(value)) return undefined;
  return {
    policyName: stringOrNull(value.policyName) ?? undefined,
    servicePeriod: stringOrNull(value.servicePeriod) ?? undefined,
    eligibility: stringOrNull(value.eligibility),
    entitlement: stringOrNull(value.entitlement) ?? undefined,
    accrual: stringOrNull(value.accrual),
    proration: stringOrNull(value.proration),
    carryForward: stringOrNull(value.carryForward),
  };
};

const leaveTypeSummary = (value: unknown): LeaveTypeSummary | undefined => {
  if (!isRecord(value)) return undefined;
  const policies = Array.isArray(value.policies)
    ? value.policies.map(policySummary).filter((item): item is PolicySummary => Boolean(item))
    : [];
  return {
    leaveType: stringOrNull(value.leaveType) ?? undefined,
    accrual: stringOrNull(value.accrual),
    proration: stringOrNull(value.proration),
    carryForward: stringOrNull(value.carryForward),
    policies,
  };
};

export const isEntitlementStructuredResult = (result: StructuredResult) =>
  result.toolName === 'getLeaveEntitlementConfigurationByJurisdiction';

const exceptionText = (policy: PolicySummary) =>
  [
    policy.accrual ? `Accrual: ${policy.accrual}` : null,
    policy.proration ? `Proration: ${policy.proration}` : null,
    policy.carryForward ? `Carry forward: ${policy.carryForward}` : null,
  ].filter(Boolean).join(' · ');

const columnsFor = (policies: PolicySummary[]): TableColumnsType<PolicySummary> => {
  const showEligibility = policies.some((policy) => Boolean(policy.eligibility));
  const showExceptions = policies.some((policy) => Boolean(exceptionText(policy)));
  const columns: TableColumnsType<PolicySummary> = [
    {
      title: 'Service period',
      dataIndex: 'servicePeriod',
      key: 'servicePeriod',
      width: 150,
      render: (value: string | undefined) => value || 'All service periods',
    },
    {
      title: 'Entitlement',
      dataIndex: 'entitlement',
      key: 'entitlement',
      width: 120,
      render: (value: string | undefined) => <Typography.Text strong>{value || 'Not configured'}</Typography.Text>,
    },
  ];
  if (showEligibility) {
    columns.push({
      title: 'Additional eligibility',
      dataIndex: 'eligibility',
      key: 'eligibility',
      render: (value: string | null | undefined) => value || '—',
    });
  }
  if (showExceptions) {
    columns.push({
      title: 'Policy exceptions',
      key: 'exceptions',
      render: (_, policy) => exceptionText(policy) || '—',
    });
  }
  return columns;
};

const CommonSetting = ({ label, value }: { label: string; value?: string | null }) =>
  value ? (
    <Space size={4}>
      <Typography.Text type="secondary">{label}:</Typography.Text>
      <Typography.Text>{value}</Typography.Text>
    </Space>
  ) : null;

export const EntitlementStructuredData = ({ result }: { result: StructuredResult }) => {
  const rawGroups = Array.isArray(result.data) ? result.data : [result.data];
  const groups = rawGroups.map(leaveTypeSummary).filter((item): item is LeaveTypeSummary => Boolean(item));

  return (
    <Card
      size="small"
      title="Leave entitlement details"
      extra={<Tag>Authoritative</Tag>}
      style={{ marginTop: 8 }}
      data-testid="entitlement-structured-data"
    >
      <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
        Configured LeaveMaster data. The table focuses on values that vary by service tier.
      </Typography.Paragraph>
      {groups.length === 0 ? <Typography.Text>No entitlement policies found.</Typography.Text> : null}
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        {groups.map((group, groupIndex) => {
          const policies = group.policies ?? [];
          return (
            <Card key={`${group.leaveType ?? 'leave-type'}-${groupIndex}`} size="small" type="inner" title={group.leaveType ?? 'Leave type'}>
              <Space direction="vertical" size="small" style={{ width: '100%' }}>
                <Space wrap size={[16, 4]}>
                  <CommonSetting label="Accrual" value={group.accrual} />
                  <CommonSetting label="Proration" value={group.proration} />
                  <CommonSetting label="Carry forward" value={group.carryForward} />
                </Space>
                <Table<PolicySummary>
                  size="small"
                  pagination={false}
                  columns={columnsFor(policies)}
                  dataSource={policies}
                  rowKey={(policy, index) => `${policy.policyName ?? 'policy'}-${index ?? 0}`}
                  scroll={{ x: 'max-content' }}
                  locale={{ emptyText: 'No policy tiers configured.' }}
                />
              </Space>
            </Card>
          );
        })}
      </Space>
    </Card>
  );
};

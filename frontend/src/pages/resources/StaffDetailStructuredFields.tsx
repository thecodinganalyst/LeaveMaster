import { Space, Table, Tag, Typography } from 'antd';

import { STAFF_SCHEDULE_DAYS, type DaySchedule, type WorkScheduleDayValue } from './staffFormHelpers.ts';

type StructuredValue = unknown[] | Record<string, unknown> | null | undefined;

interface LeaveEntitlementValue {
  leaveType?: { name?: string } | null;
  from?: string | null;
  to?: string | null;
  entitlement?: number | null;
  baseEntitlementAmount?: number | null;
  carriedForwardAmount?: number | null;
  adjustmentAmount?: number | null;
}

const parseStructuredArray = (value: unknown): unknown[] => {
  if (Array.isArray(value)) return value;
  if (typeof value !== 'string' || !value.trim()) return [];
  try {
    const parsed: StructuredValue = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const scheduleLabel: Record<DaySchedule, string> = {
  FULL: 'Full day',
  AM: 'AM',
  PM: 'PM',
};

export const StaffWorkScheduleField = ({ value }: { value: unknown }) => {
  const schedule = parseStructuredArray(value).filter((entry): entry is WorkScheduleDayValue => {
    if (!entry || typeof entry !== 'object') return false;
    const candidate = entry as Partial<WorkScheduleDayValue>;
    return STAFF_SCHEDULE_DAYS.some((day) => day.value === candidate.dayOfWeek)
      && ['FULL', 'AM', 'PM'].includes(String(candidate.daySchedule));
  });

  if (schedule.length === 0) return <Typography.Text type="secondary">No work schedule configured.</Typography.Text>;

  const byDay = new Map(schedule.map((entry) => [entry.dayOfWeek, entry.daySchedule]));
  const rows = STAFF_SCHEDULE_DAYS.map((day) => ({
    key: day.value,
    day: day.label,
    schedule: byDay.get(day.value),
  }));

  return (
    <Table
      size="small"
      pagination={false}
      dataSource={rows}
      rowKey="key"
      tableLayout="fixed"
      columns={[
        { title: 'Day', dataIndex: 'day', key: 'day', width: '45%' },
        {
          title: 'Schedule',
          dataIndex: 'schedule',
          key: 'schedule',
          render: (daySchedule: DaySchedule | undefined) => daySchedule
            ? <Tag>{scheduleLabel[daySchedule]}</Tag>
            : <Typography.Text type="secondary">Not working</Typography.Text>,
        },
      ]}
    />
  );
};

const displayNumber = (value: number | null | undefined) => value == null ? '—' : String(value);

const entitlementBreakdown = (entitlement: LeaveEntitlementValue) => {
  const parts = [
    entitlement.baseEntitlementAmount == null ? null : `Base ${entitlement.baseEntitlementAmount}`,
    entitlement.carriedForwardAmount == null ? null : `Carry forward ${entitlement.carriedForwardAmount}`,
    entitlement.adjustmentAmount == null ? null : `Adjustment ${entitlement.adjustmentAmount}`,
  ].filter(Boolean);
  return parts.length > 0 ? parts.join(' · ') : '—';
};

const entitlementPeriod = (entitlement: LeaveEntitlementValue) => entitlement.from || entitlement.to
  ? `${entitlement.from ?? '—'} to ${entitlement.to ?? '—'}`
  : 'Period not specified';

export const StaffLeaveEntitlementsField = ({ value }: { value: unknown }) => {
  const entitlements = parseStructuredArray(value).filter((entry): entry is LeaveEntitlementValue => Boolean(entry && typeof entry === 'object'));

  if (entitlements.length === 0) return <Typography.Text type="secondary">No leave entitlements configured.</Typography.Text>;

  const grouped = new Map<string, Array<LeaveEntitlementValue & { key: string }>>();
  entitlements.forEach((entitlement, index) => {
    const period = entitlementPeriod(entitlement);
    const rows = grouped.get(period) ?? [];
    rows.push({
      ...entitlement,
      key: `${entitlement.leaveType?.name ?? 'entitlement'}-${entitlement.from ?? index}-${index}`,
    });
    grouped.set(period, rows);
  });

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      {[...grouped.entries()].map(([period, rows]) => (
        <div key={period}>
          <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
            {period}
          </Typography.Text>
          <Table
            size="small"
            pagination={false}
            dataSource={rows}
            rowKey="key"
            tableLayout="fixed"
            columns={[
              {
                title: 'Leave type',
                key: 'leaveType',
                width: '32%',
                render: (_, entitlement) => entitlement.leaveType?.name || '—',
              },
              {
                title: 'Entitlement',
                dataIndex: 'entitlement',
                key: 'entitlement',
                width: '22%',
                render: displayNumber,
              },
              {
                title: 'Breakdown',
                key: 'breakdown',
                render: (_, entitlement) => (
                  <span style={{ overflowWrap: 'anywhere' }}>{entitlementBreakdown(entitlement)}</span>
                ),
              },
            ]}
          />
        </div>
      ))}
    </Space>
  );
};

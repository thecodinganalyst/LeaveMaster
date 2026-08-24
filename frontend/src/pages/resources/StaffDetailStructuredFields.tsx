import { Table, Tag, Typography } from 'antd';

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
      scroll={{ x: 360 }}
      columns={[
        { title: 'Day', dataIndex: 'day', key: 'day' },
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

export const StaffLeaveEntitlementsField = ({ value }: { value: unknown }) => {
  const entitlements = parseStructuredArray(value).filter((entry): entry is LeaveEntitlementValue => Boolean(entry && typeof entry === 'object'));

  if (entitlements.length === 0) return <Typography.Text type="secondary">No leave entitlements configured.</Typography.Text>;

  const rows = entitlements.map((entitlement, index) => ({
    ...entitlement,
    key: `${entitlement.leaveType?.name ?? 'entitlement'}-${entitlement.from ?? index}-${index}`,
  }));

  return (
    <Table
      size="small"
      pagination={false}
      dataSource={rows}
      rowKey="key"
      scroll={{ x: 720 }}
      columns={[
        {
          title: 'Leave type',
          key: 'leaveType',
          render: (_, entitlement) => entitlement.leaveType?.name || '—',
        },
        {
          title: 'Period',
          key: 'period',
          render: (_, entitlement) => entitlement.from || entitlement.to
            ? `${entitlement.from ?? '—'} to ${entitlement.to ?? '—'}`
            : '—',
        },
        {
          title: 'Entitlement',
          dataIndex: 'entitlement',
          key: 'entitlement',
          render: displayNumber,
        },
        {
          title: 'Breakdown',
          key: 'breakdown',
          render: (_, entitlement) => entitlementBreakdown(entitlement),
        },
      ]}
    />
  );
};

import { useQuery } from '@tanstack/react-query';
import { Alert, Card, Col, Form, Input, InputNumber, Row, Select, Space, Spin, Steps, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';

import { apiFetch } from '../../api/http.ts';
import { getJurisdictionOptions, type JurisdictionOptionSource } from './jurisdictions.ts';
import {
  calendarJurisdictionIds,
  scheduleForDay,
  STAFF_SCHEDULE_DAYS,
  updateWorkSchedule,
  type DaySchedule,
  type LeaveCalendarJurisdictionSource,
  type WorkScheduleDayValue,
} from './staffFormHelpers.ts';
import { StaffDependantsField } from './StaffDependantsField.tsx';
import { StaffEmploymentTypeField } from './StaffEmploymentTypeField.tsx';
import { StaffLeaveApproversField } from './StaffLeaveApproversField.tsx';
import { StaffRoleSelect } from './StaffRoleSelect.tsx';

interface LeaveTypeSource { id: string; name: string; }
interface LeaveEntitlementValue {
  id?: string | null;
  leaveType?: LeaveTypeSource;
  leaveTypeId?: string | null;
  from?: string;
  to?: string;
  entitlement?: number;
  policyId?: string | null;
  baseEntitlementAmount?: number | null;
  carriedForwardAmount?: number | null;
  adjustmentAmount?: number | null;
}
type ProposalStatus = 'AVAILABLE' | 'NO_TEMPLATE' | 'NOT_ELIGIBLE_IN_PERIOD';
interface ProposalAnalysis { proposals: LeaveEntitlementValue[]; status: ProposalStatus; }
interface Props { step: 0 | 1; }

const loadCalendars = () => apiFetch<LeaveCalendarJurisdictionSource[]>('/api/leave-calendars');
const loadJurisdictions = () => apiFetch<JurisdictionOptionSource[]>('/api/jurisdictions');

export const WorkScheduleEditor = () => {
  const form = Form.useFormInstance();
  const schedule = Form.useWatch('workSchedule', { form, preserve: true }) as WorkScheduleDayValue[] | undefined;
  const options = [
    { label: 'Not working', value: 'NONE' },
    { label: 'Full day', value: 'FULL' },
    { label: 'AM', value: 'AM' },
    { label: 'PM', value: 'PM' },
  ];
  return (
    <Card size="small" title="Work schedule" style={{ marginBottom: 24 }}>
      <Space direction="vertical" size={8} style={{ width: '100%' }}>
        {STAFF_SCHEDULE_DAYS.map((day) => (
          <div key={day.value} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16, minHeight: 40 }}>
            <Typography.Text>{day.label}</Typography.Text>
            <Select
              aria-label={`${day.label} work schedule`}
              style={{ width: 180, maxWidth: '55%' }}
              value={scheduleForDay(schedule, day.value)}
              options={options}
              onChange={(value: DaySchedule | 'NONE') => form.setFieldValue('workSchedule', updateWorkSchedule(schedule, day.value, value))}
            />
          </div>
        ))}
      </Space>
    </Card>
  );
};

export const StaffCreationFields = ({ step }: Props) => {
  const form = Form.useFormInstance();
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<ProposalStatus>();
  const [error, setError] = useState<string>();
  const jurisdictionId = Form.useWatch('jurisdictionId', form) as string | undefined;

  const calendarsQuery = useQuery({ queryKey: ['staff-form', 'leave-calendars'], queryFn: loadCalendars, staleTime: 60_000 });
  const jurisdictionsQuery = useQuery({ queryKey: ['jurisdictions', 'options'], queryFn: loadJurisdictions, staleTime: 5 * 60_000 });
  const eligibleJurisdictionIds = useMemo(() => calendarJurisdictionIds(calendarsQuery.data ?? []), [calendarsQuery.data]);
  const jurisdictionOptions = useMemo(
    () => getJurisdictionOptions(jurisdictionsQuery.data ?? []).filter((option) => eligibleJurisdictionIds.has(option.value)),
    [eligibleJurisdictionIds, jurisdictionsQuery.data],
  );

  useEffect(() => {
    const onlyJurisdiction = jurisdictionOptions.length === 1 ? jurisdictionOptions[0] : undefined;
    if (jurisdictionId || !onlyJurisdiction) return;
    form.setFieldValue('jurisdictionId', onlyJurisdiction.value);
  }, [form, jurisdictionId, jurisdictionOptions]);

  useEffect(() => {
    if (step !== 1) return;
    const values = form.getFieldsValue(true) as Record<string, unknown>;
    const nextJurisdictionId = values.jurisdictionId as string | undefined;
    const joinDate = values.joinDate as string | undefined;
    if (!nextJurisdictionId || !joinDate) return;
    let cancelled = false;
    setLoading(true);
    setStatus(undefined);
    setError(undefined);
    form.setFieldValue('leaveEntitlements', []);
    void apiFetch<ProposalAnalysis>('/api/staff/entitlement-proposals/analysis', {
      method: 'POST',
      body: JSON.stringify({
        jurisdictionId: nextJurisdictionId,
        joinDate,
        ...(values.termDate ? { termDate: values.termDate } : {}),
        ...(values.employmentType ? { employmentType: values.employmentType } : {}),
        dependants: values.dependants ?? [],
      }),
    }).then((analysis) => {
      if (cancelled) return;
      form.setFieldValue('leaveEntitlements', analysis.proposals.map((proposal) => ({
        ...proposal,
        leaveTypeId: proposal.leaveType?.id,
        baseEntitlementAmount: proposal.baseEntitlementAmount ?? proposal.entitlement ?? 0,
        adjustmentAmount: 0,
      })));
      setStatus(analysis.status);
    }).catch((cause: unknown) => {
      if (cancelled) return;
      setError(cause instanceof Error ? cause.message : 'Unable to generate leave entitlements');
    }).finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [form, step]);

  const entitlements = Form.useWatch('leaveEntitlements', { form, preserve: true }) as LeaveEntitlementValue[] | undefined;
  const noJurisdictions = !calendarsQuery.isLoading && !jurisdictionsQuery.isLoading && jurisdictionOptions.length === 0;

  return (
    <>
      <Steps current={step} items={[{ title: 'Staff Details' }, { title: 'Review Leave Entitlements' }]} style={{ marginBottom: 24 }} />
      {step === 0 ? (
        <>
          <Form.Item name="id" label="Staff ID" rules={[{ required: true, message: 'Staff ID is required' }]}><Input /></Form.Item>
          <Form.Item name="name" label="Name" rules={[{ required: true, message: 'Name is required' }]}><Input /></Form.Item>
          <Form.Item name="email" label="Email" rules={[{ type: 'email', message: 'Enter a valid email address' }]}><Input type="email" /></Form.Item>
          <Form.Item name="joinDate" label="Join date" rules={[{ required: true, message: 'Join date is required' }]}><Input type="date" allowClear /></Form.Item>
          <Form.Item name="termDate" label="Termination date"><Input type="date" allowClear /></Form.Item>
          <Form.Item name="loginName" label="Login name"><Input /></Form.Item>
          {noJurisdictions ? <Alert type="warning" showIcon message="No leave-calendar jurisdiction is configured for this tenant" style={{ marginBottom: 16 }} /> : null}
          <Form.Item name="jurisdictionId" label="Jurisdiction" rules={[{ required: true, message: 'Jurisdiction is required' }]}>
            <Select options={jurisdictionOptions} loading={calendarsQuery.isLoading || jurisdictionsQuery.isLoading} disabled={noJurisdictions} showSearch optionFilterProp="label" />
          </Form.Item>
          <WorkScheduleEditor />
          <StaffEmploymentTypeField />
          <StaffRoleSelect />
          <StaffDependantsField />
          <StaffLeaveApproversField />
        </>
      ) : (
        <Card size="small" title="Generated leave entitlements" style={{ marginBottom: 24 }}>
          <Typography.Paragraph type="secondary">
            Review the policy-generated entitlements and adjust the final amount if required. Nothing is saved until you confirm staff creation.
          </Typography.Paragraph>
          {loading ? <Space><Spin size="small" /> Generating entitlements…</Space> : null}
          {error ? <Alert type="error" showIcon message={error} /> : null}
          {!loading && !error && status === 'NO_TEMPLATE' ? <Alert type="info" showIcon message="No entitlement policy templates are configured for this staff member" /> : null}
          {!loading && !error && status === 'NOT_ELIGIBLE_IN_PERIOD' ? <Alert type="info" showIcon message="This staff member is not eligible for an automatic entitlement in the current period" /> : null}
          {!loading && !error && (entitlements ?? []).map((entitlement, index) => {
            const generated = entitlement.baseEntitlementAmount ?? entitlement.entitlement ?? 0;
            const finalAmount = entitlement.entitlement ?? 0;
            const adjusted = Math.abs(finalAmount - generated) > 0.0001;
            return (
              <Card key={`${entitlement.leaveType?.id ?? 'leave'}-${index}`} size="small" style={{ marginTop: 12 }}>
                <Row gutter={12} align="middle">
                  <Col xs={24} md={7}>
                    <Typography.Text strong>{entitlement.leaveType?.name ?? entitlement.leaveType?.id ?? 'Leave entitlement'}</Typography.Text>
                    <br />
                    <Typography.Text type="secondary">{entitlement.from ?? '—'} to {entitlement.to ?? '—'}</Typography.Text>
                  </Col>
                  <Col xs={12} md={7}>
                    <Typography.Text type="secondary">Calculated entitlement</Typography.Text><br />
                    <Typography.Text strong>{generated} days</Typography.Text>
                  </Col>
                  <Col xs={12} md={7}>
                    <Form.Item
                      name={['leaveEntitlements', index, 'entitlement']}
                      label="Final entitlement"
                      style={{ marginBottom: 0 }}
                      rules={[
                        { required: true, message: 'Final entitlement is required' },
                        { validator: (_, value?: number) => value == null || (value >= 0 && Number.isInteger(value * 2))
                          ? Promise.resolve()
                          : Promise.reject(new Error('Use 0.5-day increments')) },
                      ]}
                    >
                      <InputNumber min={0} step={0.5} precision={1} addonAfter="days" style={{ width: '100%' }} />
                    </Form.Item>
                  </Col>
                </Row>
                {adjusted ? (
                  <Typography.Text type="warning">Adjusted from {generated} days generated by policy</Typography.Text>
                ) : null}
              </Card>
            );
          })}
        </Card>
      )}
    </>
  );
};

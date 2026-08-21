import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, Col, Form, Input, InputNumber, Row, Select, Space, Spin, Typography } from 'antd';
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

interface LeaveTypeSource {
  id: string;
  name: string;
}

interface LeaveEntitlementValue {
  id?: string | null;
  leaveType?: LeaveTypeSource;
  from?: string;
  to?: string;
  entitlement?: number;
  policyId?: string | null;
  baseEntitlementAmount?: number | null;
  carriedForwardAmount?: number | null;
  adjustmentAmount?: number | null;
}

interface ProposalRequest {
  staffId?: string;
  jurisdictionId: string;
  joinDate: string;
  termDate?: string;
}

interface Props {
  editing?: boolean;
  staffId?: string;
}

const SCHEDULE_OPTIONS = [
  { label: 'Not working', value: 'NONE' },
  { label: 'Full day', value: 'FULL' },
  { label: 'AM', value: 'AM' },
  { label: 'PM', value: 'PM' },
];

const loadCalendars = () => apiFetch<LeaveCalendarJurisdictionSource[]>('/api/leave-calendars');
const loadJurisdictions = () => apiFetch<JurisdictionOptionSource[]>('/api/jurisdictions');
const loadLeaveTypes = () => apiFetch<LeaveTypeSource[]>('/api/leave-types');

const StructuredValue = () => null;

const WorkScheduleEditor = () => {
  const form = Form.useFormInstance();
  const schedule = Form.useWatch('workSchedule', form) as WorkScheduleDayValue[] | undefined;

  return (
    <Card size="small" title="Work schedule" style={{ marginBottom: 24 }}>
      <Typography.Paragraph type="secondary">
        Choose the employee's normal schedule for each day. Not working days are excluded from the submitted work schedule.
      </Typography.Paragraph>
      <Form.Item name="workSchedule" noStyle><StructuredValue /></Form.Item>
      <Row gutter={[16, 12]}>
        {STAFF_SCHEDULE_DAYS.map((day) => (
          <Col xs={24} sm={12} md={8} key={day.value}>
            <Typography.Text>{day.label}</Typography.Text>
            <Select
              aria-label={`${day.label} work schedule`}
              style={{ width: '100%', marginTop: 4 }}
              value={scheduleForDay(schedule, day.value)}
              options={SCHEDULE_OPTIONS}
              onChange={(value: DaySchedule | 'NONE') => {
                form.setFieldValue('workSchedule', updateWorkSchedule(schedule, day.value, value));
              }}
            />
          </Col>
        ))}
      </Row>
    </Card>
  );
};

const StaffJurisdictionSelect = ({
  value,
  onChange,
  options,
  loading,
  disabled,
  onSelected,
}: {
  value?: string;
  onChange?: (value: string) => void;
  options: Array<{ label: string; value: string }>;
  loading: boolean;
  disabled: boolean;
  onSelected: () => void;
}) => (
  <Select<string>
    value={value ?? null}
    onChange={(next) => {
      onChange?.(next);
      onSelected();
    }}
    options={options}
    loading={loading}
    disabled={disabled}
    showSearch
    optionFilterProp="label"
    placeholder={options.length === 0 && !loading ? 'No configured leave-calendar jurisdictions' : 'Select a jurisdiction'}
  />
);

export const StaffFormFields = ({ editing = false, staffId }: Props) => {
  const form = Form.useFormInstance();
  const [proposalEnabled, setProposalEnabled] = useState(!editing);
  const [proposalLoading, setProposalLoading] = useState(false);
  const [proposalError, setProposalError] = useState<string>();
  const jurisdictionId = Form.useWatch('jurisdictionId', form) as string | undefined;
  const joinDate = Form.useWatch('joinDate', form) as string | undefined;
  const termDate = Form.useWatch('termDate', form) as string | undefined;

  const calendarsQuery = useQuery({ queryKey: ['staff-form', 'leave-calendars'], queryFn: loadCalendars, staleTime: 60_000 });
  const jurisdictionsQuery = useQuery({ queryKey: ['jurisdictions', 'options'], queryFn: loadJurisdictions, staleTime: 5 * 60_000 });
  const leaveTypesQuery = useQuery({ queryKey: ['staff-form', 'leave-types'], queryFn: loadLeaveTypes, staleTime: 60_000 });

  const eligibleJurisdictionIds = useMemo(
    () => calendarJurisdictionIds(calendarsQuery.data ?? []),
    [calendarsQuery.data],
  );
  const jurisdictionOptions = useMemo(
    () => getJurisdictionOptions(jurisdictionsQuery.data ?? []).filter((option) => eligibleJurisdictionIds.has(option.value)),
    [eligibleJurisdictionIds, jurisdictionsQuery.data],
  );
  const leaveTypeOptions = useMemo(
    () => (leaveTypesQuery.data ?? []).map((leaveType) => ({ label: leaveType.name, value: leaveType.id })),
    [leaveTypesQuery.data],
  );

  useEffect(() => {
    if (!proposalEnabled || !jurisdictionId || !joinDate) return;
    let cancelled = false;
    setProposalLoading(true);
    setProposalError(undefined);
    const request: ProposalRequest = {
      ...(editing && staffId ? { staffId } : {}),
      jurisdictionId,
      joinDate,
      ...(termDate ? { termDate } : {}),
    };
    void apiFetch<LeaveEntitlementValue[]>('/api/staff/entitlement-proposals', {
      method: 'POST',
      body: JSON.stringify(request),
    }).then((proposals) => {
      if (!cancelled) form.setFieldValue('leaveEntitlements', proposals);
    }).catch((error: unknown) => {
      if (!cancelled) {
        setProposalError(error instanceof Error ? error.message : 'Unable to generate leave entitlements');
        form.setFieldValue('leaveEntitlements', []);
      }
    }).finally(() => {
      if (!cancelled) setProposalLoading(false);
    });
    return () => { cancelled = true; };
  }, [editing, form, joinDate, jurisdictionId, proposalEnabled, staffId, termDate]);

  const noJurisdictions = !calendarsQuery.isLoading && !jurisdictionsQuery.isLoading && jurisdictionOptions.length === 0;

  return (
    <>
      <Form.Item name="id" label="Staff ID" rules={[{ required: true, message: 'Staff ID is required' }]}>
        <Input disabled={editing} />
      </Form.Item>
      <Form.Item name="name" label="Name" rules={[{ required: true, message: 'Name is required' }]}><Input /></Form.Item>
      <Form.Item name="email" label="Email"><Input type="email" /></Form.Item>
      <Form.Item name="joinDate" label="Join date" rules={[{ required: true, message: 'Join date is required' }]}><Input type="date" /></Form.Item>
      <Form.Item name="termDate" label="Termination date"><Input type="date" /></Form.Item>
      <Form.Item name="loginName" label="Login name"><Input /></Form.Item>

      {noJurisdictions && (
        <Alert
          type="warning"
          showIcon
          message="No leave-calendar jurisdiction is configured for this tenant"
          description="Create a tenant leave calendar before creating or moving staff into a jurisdiction."
          style={{ marginBottom: 16 }}
        />
      )}
      <Form.Item name="jurisdictionId" label="Jurisdiction" rules={[{ required: true, message: 'Jurisdiction is required' }]}>
        <StaffJurisdictionSelect
          options={jurisdictionOptions}
          loading={calendarsQuery.isLoading || jurisdictionsQuery.isLoading}
          disabled={calendarsQuery.isError || jurisdictionsQuery.isError || noJurisdictions}
          onSelected={() => setProposalEnabled(true)}
        />
      </Form.Item>

      <WorkScheduleEditor />

      <Card size="small" title="Leave entitlements" style={{ marginBottom: 24 }}>
        <Typography.Paragraph type="secondary">
          Entitlements are generated from the policies matching the selected jurisdiction. HR can adjust the leave type, dates and amount before saving.
        </Typography.Paragraph>
        {proposalLoading && <Space style={{ marginBottom: 12 }}><Spin size="small" /> Generating entitlements…</Space>}
        {proposalError && <Alert type="error" showIcon message={proposalError} style={{ marginBottom: 12 }} />}
        <Form.List name="leaveEntitlements">
          {(fields, { add, remove }) => (
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              {fields.map((field) => (
                <Card key={field.key} size="small">
                  <Row gutter={12}>
                    <Col xs={24} md={7}>
                      <Form.Item
                        {...field}
                        name={[field.name, 'leaveType', 'id']}
                        label="Leave type"
                        rules={[{ required: true, message: 'Leave type is required' }]}
                      >
                        <Select options={leaveTypeOptions} loading={leaveTypesQuery.isLoading} showSearch optionFilterProp="label" />
                      </Form.Item>
                    </Col>
                    <Col xs={12} md={5}>
                      <Form.Item {...field} name={[field.name, 'from']} label="From" rules={[{ required: true }]}>
                        <Input type="date" />
                      </Form.Item>
                    </Col>
                    <Col xs={12} md={5}>
                      <Form.Item {...field} name={[field.name, 'to']} label="To" rules={[{ required: true }]}>
                        <Input type="date" />
                      </Form.Item>
                    </Col>
                    <Col xs={18} md={5}>
                      <Form.Item {...field} name={[field.name, 'entitlement']} label="Amount" rules={[{ required: true }]}>
                        <InputNumber min={0} step={0.5} style={{ width: '100%' }} />
                      </Form.Item>
                    </Col>
                    <Col xs={6} md={2} style={{ display: 'flex', alignItems: 'center' }}>
                      <Button danger type="link" onClick={() => remove(field.name)}>Remove</Button>
                    </Col>
                  </Row>
                  <Form.Item {...field} name={[field.name, 'policyId']} label="Source policy">
                    <Input readOnly placeholder="Manual entitlement" />
                  </Form.Item>
                  <Form.Item {...field} name={[field.name, 'id']} hidden><Input /></Form.Item>
                  <Form.Item {...field} name={[field.name, 'baseEntitlementAmount']} hidden><InputNumber /></Form.Item>
                  <Form.Item {...field} name={[field.name, 'carriedForwardAmount']} hidden><InputNumber /></Form.Item>
                  <Form.Item {...field} name={[field.name, 'adjustmentAmount']} hidden><InputNumber /></Form.Item>
                </Card>
              ))}
              <Button type="dashed" onClick={() => add({ adjustmentAmount: 0, carriedForwardAmount: 0 })}>Add entitlement</Button>
            </Space>
          )}
        </Form.List>
      </Card>
    </>
  );
};

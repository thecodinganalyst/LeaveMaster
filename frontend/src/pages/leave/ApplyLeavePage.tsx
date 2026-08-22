import { useCan } from '@refinedev/core';
import { App, Alert, Button, Card, Divider, Form, Input, Select, Space, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { getCurrentUser } from '../../auth/session.ts';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { applyForLeave, getLeaveTypes, type LeaveDuration, type LeaveTypeSummary } from '../../features/leave/leaveApi.ts';

interface FormValues {
  leaveTypeId: string;
  fromDate: string;
  toDate: string;
  leaveDuration: LeaveDuration;
  eventDate?: string;
  eventStartDate?: string;
  eventEndDate?: string;
  eventExternalReference?: string;
}

export const ApplyLeavePage = () => {
  const [staffId, setStaffId] = useState<string | null>(null);
  const [leaveTypes, setLeaveTypes] = useState<LeaveTypeSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string>();
  const [form] = Form.useForm<FormValues>();
  const navigate = useNavigate();
  const { message } = App.useApp();
  const { data: canWrite } = useCan({ resource: 'leave-requests', action: 'create' });

  useEffect(() => {
    void (async () => {
      try {
        const user = await getCurrentUser();
        setStaffId(user.staffId);
        if (user.authorities.includes('LEAVE_APPLICATION_WRITE')) {
          setLeaveTypes(await getLeaveTypes());
        }
      } catch (cause) {
        setError(cause instanceof Error ? cause.message : 'Unable to initialize leave application.');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const submit = async (values: FormValues) => {
    if (!staffId || !canWrite?.can) return;
    setSubmitting(true);
    setError(undefined);
    try {
      const applications = await applyForLeave({
        staffId,
        fromDate: values.fromDate,
        toDate: values.toDate,
        leaveTypeId: values.leaveTypeId,
        leaveDuration: values.leaveDuration,
        status: 'PENDING',
        ...(values.eventDate ? { eventDate: values.eventDate } : {}),
        ...(values.eventStartDate ? { eventStartDate: values.eventStartDate } : {}),
        ...(values.eventEndDate ? { eventEndDate: values.eventEndDate } : {}),
        ...(values.eventExternalReference ? { eventExternalReference: values.eventExternalReference } : {}),
      });
      const awaitingVerification = applications.some((application) => application.status === 'PENDING_VERIFICATION');
      message.success(awaitingVerification
        ? 'Leave request recorded and is awaiting qualifying-event verification.'
        : `Leave request submitted for ${applications.length} working day${applications.length === 1 ? '' : 's'}.`);
      navigate('/leave-requests');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to submit leave request.');
    } finally {
      setSubmitting(false);
    }
  };

  if (canWrite && !canWrite.can) {
    return <PageContainer><Alert type="warning" showIcon message="You do not have permission to submit leave applications." /></PageContainer>;
  }

  return (
    <PageContainer>
      <PageHeader title="Apply for leave" subtitle="Submit a leave request for one or more working days." />
      {error ? <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} /> : null}
      {!staffId && !loading ? <Alert type="warning" showIcon message="Your account is not linked to a staff record, so leave cannot be submitted." style={{ marginBottom: 16 }} /> : null}
      <Card loading={loading}>
        <Form form={form} layout="vertical" onFinish={submit} initialValues={{ leaveDuration: 'FULL' }} style={{ maxWidth: 640 }}>
          <Form.Item name="leaveTypeId" label="Leave type" rules={[{ required: true, message: 'Select a leave type' }]}>
            <Select options={leaveTypes.map((leaveType) => ({ value: leaveType.id, label: leaveType.name }))} placeholder="Select leave type" />
          </Form.Item>
          <Form.Item name="fromDate" label="From date" rules={[{ required: true, message: 'Select a start date' }]}>
            <Input type="date" />
          </Form.Item>
          <Form.Item
            name="toDate"
            label="To date"
            dependencies={['fromDate']}
            rules={[
              { required: true, message: 'Select an end date' },
              ({ getFieldValue }) => ({
                validator: (_, value: string | undefined) => {
                  const fromDate = getFieldValue('fromDate') as string | undefined;
                  return fromDate && value && value < fromDate
                    ? Promise.reject(new Error('End date must be on or after start date'))
                    : Promise.resolve();
                },
              }),
            ]}
          >
            <Input type="date" />
          </Form.Item>
          <Form.Item name="leaveDuration" label="Duration" rules={[{ required: true }]}>
            <Select options={[
              { value: 'FULL', label: 'Full day' },
              { value: 'AM', label: 'Morning half day' },
              { value: 'PM', label: 'Afternoon half day' },
            ]} />
          </Form.Item>

          <Divider orientation="left">Qualifying event</Divider>
          <Alert
            type="info"
            showIcon
            message="Only needed for event-based leave"
            description="For leave such as an NS/reservist call-up or parental event, enter the event details here. You do not need to create a separate event first. For ordinary annual or sick leave, leave these fields blank."
            style={{ marginBottom: 16 }}
          />
          <Form.Item name="eventDate" label="Event date">
            <Input type="date" />
          </Form.Item>
          <Space.Compact block style={{ alignItems: 'flex-start' }}>
            <div style={{ flex: 1 }}><Form.Item name="eventStartDate" label="Event start date"><Input type="date" /></Form.Item></div>
            <div style={{ flex: 1 }}><Form.Item name="eventEndDate" label="Event end date"><Input type="date" /></Form.Item></div>
          </Space.Compact>
          <Form.Item name="eventExternalReference" label="Event reference" extra="Optional reference such as a call-up notice or case number.">
            <Input />
          </Form.Item>

          <Alert type="info" showIcon message="The backend excludes non-working days and public holidays from the submitted range." style={{ marginBottom: 16 }} />
          <Typography.Paragraph type="secondary">Event-based policies create or reuse the qualifying event automatically. If verification is required, the request is recorded first and moves to normal approval only after the event is verified.</Typography.Paragraph>
          <Space>
            <Button type="primary" htmlType="submit" loading={submitting} disabled={!staffId}>Submit request</Button>
            <Button onClick={() => navigate('/leave-requests')}>Cancel</Button>
          </Space>
        </Form>
      </Card>
    </PageContainer>
  );
};

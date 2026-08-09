import { useCan } from '@refinedev/core';
import { App, Alert, Button, Card, Form, Input, Select, Space, Typography } from 'antd';
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
      });
      message.success(`Leave request submitted for ${applications.length} working day${applications.length === 1 ? '' : 's'}.`);
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
          <Alert type="info" showIcon message="The backend excludes non-working days and public holidays from the submitted range." style={{ marginBottom: 16 }} />
          <Typography.Paragraph type="secondary">The request is submitted directly as Pending for approval. Leave validation and entitlement rules remain authoritative on the server.</Typography.Paragraph>
          <Space>
            <Button type="primary" htmlType="submit" loading={submitting} disabled={!staffId}>Submit request</Button>
            <Button onClick={() => navigate('/leave-requests')}>Cancel</Button>
          </Space>
        </Form>
      </Card>
    </PageContainer>
  );
};

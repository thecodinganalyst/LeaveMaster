import { App, Alert, Button, Card, DatePicker, Form, Select, Space, Typography } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { getCurrentUser } from '../../auth/session.ts';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { applyForLeave, getLeaveTypes, type LeaveDuration, type LeaveTypeSummary } from '../../features/leave/leaveApi.ts';

interface FormValues {
  leaveTypeId: string;
  dates: [Dayjs, Dayjs];
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

  useEffect(() => {
    void (async () => {
      try {
        const user = await getCurrentUser();
        setStaffId(user.staffId);
        setLeaveTypes(await getLeaveTypes());
      } catch (cause) {
        setError(cause instanceof Error ? cause.message : 'Unable to initialize leave application.');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const submit = async (values: FormValues) => {
    if (!staffId) return;
    setSubmitting(true);
    setError(undefined);
    try {
      const applications = await applyForLeave({
        staffId,
        fromDate: values.dates[0].format('YYYY-MM-DD'),
        toDate: values.dates[1].format('YYYY-MM-DD'),
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
          <Form.Item
            name="dates"
            label="Dates"
            rules={[
              { required: true, message: 'Select a date range' },
              {
                validator: (_, value: [Dayjs, Dayjs] | undefined) =>
                  value && value[0].isAfter(value[1], 'day') ? Promise.reject(new Error('Start date must be on or before end date')) : Promise.resolve(),
              },
            ]}
          >
            <DatePicker.RangePicker style={{ width: '100%' }} format="YYYY-MM-DD" disabledDate={(date) => date.isBefore(dayjs().startOf('day').subtract(1, 'year'))} />
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

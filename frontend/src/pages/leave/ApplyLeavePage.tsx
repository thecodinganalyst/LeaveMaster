import { UploadOutlined } from '@ant-design/icons';
import { App, Alert, Button, Card, Divider, Form, Input, Select, Space, Typography, Upload } from 'antd';
import type { UploadFile } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { apiFetch } from '../../api/http.ts';
import { getCurrentUser } from '../../auth/session.ts';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import {
  applyForLeave,
  getLeaveApplicationPolicyMetadata,
  getLeaveTypes,
  type LeaveApplicationPolicyMetadata,
  type LeaveDuration,
  type LeaveTypeSummary,
} from '../../features/leave/leaveApi.ts';
import { normalizeLeaveTypes, normalizePolicyMetadata } from './applyLeaveGuards.ts';

interface FormValues {
  leaveTypeId: string;
  fromDate: string;
  toDate: string;
  leaveDuration: LeaveDuration;
  eventDate?: string;
  eventStartDate?: string;
  eventEndDate?: string;
  eventExternalReference?: string;
  attachment?: UploadFile[];
}

interface StaffEmploymentDates {
  id: string;
  joinDate: string;
  termDate?: string | null;
}

const employmentDateError = (
  date: string | undefined,
  employmentDates: StaffEmploymentDates | undefined,
) => {
  if (!date || !employmentDates) return undefined;
  if (date < employmentDates.joinDate) {
    return `Leave cannot be requested before your join date (${employmentDates.joinDate}).`;
  }
  if (employmentDates.termDate && date > employmentDates.termDate) {
    return `Leave cannot be requested after your termination date (${employmentDates.termDate}).`;
  }
  return undefined;
};

export const ApplyLeavePage = () => {
  const [staffId, setStaffId] = useState<string | null>(null);
  const [employmentDates, setEmploymentDates] = useState<StaffEmploymentDates>();
  const [canWrite, setCanWrite] = useState<boolean | null>(null);
  const [leaveTypes, setLeaveTypes] = useState<LeaveTypeSummary[]>([]);
  const [policyMetadata, setPolicyMetadata] = useState<LeaveApplicationPolicyMetadata>();
  const [policyLoading, setPolicyLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string>();
  const [form] = Form.useForm<FormValues>();
  const navigate = useNavigate();
  const { message } = App.useApp();
  const selectedLeaveTypeId = Form.useWatch('leaveTypeId', form);
  const fromDate = Form.useWatch('fromDate', form);
  const eventDate = Form.useWatch('eventDate', form);

  useEffect(() => {
    void (async () => {
      try {
        const user = await getCurrentUser();
        const authorities = Array.isArray(user.authorities) ? user.authorities : [];
        const allowedToApply = authorities.includes('LEAVE_APPLICATION_WRITE');
        const currentStaffId = typeof user.staffId === 'string' ? user.staffId : null;
        setStaffId(currentStaffId);
        setCanWrite(allowedToApply);
        if (allowedToApply && currentStaffId) {
          const [leaveTypeResponse, staffResponse] = await Promise.all([
            getLeaveTypes(),
            apiFetch<StaffEmploymentDates>(`/api/staff/${encodeURIComponent(currentStaffId)}`),
          ]);
          const normalized = normalizeLeaveTypes(leaveTypeResponse);
          setLeaveTypes(normalized);
          setEmploymentDates(staffResponse);
          if (!Array.isArray(leaveTypeResponse)) {
            setError('Leave types could not be loaded because the server returned an unexpected response.');
          }
        } else if (allowedToApply) {
          const response = await getLeaveTypes();
          const normalized = normalizeLeaveTypes(response);
          setLeaveTypes(normalized);
          if (!Array.isArray(response)) {
            setError('Leave types could not be loaded because the server returned an unexpected response.');
          }
        }
      } catch (cause) {
        console.error('ApplyLeavePage initialization failed', cause);
        setError(cause instanceof Error ? cause.message : 'Unable to initialize leave application.');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  useEffect(() => {
    if (!staffId || !selectedLeaveTypeId) {
      setPolicyMetadata(undefined);
      return;
    }
    let cancelled = false;
    setPolicyLoading(true);
    void getLeaveApplicationPolicyMetadata(staffId, selectedLeaveTypeId, eventDate || fromDate)
      .then((metadata) => {
        if (cancelled) return;
        const normalized = normalizePolicyMetadata(metadata);
        setPolicyMetadata(normalized);
        if (!normalized) {
          setError('Leave policy details could not be loaded because the server returned an unexpected response.');
        }
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          console.error('ApplyLeavePage policy lookup failed', cause);
          setPolicyMetadata(undefined);
          setError(cause instanceof Error ? cause.message : 'Unable to resolve leave policy.');
        }
      })
      .finally(() => {
        if (!cancelled) setPolicyLoading(false);
      });
    return () => { cancelled = true; };
  }, [eventDate, fromDate, selectedLeaveTypeId, staffId]);

  useEffect(() => {
    if (!policyMetadata?.eventBased) {
      form.resetFields(['eventDate', 'eventStartDate', 'eventEndDate', 'eventExternalReference']);
    }
  }, [form, policyMetadata?.eventBased]);

  const submit = async (values: FormValues) => {
    if (!staffId || !canWrite) return;
    const startDateError = employmentDateError(values.fromDate, employmentDates);
    const endDateError = employmentDateError(values.toDate, employmentDates);
    if (startDateError || endDateError) {
      setError(startDateError ?? endDateError);
      return;
    }
    setSubmitting(true);
    setError(undefined);
    try {
      const attachment = values.attachment?.[0]?.originFileObj;
      const applications = await applyForLeave({
        staffId,
        fromDate: values.fromDate,
        toDate: values.toDate,
        leaveTypeId: values.leaveTypeId,
        leaveDuration: values.leaveDuration,
        status: 'PENDING',
        ...(policyMetadata?.eventBased && values.eventDate ? { eventDate: values.eventDate } : {}),
        ...(policyMetadata?.eventBased && values.eventStartDate ? { eventStartDate: values.eventStartDate } : {}),
        ...(policyMetadata?.eventBased && values.eventEndDate ? { eventEndDate: values.eventEndDate } : {}),
        ...(policyMetadata?.eventBased && values.eventExternalReference ? { eventExternalReference: values.eventExternalReference } : {}),
      }, attachment);
      const awaitingVerification = Array.isArray(applications)
        && applications.some((application) => application.status === 'PENDING_VERIFICATION');
      const count = Array.isArray(applications) ? applications.length : 0;
      message.success(awaitingVerification
        ? 'Leave request recorded and is awaiting qualifying-event verification.'
        : `Leave request submitted for ${count} working day${count === 1 ? '' : 's'}.`);
      navigate('/leave-requests');
    } catch (cause) {
      console.error('ApplyLeavePage submission failed', cause);
      setError(cause instanceof Error ? cause.message : 'Unable to submit leave request.');
    } finally {
      setSubmitting(false);
    }
  };

  if (!loading && canWrite === false) {
    return <PageContainer><Alert type="warning" showIcon message="You do not have permission to submit leave applications." /></PageContainer>;
  }

  return (
    <PageContainer>
      <PageHeader title="Apply for leave" subtitle="Submit a leave request for one or more working days." />
      {error ? <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} /> : null}
      {!staffId && !loading && !error ? <Alert type="warning" showIcon message="Your account is not linked to a staff record, so leave cannot be submitted." style={{ marginBottom: 16 }} /> : null}
      <Card>
        {loading ? <Typography.Paragraph type="secondary">Loading leave application…</Typography.Paragraph> : null}
        <Form
          form={form}
          layout="vertical"
          onFinish={submit}
          initialValues={{ leaveDuration: 'FULL' }}
          style={{ maxWidth: 640 }}
          disabled={loading || canWrite !== true}
        >
          <Form.Item name="leaveTypeId" label="Leave type" rules={[{ required: true, message: 'Select a leave type' }]}>
            <Select options={leaveTypes.map((leaveType) => ({ value: leaveType.id, label: leaveType.name }))} placeholder="Select leave type" />
          </Form.Item>
          <Form.Item
            name="fromDate"
            label="From date"
            rules={[
              { required: true, message: 'Select a start date' },
              () => ({
                validator: (_, value: string | undefined) => {
                  const validationError = employmentDateError(value, employmentDates);
                  return validationError ? Promise.reject(new Error(validationError)) : Promise.resolve();
                },
              }),
            ]}
          >
            <Input type="date" allowClear min={employmentDates?.joinDate} max={employmentDates?.termDate ?? undefined} />
          </Form.Item>
          <Form.Item
            name="toDate"
            label="To date"
            dependencies={['fromDate']}
            rules={[
              { required: true, message: 'Select an end date' },
              ({ getFieldValue }) => ({
                validator: (_, value: string | undefined) => {
                  const start = getFieldValue('fromDate') as string | undefined;
                  if (start && value && value < start) {
                    return Promise.reject(new Error('End date must be on or after start date'));
                  }
                  const validationError = employmentDateError(value, employmentDates);
                  return validationError ? Promise.reject(new Error(validationError)) : Promise.resolve();
                },
              }),
            ]}
          >
            <Input type="date" allowClear min={employmentDates?.joinDate} max={employmentDates?.termDate ?? undefined} />
          </Form.Item>
          <Form.Item name="leaveDuration" label="Duration" rules={[{ required: true }]}>
            <Select options={[
              { value: 'FULL', label: 'Full day' },
              { value: 'AM', label: 'Morning half day' },
              { value: 'PM', label: 'Afternoon half day' },
            ]} />
          </Form.Item>

          {policyMetadata?.eventBased ? (
            <>
              <Divider orientation="left">Qualifying event</Divider>
              <Alert
                type="info"
                showIcon
                message="Describe the qualifying event for this leave"
                description="These generic event details are used to create or reuse the qualifying event behind this leave request."
                style={{ marginBottom: 16 }}
              />
              <Form.Item name="eventDate" label="Event date" rules={[{ required: true, message: 'Event date is required for event-based leave' }]}>
                <Input type="date" allowClear />
              </Form.Item>
              <Space.Compact block style={{ alignItems: 'flex-start' }}>
                <div style={{ flex: 1 }}><Form.Item name="eventStartDate" label="Event start date"><Input type="date" allowClear /></Form.Item></div>
                <div style={{ flex: 1 }}><Form.Item name="eventEndDate" label="Event end date"><Input type="date" allowClear /></Form.Item></div>
              </Space.Compact>
              <Form.Item name="eventExternalReference" label="Event reference" extra="Optional external reference such as a registration, case, notice, or call-up number.">
                <Input />
              </Form.Item>
            </>
          ) : null}

          <Form.Item
            name="attachment"
            label="Attachment"
            valuePropName="fileList"
            getValueFromEvent={(event) => event?.fileList}
            rules={policyMetadata?.eventBased && policyMetadata.eventRequiresVerification
              ? [{ required: true, message: 'Attachment is required because this qualifying event must be verified' }]
              : []}
            extra={policyMetadata?.eventBased && policyMetadata.eventRequiresVerification
              ? 'Evidence is required for verification of this qualifying event.'
              : 'Optional supporting evidence. PDF and common image formats are supported by the existing attachment storage flow.'}
          >
            <Upload
              beforeUpload={() => false}
              maxCount={1}
              accept="application/pdf,image/jpeg,image/png,image/gif,image/webp"
            >
              <Button icon={<UploadOutlined />}>Choose file</Button>
            </Upload>
          </Form.Item>

          {policyLoading ? <Typography.Paragraph type="secondary">Resolving leave policy…</Typography.Paragraph> : null}
          <Alert type="info" showIcon message="The backend excludes non-working days and public holidays from the submitted range." style={{ marginBottom: 16 }} />
          {policyMetadata?.eventBased ? (
            <Typography.Paragraph type="secondary">Event-based policies create or reuse the qualifying event automatically. If verification is required, this leave attachment is used as the supporting evidence and the request moves to normal approval after verification.</Typography.Paragraph>
          ) : null}
          <Space>
            <Button type="primary" htmlType="submit" loading={submitting || policyLoading} disabled={!staffId || canWrite !== true}>Submit request</Button>
            <Button onClick={() => navigate('/leave-requests')}>Cancel</Button>
          </Space>
        </Form>
      </Card>
    </PageContainer>
  );
};

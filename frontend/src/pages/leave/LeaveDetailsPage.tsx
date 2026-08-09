import { useCan } from '@refinedev/core';
import { App, Alert, Button, Card, Descriptions, Popconfirm, Select, Space, Tag } from 'antd';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { getCurrentUser } from '../../auth/session.ts';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import {
  cancelLeave,
  getLeaveApplication,
  updateLeaveDuration,
  type LeaveApplication,
  type LeaveDuration,
} from '../../features/leave/leaveApi.ts';
import { canCancelApplication, canEditApplication, statusColor, statusLabel } from '../../features/leave/leaveView.ts';

export const LeaveDetailsPage = () => {
  const { id } = useParams();
  const [application, setApplication] = useState<LeaveApplication>();
  const [currentStaffId, setCurrentStaffId] = useState<string | null>(null);
  const [duration, setDuration] = useState<LeaveDuration>('FULL');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string>();
  const { message } = App.useApp();
  const { data: canWrite } = useCan({ resource: 'leave-requests', action: 'edit' });

  const refresh = async () => {
    if (!id) return;
    try {
      const [data, user] = await Promise.all([getLeaveApplication(id), getCurrentUser()]);
      setApplication(data);
      setCurrentStaffId(user.staffId);
      setDuration(data.leaveDuration);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to load leave request.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void refresh(); }, [id]);

  const ownsApplication = Boolean(application && currentStaffId && application.staff.id === currentStaffId);

  const saveDuration = async () => {
    if (!application || !ownsApplication) return;
    setSaving(true);
    try {
      setApplication(await updateLeaveDuration(application, duration));
      message.success('Leave duration updated.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to update leave request.');
    } finally {
      setSaving(false);
    }
  };

  const cancel = async () => {
    if (!application || !ownsApplication) return;
    setSaving(true);
    try {
      await cancelLeave(application.id);
      await refresh();
      message.success('Cancellation processed. Past approved leave may require approver confirmation.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to cancel leave request.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <PageContainer>
      <PageHeader title="Leave request details" subtitle={id} extra={<Button><Link to="/leave-requests">Back to requests</Link></Button>} />
      {error ? <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} /> : null}
      <Card loading={loading}>
        {application ? (
          <>
            <Descriptions bordered column={{ xs: 1, md: 2 }}>
              <Descriptions.Item label="Staff">{application.staff.name}</Descriptions.Item>
              <Descriptions.Item label="Date">{application.leaveDate}</Descriptions.Item>
              <Descriptions.Item label="Leave type">{application.leaveType.name}</Descriptions.Item>
              <Descriptions.Item label="Duration">{application.leaveDuration}</Descriptions.Item>
              <Descriptions.Item label="Status"><Tag color={statusColor[application.status]}>{statusLabel[application.status]}</Tag></Descriptions.Item>
              <Descriptions.Item label="Applied on">{application.applicationDate}</Descriptions.Item>
              <Descriptions.Item label="Approver">{application.approver?.name ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="Approval date">{application.approvalDate ?? '—'}</Descriptions.Item>
            </Descriptions>

            {ownsApplication && canWrite?.can && canEditApplication(application) ? (
              <Space wrap style={{ marginTop: 16 }}>
                <Select<LeaveDuration>
                  value={duration}
                  onChange={setDuration}
                  options={[
                    { value: 'FULL', label: 'Full day' },
                    { value: 'AM', label: 'Morning half day' },
                    { value: 'PM', label: 'Afternoon half day' },
                  ]}
                  style={{ width: 190 }}
                />
                <Button onClick={saveDuration} loading={saving} disabled={duration === application.leaveDuration}>Save duration</Button>
              </Space>
            ) : null}

            {ownsApplication && canWrite?.can && canCancelApplication(application) ? (
              <Popconfirm
                title="Cancel this leave request?"
                description="Approved leave in the past becomes a cancellation request and requires approval; other eligible requests are cancelled immediately."
                onConfirm={cancel}
              >
                <Button danger loading={saving} style={{ marginTop: 16 }}>Cancel leave</Button>
              </Popconfirm>
            ) : null}
          </>
        ) : null}
      </Card>
    </PageContainer>
  );
};

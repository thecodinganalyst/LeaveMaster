import { useCan } from '@refinedev/core';
import { App, Alert, Button, Card, Descriptions, Empty, Modal, Space, Table, Tag, Typography } from 'antd';
import { useEffect, useState } from 'react';

import { getCurrentUser } from '../../auth/session.ts';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import {
  approveCancellation,
  approveLeave,
  getPendingApprovals,
  getVisibleLeave,
  rejectCancellation,
  rejectLeave,
  type LeaveApplication,
} from '../../features/leave/leaveApi.ts';
import { statusColor, statusLabel } from '../../features/leave/leaveView.ts';

export const ApprovalInboxPage = () => {
  const [staffId, setStaffId] = useState<string | null>(null);
  const [applications, setApplications] = useState<LeaveApplication[]>([]);
  const [selected, setSelected] = useState<LeaveApplication>();
  const [loading, setLoading] = useState(true);
  const [acting, setActing] = useState(false);
  const [error, setError] = useState<string>();
  const { message } = App.useApp();
  const { data: canApprove } = useCan({ resource: 'leave-requests', action: 'approve' });

  const load = async () => {
    setLoading(true);
    try {
      const user = await getCurrentUser();
      setStaffId(user.staffId);
      if (!user.staffId) return;
      const [pending, visible] = await Promise.all([
        getPendingApprovals(user.staffId),
        getVisibleLeave(user.staffId),
      ]);
      const cancellation = visible.filter(
        (application) => application.status === 'CANCEL_REQUESTED' && application.staff.id !== user.staffId,
      );
      const byId = new Map([...pending, ...cancellation].map((application) => [application.id, application]));
      setApplications([...byId.values()].sort((a, b) => a.leaveDate.localeCompare(b.leaveDate)));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to load approval inbox.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, []);

  const decide = async (decision: 'approve' | 'reject') => {
    if (!selected || !staffId) return;
    setActing(true);
    try {
      if (selected.status === 'CANCEL_REQUESTED') {
        await (decision === 'approve' ? approveCancellation(selected.id) : rejectCancellation(selected.id));
      } else {
        await (decision === 'approve' ? approveLeave(selected.id, staffId) : rejectLeave(selected.id, staffId));
      }
      message.success(decision === 'approve' ? 'Request approved.' : 'Request rejected.');
      setSelected(undefined);
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to process approval.');
    } finally {
      setActing(false);
    }
  };

  if (!canApprove?.can) {
    return <PageContainer><Alert type="warning" showIcon message="You do not have permission to approve leave applications." /></PageContainer>;
  }

  return (
    <PageContainer>
      <PageHeader title="Approval inbox" subtitle="Pending leave requests and cancellation requests assigned to you." />
      {error ? <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} /> : null}
      {!staffId && !loading ? <Alert type="info" showIcon message="This account is not linked to a staff record." style={{ marginBottom: 16 }} /> : null}
      <Card>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={applications}
          locale={{ emptyText: <Empty description="No requests awaiting action" /> }}
          scroll={{ x: 760 }}
          columns={[
            { title: 'Staff', render: (_, row) => row.staff.name },
            { title: 'Date', dataIndex: 'leaveDate' },
            { title: 'Leave type', render: (_, row) => row.leaveType.name },
            { title: 'Duration', dataIndex: 'leaveDuration' },
            { title: 'Status', render: (_, row) => <Tag color={statusColor[row.status]}>{statusLabel[row.status]}</Tag> },
            { title: 'Action', render: (_, row) => <Button type="link" onClick={() => setSelected(row)}>Review</Button> },
          ]}
        />
      </Card>

      <Modal
        open={Boolean(selected)}
        title={selected?.status === 'CANCEL_REQUESTED' ? 'Review cancellation request' : 'Review leave request'}
        onCancel={() => setSelected(undefined)}
        footer={selected ? [
          <Button key="reject" danger loading={acting} onClick={() => void decide('reject')}>Reject</Button>,
          <Button key="approve" type="primary" loading={acting} onClick={() => void decide('approve')}>Approve</Button>,
        ] : null}
      >
        {selected ? (
          <>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Staff">{selected.staff.name}</Descriptions.Item>
              <Descriptions.Item label="Date">{selected.leaveDate}</Descriptions.Item>
              <Descriptions.Item label="Leave type">{selected.leaveType.name}</Descriptions.Item>
              <Descriptions.Item label="Duration">{selected.leaveDuration}</Descriptions.Item>
              <Descriptions.Item label="Status">{statusLabel[selected.status]}</Descriptions.Item>
              <Descriptions.Item label="Applied on">{selected.applicationDate}</Descriptions.Item>
            </Descriptions>
            <Space direction="vertical" style={{ marginTop: 12 }}>
              <Typography.Text type="secondary">The current backend approval API does not support approver comments, so this screen does not collect or discard comments.</Typography.Text>
            </Space>
          </>
        ) : null}
      </Modal>
    </PageContainer>
  );
};

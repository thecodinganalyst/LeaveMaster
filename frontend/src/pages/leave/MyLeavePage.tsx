import { useCan } from '@refinedev/core';
import { Alert, Button, Card, Input, Space, Table, Tag, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';

import { getCurrentUser } from '../../auth/session.ts';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { getVisibleLeave, type LeaveApplication } from '../../features/leave/leaveApi.ts';
import { statusColor, statusLabel } from '../../features/leave/leaveView.ts';

export const MyLeavePage = () => {
  const [applications, setApplications] = useState<LeaveApplication[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [staffId, setStaffId] = useState<string | null>(null);
  const { data: canWrite } = useCan({ resource: 'leave-requests', action: 'create' });

  useEffect(() => {
    void (async () => {
      try {
        const user = await getCurrentUser();
        setStaffId(user.staffId);
        if (!user.staffId) return;
        setApplications(await getVisibleLeave(user.staffId));
      } catch (cause) {
        setError(cause instanceof Error ? cause.message : 'Unable to load leave requests.');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const rows = useMemo(() => {
    const needle = search.trim().toLowerCase();
    const sorted = [...applications].sort((a, b) => b.leaveDate.localeCompare(a.leaveDate));
    if (!needle) return sorted;
    return sorted.filter((application) =>
      [application.leaveType.name, application.leaveDate, application.status, application.staff.name]
        .some((value) => value.toLowerCase().includes(needle)),
    );
  }, [applications, search]);

  return (
    <PageContainer>
      <PageHeader
        title="Leave requests"
        subtitle="Review your leave history and any team leave visible to you."
        extra={canWrite?.can && staffId ? <Button type="primary"><Link to="/leave-requests/apply">Apply for leave</Link></Button> : undefined}
      />
      {error ? <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} /> : null}
      {!staffId && !loading ? <Alert type="info" showIcon message="This account is not linked to a staff record." style={{ marginBottom: 16 }} /> : null}
      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Input.Search allowClear placeholder="Search leave type, date, status or staff" onChange={(event) => setSearch(event.target.value)} style={{ maxWidth: 420 }} />
          <Table
            loading={loading}
            rowKey="id"
            dataSource={rows}
            pagination={{ pageSize: 10, showSizeChanger: true }}
            scroll={{ x: 760 }}
            columns={[
              { title: 'Staff', render: (_, row) => row.staff.name },
              { title: 'Date', dataIndex: 'leaveDate', sorter: (a, b) => a.leaveDate.localeCompare(b.leaveDate) },
              { title: 'Leave type', render: (_, row) => row.leaveType.name },
              { title: 'Duration', render: (_, row) => row.leaveDuration === 'FULL' ? 'Full day' : `${row.leaveDuration} half day` },
              { title: 'Status', render: (_, row) => <Tag color={statusColor[row.status]}>{statusLabel[row.status]}</Tag> },
              { title: 'Action', render: (_, row) => <Link to={`/leave-requests/show/${row.id}`}>View</Link> },
            ]}
          />
          <Typography.Text type="secondary">Leave visibility and tenant isolation are determined by the backend for the signed-in staff member.</Typography.Text>
        </Space>
      </Card>
    </PageContainer>
  );
};

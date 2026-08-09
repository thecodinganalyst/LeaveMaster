import { useCan } from '@refinedev/core';
import { Alert, Button, Card, Col, List, Row, Space, Statistic, Tag, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

import { getCurrentUser } from '../../auth/session.ts';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { getLeaveBalances, getVisibleLeave, type LeaveApplication, type LeaveBalance } from '../../features/leave/leaveApi.ts';
import { isUpcoming, sortByLeaveDate, statusColor, statusLabel } from '../../features/leave/leaveView.ts';

export const DashboardPage = () => {
  const [staffId, setStaffId] = useState<string | null>(null);
  const [balances, setBalances] = useState<LeaveBalance[]>([]);
  const [applications, setApplications] = useState<LeaveApplication[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const { data: canWrite } = useCan({ resource: 'leave-requests', action: 'create' });
  const { data: canApprove } = useCan({ resource: 'leave-requests', action: 'approve' });

  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const user = await getCurrentUser();
        if (!active) return;
        setStaffId(user.staffId);
        if (!user.staffId || !user.authorities.includes('LEAVE_APPLICATION_READ')) return;
        const [balanceData, leaveData] = await Promise.all([
          getLeaveBalances(user.staffId),
          getVisibleLeave(user.staffId),
        ]);
        if (!active) return;
        setBalances(balanceData);
        setApplications(leaveData);
      } catch (cause) {
        if (active) setError(cause instanceof Error ? cause.message : 'Unable to load leave dashboard.');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const upcoming = sortByLeaveDate(applications.filter((application) => isUpcoming(application))).slice(0, 5);
  const pending = applications.filter((application) => application.status === 'PENDING').length;

  return (
    <PageContainer>
      <PageHeader
        title="My Leave"
        subtitle="Balances, upcoming leave and requests that need attention."
        extra={
          <Space wrap>
            {canApprove?.can ? <Button><Link to="/approvals">Approval inbox</Link></Button> : null}
            {canWrite?.can && staffId ? <Button type="primary"><Link to="/leave-requests/apply">Apply for leave</Link></Button> : null}
          </Space>
        }
      />
      {error ? <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} /> : null}
      {!staffId && !loading ? <Alert type="info" showIcon message="This account is not linked to a staff record, so personal leave information is unavailable." style={{ marginBottom: 16 }} /> : null}

      <Row gutter={[16, 16]}>
        {balances.map((balance) => (
          <Col xs={24} sm={12} lg={8} key={balance.leaveType.id}>
            <Card loading={loading}>
              <Typography.Text strong>{balance.leaveType.name}</Typography.Text>
              <Row gutter={8} style={{ marginTop: 12 }}>
                <Col span={8}><Statistic title="Entitlement" value={balance.entitlement} precision={1} /></Col>
                <Col span={8}><Statistic title="Used / pending" value={balance.used} precision={1} /></Col>
                <Col span={8}><Statistic title="Balance" value={balance.balance} precision={1} /></Col>
              </Row>
            </Card>
          </Col>
        ))}
        {staffId ? (
          <Col xs={24} sm={12} lg={8}>
            <Card loading={loading}><Statistic title="Pending requests" value={pending} /></Card>
          </Col>
        ) : null}
      </Row>

      <Card title="Upcoming leave" style={{ marginTop: 16 }} loading={loading} extra={<Link to="/leave-requests">View all</Link>}>
        <List
          locale={{ emptyText: 'No upcoming leave.' }}
          dataSource={upcoming}
          renderItem={(application) => (
            <List.Item actions={[<Link key="view" to={`/leave-requests/show/${application.id}`}>View</Link>]}>
              <List.Item.Meta
                title={`${application.leaveType.name} — ${application.leaveDate}`}
                description={application.leaveDuration === 'FULL' ? 'Full day' : `${application.leaveDuration} half day`}
              />
              <Tag color={statusColor[application.status]}>{statusLabel[application.status]}</Tag>
            </List.Item>
          )}
        />
      </Card>
    </PageContainer>
  );
};

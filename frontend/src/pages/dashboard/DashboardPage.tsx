import { Card, Col, Row, Statistic } from 'antd';

import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';

export const DashboardPage = () => {
  return (
    <PageContainer>
      <PageHeader
        title="Operations Dashboard"
        subtitle="Visibility into approvals, team availability and utilization."
      />
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic title="Pending approvals" value={12} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic title="Employees on leave" value={6} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic title="Leave utilization" value={74} suffix="%" />
          </Card>
        </Col>
      </Row>
    </PageContainer>
  );
};

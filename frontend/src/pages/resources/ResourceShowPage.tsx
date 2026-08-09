import { Card, Descriptions } from 'antd';
import { useParams } from 'react-router-dom';

import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { useResourceMeta } from './resourceMeta.ts';

export const ResourceShowPage = () => {
  const { id } = useParams();
  const { label } = useResourceMeta();

  return (
    <PageContainer>
      <PageHeader title={`${label} Details`} subtitle="Standardized read-only detail view." />
      <Card>
        <Descriptions bordered column={1}>
          <Descriptions.Item label="Record ID">{id}</Descriptions.Item>
          <Descriptions.Item label="Status">Pending</Descriptions.Item>
          <Descriptions.Item label="Owner">Demo User</Descriptions.Item>
        </Descriptions>
      </Card>
    </PageContainer>
  );
};

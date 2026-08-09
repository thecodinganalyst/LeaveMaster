import { Button } from 'antd';
import { Link } from 'react-router-dom';

import { DataTable } from '../../components/common/DataTable.tsx';
import { EmptyState } from '../../components/common/EmptyState.tsx';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { useResourceMeta } from './resourceMeta.ts';

const rows = [
  { id: '1', status: 'Approved', owner: 'Avery Stone' },
  { id: '2', status: 'Pending', owner: 'Jordan Hale' },
];

export const ResourceListPage = () => {
  const { label, name } = useResourceMeta();

  if (!name) {
    return <EmptyState title="Resource unavailable" description="No resource was selected." />;
  }

  return (
    <PageContainer>
      <PageHeader
        title={label}
        subtitle={`Manage ${label.toLowerCase()} records.`}
        extra={
          <Button type="primary">
            <Link to={`/${name}/create`}>Create</Link>
          </Button>
        }
      />
      <DataTable
        rowKey="id"
        dataSource={rows}
        columns={[
          { title: 'ID', dataIndex: 'id' },
          { title: 'Status', dataIndex: 'status' },
          { title: 'Owner', dataIndex: 'owner' },
          {
            title: 'Actions',
            render: (_, row) => <Link to={`/${name}/show/${row.id}`}>View</Link>,
          },
        ]}
      />
    </PageContainer>
  );
};

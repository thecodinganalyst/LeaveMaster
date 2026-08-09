import { useCan, useOne, useResource } from '@refinedev/core';
import { Button, Card, Descriptions, Space } from 'antd';
import { Link, useParams } from 'react-router-dom';

import { LoadingState } from '../../components/common/LoadingState.tsx';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { getAdminResourceConfig } from './adminResourceConfig.ts';
import { RoleMembershipCard } from './RoleMembershipCard.tsx';

const displayValue = (value: unknown) => {
  if (typeof value === 'boolean') return value ? 'Yes' : 'No';
  if (value && typeof value === 'object') return JSON.stringify(value, null, 2);
  return String(value ?? '—');
};

export const ResourceShowPage = () => {
  const { resource } = useResource();
  const { id } = useParams();
  const config = getAdminResourceConfig(resource?.name);
  const { query } = useOne({ resource: config?.name ?? '', id: id ?? '', queryOptions: { enabled: Boolean(config && id) } });
  const { data: canEdit } = useCan({ resource: config?.name ?? '', action: 'edit' });

  if (!config || !id) return null;
  if (query.isLoading) return <LoadingState />;
  const record = (query.data?.data ?? {}) as Record<string, unknown>;

  return (
    <PageContainer>
      <PageHeader
        title={`${config.singular} details`}
        subtitle={id}
        extra={canEdit?.can ? <Button type="primary"><Link to={`/${config.name}/edit/${encodeURIComponent(id)}`}>Edit</Link></Button> : undefined}
      />
      <Card>
        <Descriptions bordered column={{ xs: 1, sm: 1, md: 2 }}>
          {config.fields.filter((field) => field.type !== 'password').map((field) => (
            <Descriptions.Item key={field.name} label={field.label}>
              <span style={{ whiteSpace: field.type === 'json' ? 'pre-wrap' : 'normal' }}>{displayValue(record[field.name])}</span>
            </Descriptions.Item>
          ))}
        </Descriptions>
      </Card>
      {config.name === 'roles' && canEdit?.can ? <RoleMembershipCard roleId={id} /> : null}
      <Space><Link to={`/${config.name}`}>Back to {config.label}</Link></Space>
    </PageContainer>
  );
};

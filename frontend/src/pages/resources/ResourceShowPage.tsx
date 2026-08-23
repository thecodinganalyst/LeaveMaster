import { useCan, useGetIdentity, useOne, useResource } from '@refinedev/core';
import { Button, Card, Descriptions, Space } from 'antd';
import { Link, useParams } from 'react-router-dom';

import { LoadingState } from '../../components/common/LoadingState.tsx';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import type { AdminField } from './resourceConfigResolver.ts';
import { getAdminResourceConfig, isAdminFieldVisible, toFormValues } from './resourceConfigResolver.ts';
import { PublicHolidayTable } from './PublicHolidayTable.tsx';
import { RoleMembershipCard } from './RoleMembershipCard.tsx';
import { RolePermissionCheckboxList } from './RolePermissionCheckboxList.tsx';
import { TenantEntitlementPolicySummary } from './TenantEntitlementPolicySummary.tsx';
import { TenantLeaveTypeName } from './TenantLeaveTypeName.tsx';
import { shouldHideTenantInternalId, shouldShowResourceIdSubtitle } from './tenantInternalIdVisibility.ts';

interface LeaveMasterIdentity {
  platformAdmin?: boolean;
}

const displayValue = (field: AdminField, value: unknown) => {
  if (typeof value === 'boolean') return value ? 'Yes' : 'No';
  if (field.options) {
    const option = field.options.find((item) => item.value === value);
    if (option) return option.label;
  }
  if (value && typeof value === 'object') return JSON.stringify(value, null, 2);
  return String(value ?? '—');
};

export const tenantLeaveTypeSourceLink = (value: unknown) => {
  const url = typeof value === 'string' ? value.trim() : '';
  if (!url) return <span>—</span>;
  return <a href={url} target="_blank" rel="noopener noreferrer">{url}</a>;
};

export const ResourceShowPage = () => {
  const { resource } = useResource();
  const { id } = useParams();
  const config = getAdminResourceConfig(resource?.name);
  const { data: identity } = useGetIdentity<LeaveMasterIdentity>();
  const platformAdmin = Boolean(identity?.platformAdmin);
  const recordQuery = useOne({ resource: config?.name ?? '', id: id ?? '', queryOptions: { enabled: Boolean(config && id) } });
  const { data: canEdit } = useCan({ resource: config?.name ?? '', action: 'edit' });

  if (!config || !id) return null;
  if (recordQuery.isLoading) return <LoadingState />;
  const record = toFormValues(config, (recordQuery.data?.data ?? {}) as Record<string, unknown>);

  return (
    <PageContainer>
      <PageHeader
        title={`${config.singular} details`}
        {...(shouldShowResourceIdSubtitle(config.name, platformAdmin) ? { subtitle: id } : {})}
        extra={canEdit?.can && config.editable !== false ? <Button type="primary"><Link to={`/${config.name}/edit/${encodeURIComponent(id)}`}>Edit</Link></Button> : undefined}
      />
      <Card>
        <Descriptions bordered column={1}>
          {config.fields.filter((field) => field.type !== 'password'
            && !field.hidden
            && isAdminFieldVisible(field, platformAdmin)
            && !shouldHideTenantInternalId(config.name, config.idField, field.name, platformAdmin)).map((field) => (
            <Descriptions.Item key={field.name} label={field.label}>
              {field.type === 'permissions' ? (
                <RolePermissionCheckboxList value={(record[field.name] as string[] | undefined) ?? []} disabled />
              ) : field.type === 'holiday-list' ? (
                <PublicHolidayTable value={record[field.name]} />
              ) : config.name === 'leave-types' && field.name === 'sourceUrl' && !platformAdmin ? (
                tenantLeaveTypeSourceLink(record[field.name])
              ) : config.name === 'leave-entitlement-policies' && field.name === 'leaveTypeId' && !platformAdmin ? (
                <TenantLeaveTypeName leaveTypeId={String(record[field.name] ?? '')} />
              ) : config.name === 'leave-entitlement-policy-eligibility-rules' && field.name === 'policyId' && !platformAdmin ? (
                <TenantEntitlementPolicySummary policyId={String(record[field.name] ?? '')} />
              ) : (
                <span style={{ whiteSpace: field.type === 'json' ? 'pre-wrap' : 'normal' }}>{displayValue(field, record[field.name])}</span>
              )}
            </Descriptions.Item>
          ))}
        </Descriptions>
      </Card>
      {config.name === 'roles' && canEdit?.can ? <RoleMembershipCard roleId={id} /> : null}
      <Space><Link to={`/${config.name}`}>Back to {config.label}</Link></Space>
    </PageContainer>
  );
};

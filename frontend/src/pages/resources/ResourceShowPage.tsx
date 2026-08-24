import { useCan, useGetIdentity, useOne, useResource } from '@refinedev/core';
import { useQuery } from '@tanstack/react-query';
import { Button, Card, Descriptions, Space } from 'antd';
import { Link, useParams } from 'react-router-dom';

import { apiFetch } from '../../api/http.ts';
import { LoadingState } from '../../components/common/LoadingState.tsx';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { getJurisdictionOptions, type JurisdictionOptionSource } from './jurisdictions.ts';
import { PublicHolidayTable } from './PublicHolidayTable.tsx';
import type { AdminField } from './resourceConfigResolver.ts';
import { getAdminResourceConfig, isAdminFieldVisible, toFormValues } from './resourceConfigResolver.ts';
import { RoleMembershipCard } from './RoleMembershipCard.tsx';
import { RolePermissionCheckboxList } from './RolePermissionCheckboxList.tsx';
import { StaffLeaveEntitlementsField, StaffWorkScheduleField } from './StaffDetailStructuredFields.tsx';
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

const staffRoles = (value: unknown) => Array.isArray(value) && value.length > 0
  ? value.map(String).join(', ')
  : '—';

const loadJurisdictions = () => apiFetch<JurisdictionOptionSource[]>('/api/jurisdictions');

export const ResourceShowPage = () => {
  const { resource } = useResource();
  const { id } = useParams();
  const config = getAdminResourceConfig(resource?.name);
  const { data: identity } = useGetIdentity<LeaveMasterIdentity>();
  const platformAdmin = Boolean(identity?.platformAdmin);
  const recordQuery = useOne({ resource: config?.name ?? '', id: id ?? '', queryOptions: { enabled: Boolean(config && id) } });
  const { data: canEdit } = useCan({ resource: config?.name ?? '', action: 'edit' });
  const jurisdictionsQuery = useQuery({
    queryKey: ['jurisdictions', 'staff-detail'],
    queryFn: loadJurisdictions,
    enabled: config?.name === 'employees',
    staleTime: 5 * 60_000,
  });

  if (!config || !id) return null;
  if (recordQuery.isLoading) return <LoadingState />;
  const record = toFormValues(config, (recordQuery.data?.data ?? {}) as Record<string, unknown>);
  const isStaff = config.name === 'employees';
  const visibleFields = config.fields.filter((field) => field.type !== 'password'
    && !field.hidden
    && isAdminFieldVisible(field, platformAdmin)
    && !shouldHideTenantInternalId(config.name, config.idField, field.name, platformAdmin));
  const detailFields = isStaff
    ? visibleFields.filter((field) => field.name !== 'workSchedule' && field.name !== 'leaveEntitlements')
    : visibleFields;
  const jurisdictionName = isStaff
    ? getJurisdictionOptions(jurisdictionsQuery.data ?? []).find((option) => option.value === String(record.jurisdictionId ?? ''))?.label
    : undefined;

  const renderFieldValue = (field: AdminField) => {
    if (field.type === 'permissions') {
      return <RolePermissionCheckboxList value={(record[field.name] as string[] | undefined) ?? []} disabled />;
    }
    if (field.type === 'holiday-list') return <PublicHolidayTable value={record[field.name]} />;
    if (isStaff && field.name === 'jurisdictionId') return jurisdictionName ?? displayValue(field, record[field.name]);
    if (config.name === 'leave-types' && field.name === 'sourceUrl' && !platformAdmin) return tenantLeaveTypeSourceLink(record[field.name]);
    if (config.name === 'leave-entitlement-policies' && field.name === 'leaveTypeId' && !platformAdmin) {
      return <TenantLeaveTypeName leaveTypeId={String(record[field.name] ?? '')} />;
    }
    if (config.name === 'leave-entitlement-policy-eligibility-rules' && field.name === 'policyId' && !platformAdmin) {
      return <TenantEntitlementPolicySummary policyId={String(record[field.name] ?? '')} />;
    }
    return <span style={{ whiteSpace: field.type === 'json' ? 'pre-wrap' : 'normal' }}>{displayValue(field, record[field.name])}</span>;
  };

  return (
    <PageContainer>
      <PageHeader
        title={`${config.singular} details`}
        {...(shouldShowResourceIdSubtitle(config.name, platformAdmin) ? { subtitle: id } : {})}
        extra={canEdit?.can && config.editable !== false ? <Button type="primary"><Link to={`/${config.name}/edit/${encodeURIComponent(id)}`}>Edit</Link></Button> : undefined}
      />
      <Card style={{ marginBottom: isStaff ? 16 : undefined }}>
        <Descriptions
          bordered
          column={1}
          styles={{
            label: { fontSize: 14, fontWeight: 500 },
            content: { fontSize: 14, fontWeight: 400 },
          }}
        >
          {detailFields.map((field) => (
            <Descriptions.Item key={field.name} label={field.label}>
              {renderFieldValue(field)}
            </Descriptions.Item>
          ))}
          {isStaff ? (
            <Descriptions.Item label="Roles">{staffRoles(record.roleIds)}</Descriptions.Item>
          ) : null}
        </Descriptions>
      </Card>
      {isStaff ? (
        <>
          <Card title="Work Schedule" style={{ marginBottom: 16 }}>
            <StaffWorkScheduleField value={record.workSchedule} />
          </Card>
          <Card title="Leave Entitlements" style={{ marginBottom: 16 }}>
            <StaffLeaveEntitlementsField value={record.leaveEntitlements} />
          </Card>
        </>
      ) : null}
      {config.name === 'roles' && canEdit?.can ? <RoleMembershipCard roleId={id} /> : null}
      <Space><Link to={`/${config.name}`}>Back to {config.label}</Link></Space>
    </PageContainer>
  );
};

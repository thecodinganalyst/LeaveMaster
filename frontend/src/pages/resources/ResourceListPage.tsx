import { useCan, useDelete, useGetIdentity, useList, useResource } from '@refinedev/core';
import { App, Button, Input, Popconfirm, Select, Space, Tag, type TableProps } from 'antd';
import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { DataTable } from '../../components/common/DataTable.tsx';
import { EmptyState } from '../../components/common/EmptyState.tsx';
import { LoadingState } from '../../components/common/LoadingState.tsx';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import type { AdminField } from './resourceConfigResolver.ts';
import { getAdminResourceConfig, isAdminFieldVisible, toFormValues } from './resourceConfigResolver.ts';
import {
  filterRecordsByJurisdiction,
  supportsJurisdictionFilter,
} from './jurisdictionListFilter.ts';
import { getJurisdictionOptions, type JurisdictionOptionSource } from './jurisdictions.ts';
import { TenantEntitlementPolicySummary } from './TenantEntitlementPolicySummary.tsx';
import { TenantLeaveTypeName } from './TenantLeaveTypeName.tsx';
import { shouldHideTenantInternalId } from './tenantInternalIdVisibility.ts';

interface LeaveMasterIdentity {
  platformAdmin?: boolean;
}

const displayValue = (field: AdminField, value: unknown) => {
  if (typeof value === 'boolean') return value ? <Tag color="green">Yes</Tag> : <Tag>No</Tag>;
  if (field.options) {
    const option = field.options.find((item) => item.value === value);
    if (option) return option.label;
  }
  if (value && typeof value === 'object') return JSON.stringify(value);
  return String(value ?? '—');
};

export const ResourceListPage = () => {
  const { resource } = useResource();
  const config = getAdminResourceConfig(resource?.name);
  const { data: identity } = useGetIdentity<LeaveMasterIdentity>();
  const platformAdmin = Boolean(identity?.platformAdmin);
  const [search, setSearch] = useState('');
  const [jurisdictionFilter, setJurisdictionFilter] = useState<string>();
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const navigate = useNavigate();
  const { message } = App.useApp();
  const jurisdictionFilterEnabled = supportsJurisdictionFilter(config?.name);
  const listQuery = useList({ resource: config?.name ?? '', pagination: { mode: 'off' }, queryOptions: { enabled: Boolean(config) } });
  const jurisdictionsQuery = useList({
    resource: 'jurisdictions',
    pagination: { mode: 'off' },
    queryOptions: { enabled: jurisdictionFilterEnabled },
  });
  const tenantJurisdictionsQuery = useList({
    resource: 'tenant-jurisdictions',
    pagination: { mode: 'off' },
    queryOptions: { enabled: jurisdictionFilterEnabled && !platformAdmin },
  });
  const { data: canCreate } = useCan({ resource: config?.name ?? '', action: 'create' });
  const { data: canEdit } = useCan({ resource: config?.name ?? '', action: 'edit' });
  const { data: canDelete } = useCan({ resource: config?.name ?? '', action: 'delete' });
  const { mutateAsync: deleteRecord } = useDelete();

  const jurisdictionOptions = useMemo(() => {
    if (!jurisdictionFilterEnabled) return [];
    const jurisdictions = (jurisdictionsQuery.data?.data ?? []) as unknown as JurisdictionOptionSource[];
    const options = getJurisdictionOptions(jurisdictions);
    if (platformAdmin) return options;

    const tenantJurisdictionIds = new Set(
      ((tenantJurisdictionsQuery.data?.data ?? []) as Record<string, unknown>[])
        .map((record) => String(record.jurisdictionId ?? '').trim())
        .filter(Boolean),
    );
    return options.filter((option) => tenantJurisdictionIds.has(option.value));
  }, [jurisdictionFilterEnabled, jurisdictionsQuery.data, platformAdmin, tenantJurisdictionsQuery.data]);

  const rows = useMemo(() => {
    if (!config) return [];
    const records = ((listQuery.data?.data ?? []) as Record<string, unknown>[]).map((record) => toFormValues(config, record));
    const jurisdictionFiltered = supportsJurisdictionFilter(config.name)
      ? filterRecordsByJurisdiction(config.name, records, jurisdictionFilter)
      : records;
    if (!search.trim()) return jurisdictionFiltered;
    const needle = search.toLowerCase();
    return jurisdictionFiltered.filter((record) => Object.values(record).some((value) => String(value ?? '').toLowerCase().includes(needle)));
  }, [config, jurisdictionFilter, listQuery.data, search]);

  if (!config) return <EmptyState title="Resource unavailable" description="No administration configuration exists for this resource." />;
  if (listQuery.isLoading) return <LoadingState />;

  const remove = async (id: string) => {
    try {
      await deleteRecord({ resource: config.name, id });
      message.success(`${config.singular} deleted`);
    } catch {
      message.error(`Unable to delete ${config.singular.toLowerCase()}`);
    }
  };

  const columns: TableProps<Record<string, unknown>>['columns'] = config.fields
    .filter((field) => field.list
      && isAdminFieldVisible(field, platformAdmin)
      && !shouldHideTenantInternalId(config.name, config.idField, field.name, platformAdmin))
    .map((field) => ({
      title: field.label,
      dataIndex: field.name,
      sorter: (a, b) => String(a[field.name] ?? '').localeCompare(String(b[field.name] ?? ''), undefined, { numeric: true }),
      render: (value: unknown) => config.name === 'leave-entitlement-policies' && field.name === 'leaveTypeId' && !platformAdmin
        ? <TenantLeaveTypeName leaveTypeId={String(value ?? '')} />
        : config.name === 'leave-entitlement-policy-eligibility-rules' && field.name === 'policyId' && !platformAdmin
          ? <TenantEntitlementPolicySummary policyId={String(value ?? '')} />
          : displayValue(field, value),
    }));

  columns?.push({
    title: 'Actions', dataIndex: '__actions', sorter: () => 0,
    render: (_: unknown, row: Record<string, unknown>) => {
      const id = String(row[config.idField] ?? '');
      return (
        <Space wrap>
          <Link to={`/${config.name}/show/${encodeURIComponent(id)}`}>View</Link>
          {canEdit?.can && config.editable !== false ? <Link to={`/${config.name}/edit/${encodeURIComponent(id)}`}>Edit</Link> : null}
          {canDelete?.can && config.deletable !== false ? (
            <Popconfirm title={`Delete ${config.singular.toLowerCase()}?`} description="This action cannot be undone." onConfirm={() => remove(id)}>
              <Button danger type="link" size="small">Delete</Button>
            </Popconfirm>
          ) : null}
        </Space>
      );
    },
  });

  const createButton = canCreate?.can && config.creatable !== false
    ? <Button type="primary" onClick={() => navigate(`/${config.name}/create`)}>Create</Button>
    : null;

  const jurisdictionSelect = jurisdictionFilterEnabled ? (
    <Select
      aria-label="Jurisdiction"
      value={jurisdictionFilter ?? '__ALL__'}
      onChange={(value) => {
        setJurisdictionFilter(value === '__ALL__' ? undefined : value);
        setCurrentPage(1);
      }}
      loading={jurisdictionsQuery.isLoading || (!platformAdmin && tenantJurisdictionsQuery.isLoading)}
      disabled={jurisdictionsQuery.isError || (!platformAdmin && tenantJurisdictionsQuery.isError)}
      options={[
        { label: 'All jurisdictions', value: '__ALL__' },
        ...jurisdictionOptions,
      ]}
      showSearch
      optionFilterProp="label"
      style={{ minWidth: 220 }}
    />
  ) : null;

  return (
    <PageContainer>
      <PageHeader title={config.label} subtitle={`Manage ${config.label.toLowerCase()}.`} extra={createButton} />
      <Space wrap>
        <Input.Search
          allowClear
          placeholder={`Search ${config.label.toLowerCase()}`}
          onChange={(event) => {
            setSearch(event.target.value);
            setCurrentPage(1);
          }}
          style={{ maxWidth: 360 }}
        />
        {jurisdictionSelect}
      </Space>
      <DataTable<Record<string, unknown>>
        rowKey={(row) => String(row[config.idField])}
        dataSource={rows}
        columns={columns}
        pagination={{
          current: currentPage,
          pageSize,
          showSizeChanger: true,
          onChange: (page, nextPageSize) => {
            setCurrentPage(nextPageSize !== pageSize ? 1 : page);
            setPageSize(nextPageSize);
          },
        }}
      />
    </PageContainer>
  );
};

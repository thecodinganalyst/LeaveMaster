import { useQuery } from '@tanstack/react-query';
import { Form, Input, Select, Switch, Typography } from 'antd';

import { apiFetch } from '../../api/http.ts';
import { getJurisdictionOptions, type JurisdictionOptionSource } from './jurisdictions.ts';

interface TenantJurisdictionRecord {
  jurisdictionId: string;
}

const loadJurisdictions = () => apiFetch<JurisdictionOptionSource[]>('/api/jurisdictions');
const loadTenantJurisdictions = () => apiFetch<TenantJurisdictionRecord[]>('/api/tenant-jurisdictions');

export const TenantJurisdictionFormFields = () => {
  const jurisdictionsQuery = useQuery({
    queryKey: ['jurisdictions', 'options'],
    queryFn: loadJurisdictions,
    staleTime: 5 * 60 * 1000,
  });
  const tenantJurisdictionsQuery = useQuery({
    queryKey: ['tenant-jurisdictions', 'current'],
    queryFn: loadTenantJurisdictions,
  });

  const existing = new Set((tenantJurisdictionsQuery.data ?? []).map((item) => item.jurisdictionId));
  const availableOptions = getJurisdictionOptions(jurisdictionsQuery.data ?? [])
    .filter((option) => !existing.has(String(option.value)));
  const loading = jurisdictionsQuery.isLoading || tenantJurisdictionsQuery.isLoading;
  const failed = jurisdictionsQuery.isError || tenantJurisdictionsQuery.isError;

  return (
    <>
      <Typography.Paragraph type="secondary">
        Add another jurisdiction to this tenant. Existing tenant jurisdictions are excluded from the list.
      </Typography.Paragraph>
      <Form.Item name="jurisdictionId" label="Jurisdiction" rules={[{ required: true, message: 'Jurisdiction is required' }]}>
        <Select
          loading={loading}
          disabled={failed}
          options={availableOptions}
          showSearch
          optionFilterProp="label"
          placeholder={failed ? 'Unable to load jurisdictions' : 'Select a jurisdiction'}
        />
      </Form.Item>
      <Form.Item name="includePublicHolidays" label="Add public holidays from template" valuePropName="checked">
        <Switch />
      </Form.Item>
      <Form.Item
        name="includeLeaveConfiguration"
        label="Add leave types, entitlement policies and eligibility rules from template"
        valuePropName="checked"
      >
        <Switch />
      </Form.Item>
      <Form.Item name="calendarStart" label="Calendar start" rules={[{ required: true, message: 'Calendar start is required' }]}>
        <Input type="date" />
      </Form.Item>
      <Form.Item name="calendarEnd" label="Calendar end" rules={[{ required: true, message: 'Calendar end is required' }]}>
        <Input type="date" />
      </Form.Item>
    </>
  );
};

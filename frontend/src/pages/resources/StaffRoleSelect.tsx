import { useQuery } from '@tanstack/react-query';
import { Form, Select, Typography } from 'antd';

import { apiFetch } from '../../api/http.ts';

interface RoleSource {
  id: string;
  description: string;
  active: boolean;
}

const loadRoles = () => apiFetch<RoleSource[]>('/api/staff/role-options');

export const StaffRoleSelect = () => {
  const rolesQuery = useQuery({
    queryKey: ['staff-form', 'roles'],
    queryFn: loadRoles,
    staleTime: 60_000,
  });

  const options = (rolesQuery.data ?? []).map((role) => ({
    value: role.id,
    label: role.active ? `${role.description} (${role.id})` : `${role.description} (${role.id}, inactive)`,
  }));

  return (
    <Form.Item
      name="roleIds"
      label="Roles"
      extra={<Typography.Text type="secondary">Permissions are combined from all assigned active roles.</Typography.Text>}
    >
      <Select
        mode="multiple"
        allowClear
        showSearch
        optionFilterProp="label"
        options={options}
        loading={rolesQuery.isLoading}
        disabled={rolesQuery.isError}
        placeholder={rolesQuery.isError ? 'Unable to load roles' : 'Select one or more roles'}
      />
    </Form.Item>
  );
};

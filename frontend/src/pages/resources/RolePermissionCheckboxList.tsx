import { useQuery } from '@tanstack/react-query';
import { Alert, Checkbox, Space, Spin, Typography } from 'antd';

import { apiFetch } from '../../api/http.ts';
import { getCurrentUser } from '../../auth/session.ts';

interface AppPermission {
  code: string;
  description: string;
}

interface Props {
  value?: string[];
  onChange?: (value: string[]) => void;
  disabled?: boolean;
}

const PLATFORM_ONLY_PERMISSIONS = new Set(['TENANT_READ', 'TENANT_WRITE']);
const loadRolePermissions = () => apiFetch<AppPermission[]>('/roles/permissions');

export const RolePermissionCheckboxList = ({ value = [], onChange, disabled = false }: Props) => {
  const permissionsQuery = useQuery({
    queryKey: ['role-permissions'],
    queryFn: loadRolePermissions,
    staleTime: 5 * 60 * 1000,
  });
  const currentUserQuery = useQuery({
    queryKey: ['current-user'],
    queryFn: () => getCurrentUser(),
    staleTime: 5 * 60 * 1000,
  });

  if (permissionsQuery.isLoading || currentUserQuery.isLoading) return <Spin size="small" />;
  if (permissionsQuery.isError || currentUserQuery.isError) {
    return <Alert type="error" showIcon message="Unable to load permissions" />;
  }

  const platformAdmin = currentUserQuery.data?.platformAdmin === true;
  const permissions = [...(permissionsQuery.data ?? [])]
    .filter((permission) => platformAdmin || !PLATFORM_ONLY_PERMISSIONS.has(permission.code))
    .sort((left, right) => left.code.localeCompare(right.code));
  if (permissions.length === 0) return <Typography.Text type="secondary">No permissions available.</Typography.Text>;

  return (
    <Checkbox.Group
      value={value.filter((permission) => platformAdmin || !PLATFORM_ONLY_PERMISSIONS.has(permission))}
      disabled={disabled}
      onChange={(checkedValues) => onChange?.(checkedValues.map(String))}
      style={{ width: '100%' }}
    >
      <Space direction="vertical" size="small">
        {permissions.map((permission) => (
          <Checkbox key={permission.code} value={permission.code}>
            <Typography.Text strong>{permission.code}</Typography.Text>
            <Typography.Text type="secondary"> — {permission.description}</Typography.Text>
          </Checkbox>
        ))}
      </Space>
    </Checkbox.Group>
  );
};

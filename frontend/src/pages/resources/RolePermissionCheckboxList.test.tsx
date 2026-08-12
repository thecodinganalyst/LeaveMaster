import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../../api/http.ts';
import { getCurrentUser } from '../../auth/session.ts';
import { RolePermissionCheckboxList } from './RolePermissionCheckboxList.tsx';

vi.mock('../../api/http.ts', () => ({ apiFetch: vi.fn() }));
vi.mock('../../auth/session.ts', () => ({ getCurrentUser: vi.fn() }));

const mockedApiFetch = vi.mocked(apiFetch);
const mockedGetCurrentUser = vi.mocked(getCurrentUser);

const renderWithQueryClient = (ui: ReactNode) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

describe('RolePermissionCheckboxList', () => {
  beforeEach(() => {
    mockedApiFetch.mockReset();
    mockedGetCurrentUser.mockReset();
    mockedApiFetch.mockResolvedValue([
      { code: 'STAFF_READ', description: 'Read staff records' },
      { code: 'LEAVE_APPLICATION_APPROVE', description: 'Approve leave applications' },
    ]);
    mockedGetCurrentUser.mockResolvedValue({
      loginName: 'tenant-admin',
      staffId: null,
      tenantId: 'tenant-a',
      active: true,
      platformAdmin: false,
      authorities: ['ROLE_MANAGE'],
    });
  });

  it('shows every tenant-assignable permission and pre-selects assigned permissions', async () => {
    renderWithQueryClient(<RolePermissionCheckboxList value={['STAFF_READ']} />);

    const staffRead = await screen.findByRole('checkbox', { name: /STAFF_READ/ });
    const approveLeave = screen.getByRole('checkbox', { name: /LEAVE_APPLICATION_APPROVE/ });

    expect(mockedApiFetch).toHaveBeenCalledWith('/api/roles/permissions');
    expect(staffRead).toBeChecked();
    expect(approveLeave).not.toBeChecked();
  });

  it('hides tenant management permissions from non platform admins even if the API returns them', async () => {
    mockedApiFetch.mockResolvedValue([
      { code: 'STAFF_READ', description: 'Read staff records' },
      { code: 'TENANT_READ', description: 'Read tenants' },
      { code: 'TENANT_WRITE', description: 'Manage tenants' },
    ]);

    renderWithQueryClient(<RolePermissionCheckboxList value={['STAFF_READ', 'TENANT_READ']} />);

    expect(await screen.findByRole('checkbox', { name: /STAFF_READ/ })).toBeChecked();
    expect(screen.queryByRole('checkbox', { name: /TENANT_READ/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: /TENANT_WRITE/ })).not.toBeInTheDocument();
  });

  it('shows tenant management permissions to platform admins', async () => {
    mockedGetCurrentUser.mockResolvedValue({
      loginName: 'platformadmin',
      staffId: null,
      tenantId: null,
      active: true,
      platformAdmin: true,
      authorities: ['ROLE_MANAGE', 'TENANT_READ', 'TENANT_WRITE'],
    });
    mockedApiFetch.mockResolvedValue([
      { code: 'TENANT_READ', description: 'Read tenants' },
      { code: 'TENANT_WRITE', description: 'Manage tenants' },
    ]);

    renderWithQueryClient(<RolePermissionCheckboxList />);

    expect(await screen.findByRole('checkbox', { name: /TENANT_READ/ })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: /TENANT_WRITE/ })).toBeInTheDocument();
  });

  it('returns the selected permission codes when a permission is toggled', async () => {
    const onChange = vi.fn();
    renderWithQueryClient(<RolePermissionCheckboxList value={['STAFF_READ']} onChange={onChange} />);

    const approveLeave = await screen.findByRole('checkbox', { name: /LEAVE_APPLICATION_APPROVE/ });
    fireEvent.click(approveLeave);

    expect(onChange).toHaveBeenCalledWith(['STAFF_READ', 'LEAVE_APPLICATION_APPROVE']);
  });

  it('disables permission changes in read-only mode', async () => {
    renderWithQueryClient(<RolePermissionCheckboxList value={['STAFF_READ']} disabled />);

    expect(await screen.findByRole('checkbox', { name: /STAFF_READ/ })).toBeDisabled();
    expect(screen.getByRole('checkbox', { name: /LEAVE_APPLICATION_APPROVE/ })).toBeDisabled();
  });
});

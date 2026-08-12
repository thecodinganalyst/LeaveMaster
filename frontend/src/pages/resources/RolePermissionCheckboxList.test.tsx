import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../../api/http.ts';
import { RolePermissionCheckboxList } from './RolePermissionCheckboxList.tsx';

vi.mock('../../api/http.ts', () => ({ apiFetch: vi.fn() }));

const mockedApiFetch = vi.mocked(apiFetch);

const renderWithQueryClient = (ui: ReactNode) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

describe('RolePermissionCheckboxList', () => {
  beforeEach(() => {
    mockedApiFetch.mockReset();
    mockedApiFetch.mockResolvedValue([
      { code: 'STAFF_READ', description: 'Read staff records' },
      { code: 'LEAVE_APPLICATION_APPROVE', description: 'Approve leave applications' },
    ]);
  });

  it('shows every permission and pre-selects assigned permissions', async () => {
    renderWithQueryClient(<RolePermissionCheckboxList value={['STAFF_READ']} />);

    const staffRead = await screen.findByRole('checkbox', { name: /STAFF_READ/ });
    const approveLeave = screen.getByRole('checkbox', { name: /LEAVE_APPLICATION_APPROVE/ });

    expect(mockedApiFetch).toHaveBeenCalledWith('/roles/permissions');
    expect(staffRead).toBeChecked();
    expect(approveLeave).not.toBeChecked();
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

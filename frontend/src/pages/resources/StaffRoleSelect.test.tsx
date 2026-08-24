import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { Form } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../../api/http.ts';
import { StaffRoleSelect } from './StaffRoleSelect.tsx';

vi.mock('../../api/http.ts', () => ({ apiFetch: vi.fn() }));

const mockedApiFetch = vi.mocked(apiFetch);

const renderSelect = (initialRoleIds: string[] = []) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <Form initialValues={{ roleIds: initialRoleIds }}>
        <StaffRoleSelect />
      </Form>
    </QueryClientProvider>,
  );
};

describe('StaffRoleSelect', () => {
  beforeEach(() => {
    mockedApiFetch.mockReset();
    mockedApiFetch.mockResolvedValue([
      { id: 'EMPLOYEE', description: 'Employee', active: true },
      { id: 'APPROVER', description: 'Leave approver', active: true },
    ]);
  });

  it('loads tenant roles and supports existing multiple assignments', async () => {
    renderSelect(['EMPLOYEE', 'APPROVER']);

    expect(mockedApiFetch).toHaveBeenCalledWith('/api/roles');
    expect(await screen.findByText('Employee (EMPLOYEE)')).toBeInTheDocument();
    expect(screen.getByText('Leave approver (APPROVER)')).toBeInTheDocument();
    expect(screen.getByText(/Permissions are combined from all assigned active roles/)).toBeInTheDocument();
  });
});

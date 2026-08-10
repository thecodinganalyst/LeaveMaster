import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getCurrentUser } from '../../auth/session.ts';
import { getLeaveBalances, getVisibleLeave } from '../../features/leave/leaveApi.ts';
import { DashboardPage } from './DashboardPage.tsx';

vi.mock('@refinedev/core', () => ({
  useCan: vi.fn(() => ({ data: { can: true } })),
  useList: vi.fn(() => ({
    data: {
      data: [
        { id: 'T1', name: 'Tenant One', status: 'ACTIVE', startDate: '2026-01-01' },
        { id: 'T2', name: 'Tenant Two', status: 'DORMANT', startDate: '2026-02-01' },
      ],
    },
    isLoading: false,
    isError: false,
  })),
}));

vi.mock('../../auth/session.ts', () => ({ getCurrentUser: vi.fn() }));
vi.mock('../../features/leave/leaveApi.ts', async () => {
  const actual = await vi.importActual<typeof import('../../features/leave/leaveApi.ts')>('../../features/leave/leaveApi.ts');
  return { ...actual, getLeaveBalances: vi.fn(), getVisibleLeave: vi.fn() };
});

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'employee', staffId: 'S1', tenantId: 'T1', active: true, authorities: ['LEAVE_APPLICATION_READ'],
    });
    vi.mocked(getLeaveBalances).mockResolvedValue([]);
    vi.mocked(getVisibleLeave).mockResolvedValue([]);
  });

  it('renders the employee leave dashboard and loads server-backed leave data', async () => {
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'My Leave' })).toBeInTheDocument();
    expect(await screen.findByText('No upcoming leave.')).toBeInTheDocument();
    expect(getLeaveBalances).toHaveBeenCalledWith('S1');
    expect(getVisibleLeave).toHaveBeenCalledWith('S1');
  });

  it('renders tenant administration instead of personal leave for PlatformAdmin', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'PlatformAdmin', staffId: null, tenantId: null, active: true, authorities: ['TENANT_READ', 'TENANT_WRITE'],
    });

    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>,
    );

    expect(await screen.findByRole('heading', { name: 'Tenant Administration' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Create tenant' })).toHaveAttribute('href', '/tenants/create');
    expect(screen.getByText('Tenant One')).toBeInTheDocument();
    expect(screen.getByText('Tenant Two')).toBeInTheDocument();
    expect(getLeaveBalances).not.toHaveBeenCalled();
    expect(getVisibleLeave).not.toHaveBeenCalled();
  });
});

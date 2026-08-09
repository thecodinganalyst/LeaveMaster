import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getCurrentUser } from '../../auth/session.ts';
import { getLeaveBalances, getVisibleLeave } from '../../features/leave/leaveApi.ts';
import { DashboardPage } from './DashboardPage.tsx';

vi.mock('@refinedev/core', () => ({
  useCan: vi.fn(() => ({ data: { can: true } })),
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
});

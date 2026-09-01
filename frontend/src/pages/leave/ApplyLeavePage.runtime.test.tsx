import { App } from 'antd';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getCurrentUser } from '../../auth/session.ts';
import { getLeaveTypes } from '../../features/leave/leaveApi.ts';
import { ApplyLeavePage } from './ApplyLeavePage.tsx';

vi.mock('../../auth/session.ts', () => ({ getCurrentUser: vi.fn() }));
vi.mock('../../features/leave/leaveApi.ts', async () => {
  const actual = await vi.importActual<typeof import('../../features/leave/leaveApi.ts')>('../../features/leave/leaveApi.ts');
  return { ...actual, getLeaveTypes: vi.fn() };
});

const mockedGetCurrentUser = vi.mocked(getCurrentUser);
const mockedGetLeaveTypes = vi.mocked(getLeaveTypes);

const renderPage = () => render(
  <MemoryRouter>
    <App>
      <ApplyLeavePage />
    </App>
  </MemoryRouter>,
);

describe('ApplyLeavePage runtime response resilience', () => {
  beforeEach(() => {
    mockedGetCurrentUser.mockResolvedValue({
      loginName: 'staff',
      staffId: 'S1',
      tenantId: 'T1',
      active: true,
      authorities: ['LEAVE_APPLICATION_WRITE'],
    });
    mockedGetLeaveTypes.mockReset();
  });

  it('keeps the route rendered when leave types returns null with HTTP 200 semantics', async () => {
    mockedGetLeaveTypes.mockResolvedValue(null as never);

    renderPage();

    expect(await screen.findByText('Apply for leave')).toBeInTheDocument();
    expect(await screen.findByText('Leave types could not be loaded because the server returned an unexpected response.')).toBeInTheDocument();
    expect(screen.getByText('Leave type')).toBeInTheDocument();
  });

  it('keeps the route rendered when leave types returns an unexpected object', async () => {
    mockedGetLeaveTypes.mockResolvedValue({ content: [] } as never);

    renderPage();

    await waitFor(() => expect(mockedGetLeaveTypes).toHaveBeenCalledTimes(1));
    expect(screen.getByText('Apply for leave')).toBeInTheDocument();
    expect(screen.getByText('Leave types could not be loaded because the server returned an unexpected response.')).toBeInTheDocument();
  });
});

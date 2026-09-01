import { App } from 'antd';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { getCurrentUser } from '../../auth/session.ts';
import { ApplyLeavePage } from './ApplyLeavePage.tsx';

vi.mock('../../auth/session.ts', () => ({ getCurrentUser: vi.fn() }));
vi.mock('../../features/leave/leaveApi.ts', async () => {
  const actual = await vi.importActual<typeof import('../../features/leave/leaveApi.ts')>('../../features/leave/leaveApi.ts');
  return {
    ...actual,
    applyForLeave: vi.fn(),
    getLeaveApplicationPolicyMetadata: vi.fn(),
    getLeaveTypes: vi.fn(),
  };
});

describe('ApplyLeavePage render lifecycle', () => {
  it('keeps the form mounted while the session is still initializing', () => {
    vi.mocked(getCurrentUser).mockReturnValue(new Promise(() => {}));

    render(
      <MemoryRouter>
        <App>
          <ApplyLeavePage />
        </App>
      </MemoryRouter>,
    );

    expect(screen.getByText('Apply for leave')).toBeInTheDocument();
    expect(screen.getByText('Loading leave application…')).toBeInTheDocument();
    expect(screen.getByLabelText('Leave type')).toBeInTheDocument();
    expect(screen.getByLabelText('From date')).toBeInTheDocument();
    expect(screen.getByLabelText('To date')).toBeInTheDocument();
  });
});

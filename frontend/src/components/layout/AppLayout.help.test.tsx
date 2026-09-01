import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AppLayout } from './AppLayout.tsx';

const mocks = vi.hoisted(() => ({
  useCan: vi.fn(),
  logout: vi.fn(),
  breakpoint: { lg: true },
}));

vi.mock('@refinedev/core', () => ({
  useCan: mocks.useCan,
  useGetIdentity: () => ({ data: { platformAdmin: false } }),
  useLogout: () => ({ mutate: mocks.logout }),
}));

vi.mock('antd', async () => {
  const actual = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...actual,
    Grid: { ...actual.Grid, useBreakpoint: () => mocks.breakpoint },
  };
});

const renderAt = (path: string) => render(
  <MemoryRouter initialEntries={[path]}>
    <AppLayout><div>Page content</div></AppLayout>
  </MemoryRouter>,
);

describe('AppLayout help links', () => {
  beforeEach(() => {
    mocks.useCan.mockReturnValue({ data: { can: false } });
    mocks.breakpoint.lg = true;
  });

  it('shows the global User Guide as an external link on desktop', () => {
    renderAt('/tenants');
    const link = screen.getByRole('link', { name: 'Open LeaveMaestro User Guide' });
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', expect.stringContaining('noopener'));
    expect(link).toHaveAttribute('href', expect.stringContaining('/LeaveMaster/user-guide/'));
  });

  it('links Apply Leave directly to employee instructions', () => {
    renderAt('/leave-requests/apply');
    const link = screen.getByRole('link', { name: 'How to apply for leave' });
    expect(link).toHaveAttribute('href', expect.stringContaining('/user-guide/employee/#apply-for-leave'));
    expect(link).toHaveAttribute('target', '_blank');
  });

  it('links manager approvals to manager instructions', () => {
    renderAt('/approvals');
    expect(screen.getByRole('link', { name: 'Help with leave approvals' }))
      .toHaveAttribute('href', expect.stringContaining('/user-guide/manager/#review-and-decide-leave-requests'));
  });

  it('links HR staff creation and Admin leave types to their role guides', () => {
    const { unmount } = renderAt('/employees/create');
    expect(screen.getByRole('link', { name: 'Help creating staff' }))
      .toHaveAttribute('href', expect.stringContaining('/user-guide/hr/#create-staff'));
    unmount();

    renderAt('/leave-types');
    expect(screen.getByRole('link', { name: 'Help with leave types' }))
      .toHaveAttribute('href', expect.stringContaining('/user-guide/admin/#manage-leave-types'));
  });

  it('keeps the global User Guide discoverable in the mobile menu', () => {
    mocks.breakpoint.lg = false;
    renderAt('/leave-requests/apply');
    fireEvent.click(screen.getByRole('button', { name: 'Open menu' }));
    const link = screen.getByRole('link', { name: 'User Guide' });
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('href', expect.stringContaining('/LeaveMaster/user-guide/'));
  });
});

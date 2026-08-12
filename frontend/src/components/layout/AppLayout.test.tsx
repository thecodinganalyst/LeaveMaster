import { render, screen } from '@testing-library/react';
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
  useLogout: () => ({ mutate: mocks.logout }),
}));

vi.mock('antd', async () => {
  const actual = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...actual,
    Grid: {
      ...actual.Grid,
      useBreakpoint: () => mocks.breakpoint,
    },
  };
});

describe('AppLayout navigation', () => {
  beforeEach(() => {
    mocks.useCan.mockReset();
    mocks.logout.mockReset();
    mocks.breakpoint.lg = true;
  });

  it('shows Tenants navigation when TENANT_READ access is available', () => {
    mocks.useCan.mockImplementation(({ resource }: { resource: string }) => ({
      data: { can: resource === 'tenants' },
    }));

    render(
      <MemoryRouter>
        <AppLayout><div>Page content</div></AppLayout>
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: 'Tenants' })).toHaveAttribute('href', '/tenants');
    expect(screen.queryByRole('link', { name: 'Staff' })).not.toBeInTheDocument();
  });

  it('hides Tenants navigation when tenant access is absent', () => {
    mocks.useCan.mockReturnValue({ data: { can: false } });

    render(
      <MemoryRouter>
        <AppLayout><div>Page content</div></AppLayout>
      </MemoryRouter>,
    );

    expect(screen.queryByRole('link', { name: 'Tenants' })).not.toBeInTheDocument();
  });

  it('renders a high-contrast mobile menu button hook on small screens', () => {
    mocks.breakpoint.lg = false;
    mocks.useCan.mockReturnValue({ data: { can: false } });

    render(
      <MemoryRouter>
        <AppLayout><div>Page content</div></AppLayout>
      </MemoryRouter>,
    );

    expect(screen.getByRole('button', { name: 'Open menu' })).toHaveClass('mobile-menu-button');
  });

  it('does not render the mobile menu button on desktop', () => {
    mocks.useCan.mockReturnValue({ data: { can: false } });

    render(
      <MemoryRouter>
        <AppLayout><div>Page content</div></AppLayout>
      </MemoryRouter>,
    );

    expect(screen.queryByRole('button', { name: 'Open menu' })).not.toBeInTheDocument();
  });
});

import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AppLayout } from './AppLayout.tsx';

const mocks = vi.hoisted(() => ({
  useCan: vi.fn(),
  logout: vi.fn(),
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
      useBreakpoint: () => ({ lg: true }),
    },
  };
});

describe('AppLayout tenant navigation', () => {
  beforeEach(() => {
    mocks.useCan.mockReset();
    mocks.logout.mockReset();
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
});

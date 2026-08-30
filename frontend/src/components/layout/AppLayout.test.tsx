import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AppLayout } from './AppLayout.tsx';

const mocks = vi.hoisted(() => ({
  useCan: vi.fn(),
  logout: vi.fn(),
  breakpoint: { lg: true },
  identity: { platformAdmin: false },
}));

vi.mock('@refinedev/core', () => ({
  useCan: mocks.useCan,
  useGetIdentity: () => ({ data: mocks.identity }),
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
    mocks.identity.platformAdmin = false;
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

  it('shows Contact Enquiries navigation for platform administrators', () => {
    mocks.identity.platformAdmin = true;
    mocks.useCan.mockReturnValue({ data: { can: false } });

    render(
      <MemoryRouter initialEntries={['/contact-enquiries']}>
        <AppLayout><div>Page content</div></AppLayout>
      </MemoryRouter>,
    );

    const link = screen.getByRole('link', { name: 'Contact Enquiries' });
    expect(link).toHaveAttribute('href', '/contact-enquiries');
    expect(link.closest('.ant-menu-item')).toHaveClass('ant-menu-item-selected');
  });

  it('hides Contact Enquiries navigation from tenant-scoped users', () => {
    mocks.useCan.mockReturnValue({ data: { can: true } });

    render(
      <MemoryRouter>
        <AppLayout><div>Page content</div></AppLayout>
      </MemoryRouter>,
    );

    expect(screen.queryByRole('link', { name: 'Contact Enquiries' })).not.toBeInTheDocument();
  });

  it('shows Contact Enquiries in the mobile drawer for platform administrators', () => {
    mocks.breakpoint.lg = false;
    mocks.identity.platformAdmin = true;
    mocks.useCan.mockReturnValue({ data: { can: false } });

    render(
      <MemoryRouter>
        <AppLayout><div>Page content</div></AppLayout>
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Open menu' }));

    expect(screen.getByRole('link', { name: 'Contact Enquiries' })).toHaveAttribute('href', '/contact-enquiries');
  });

  it('shows Public Holiday Templates without exposing tenant Leave Calendars', () => {
    mocks.useCan.mockImplementation(({ resource }: { resource: string }) => ({
      data: { can: resource === 'public-holidays' },
    }));

    render(
      <MemoryRouter>
        <AppLayout><div>Page content</div></AppLayout>
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: 'Public Holiday Templates' })).toHaveAttribute('href', '/public-holidays');
    expect(screen.queryByRole('link', { name: 'Leave Calendars' })).not.toBeInTheDocument();
  });

  it('makes Leave Types the tenant entitlement entry point', () => {
    mocks.useCan.mockImplementation(({ resource }: { resource: string }) => ({
      data: { can: ['leave-types', 'leave-entitlement-policies', 'leave-entitlement-policy-eligibility-rules'].includes(resource) },
    }));

    render(
      <MemoryRouter>
        <AppLayout><div>Page content</div></AppLayout>
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: 'Leave Types' })).toHaveAttribute('href', '/leave-types');
    expect(screen.queryByRole('link', { name: 'Entitlement Policies' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Eligibility Rules' })).not.toBeInTheDocument();
  });

  it('retains standalone entitlement navigation for platform administrators', () => {
    mocks.identity.platformAdmin = true;
    mocks.useCan.mockImplementation(({ resource }: { resource: string }) => ({
      data: { can: ['leave-types', 'leave-entitlement-policies', 'leave-entitlement-policy-eligibility-rules'].includes(resource) },
    }));

    render(
      <MemoryRouter>
        <AppLayout><div>Page content</div></AppLayout>
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: 'Leave Types' })).toHaveAttribute('href', '/leave-types');
    expect(screen.getByRole('link', { name: 'Entitlement Policies' })).toHaveAttribute('href', '/leave-entitlement-policies');
    expect(screen.getByRole('link', { name: 'Eligibility Rules' })).toHaveAttribute('href', '/leave-entitlement-policy-eligibility-rules');
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

  it('moves security and change password actions into the mobile menu', () => {
    mocks.breakpoint.lg = false;
    mocks.useCan.mockReturnValue({ data: { can: false } });

    render(
      <MemoryRouter>
        <AppLayout><div>Page content</div></AppLayout>
      </MemoryRouter>,
    );

    expect(screen.queryByRole('button', { name: 'Security' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Change password' })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Open menu' }));

    expect(screen.getByRole('link', { name: 'Security' })).toHaveAttribute('href', '/account/security');
    expect(screen.getByRole('link', { name: 'Change Password' })).toHaveAttribute('href', '/account/change-password');
  });

  it('keeps security and change password actions in the desktop header', () => {
    mocks.useCan.mockReturnValue({ data: { can: false } });

    render(
      <MemoryRouter>
        <AppLayout><div>Page content</div></AppLayout>
      </MemoryRouter>,
    );

    expect(screen.getByRole('button', { name: 'Security' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Change password' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Security' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Change Password' })).not.toBeInTheDocument();
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

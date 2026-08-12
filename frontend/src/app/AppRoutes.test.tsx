import type { AuthBindings } from '@refinedev/core';
import { Refine } from '@refinedev/core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import routerBindings from '@refinedev/react-router-v6';
import { describe, expect, it, vi } from 'vitest';

import { AppRoutes } from './AppRoutes.tsx';

vi.mock('../components/layout/AppLayout.tsx', () => ({
  AppLayout: ({ children }: { children: ReactNode }) => <div data-testid="app-layout">{children}</div>,
}));

vi.mock('../components/common/ErrorState.tsx', () => ({
  ErrorState: ({ title }: { title: string }) => <div>{title}</div>,
}));

vi.mock('../pages/auth/LoginPage.tsx', () => ({
  LoginPage: () => <div>Login page</div>,
}));

vi.mock('../pages/dashboard/DashboardPage.tsx', () => ({
  DashboardPage: () => <div>Dashboard page</div>,
}));

vi.mock('../pages/leave/ApplyLeavePage.tsx', () => ({
  ApplyLeavePage: () => <div>Apply leave page</div>,
}));

vi.mock('../pages/leave/ApprovalInboxPage.tsx', () => ({
  ApprovalInboxPage: () => <div>Approval inbox page</div>,
}));

vi.mock('../pages/leave/LeaveDetailsPage.tsx', () => ({
  LeaveDetailsPage: () => <div>Leave details page</div>,
}));

vi.mock('../pages/leave/MyLeavePage.tsx', () => ({
  MyLeavePage: () => <div>My leave page</div>,
}));

vi.mock('../pages/resources/ResourceCreatePage.tsx', () => ({
  ResourceCreatePage: () => <div>Resource create page</div>,
}));

vi.mock('../pages/resources/ResourceEditPage.tsx', () => ({
  ResourceEditPage: () => <div>Resource edit page</div>,
}));

vi.mock('../pages/resources/ResourceListPage.tsx', () => ({
  ResourceListPage: () => <div>Resource list page</div>,
}));

vi.mock('../pages/resources/ResourceShowPage.tsx', () => ({
  ResourceShowPage: () => <div>Resource show page</div>,
}));

const LocationProbe = () => {
  const location = useLocation();
  return (
    <>
      <div data-testid="location-path">{location.pathname}</div>
      <div data-testid="location-to">{new URLSearchParams(location.search).get('to') ?? ''}</div>
    </>
  );
};

const renderRoutes = (initialPath: string, authenticated: boolean, checkOverride?: AuthBindings['check']) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  const authProvider: AuthBindings = {
    login: async () => ({ success: true, redirectTo: '/' }),
    logout: async () => ({ success: true, redirectTo: '/login' }),
    check:
      checkOverride ??
      (async () =>
        authenticated
          ? { authenticated: true }
          : { authenticated: false, redirectTo: '/login' }),
    onError: async (error) => ({ error }),
    getIdentity: async () => null,
  };

  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <QueryClientProvider client={queryClient}>
        <Refine
          authProvider={authProvider}
          routerProvider={routerBindings}
          resources={[{ name: 'dashboard', list: '/' }]}
        >
          <AppRoutes />
          <LocationProbe />
        </Refine>
      </QueryClientProvider>
    </MemoryRouter>,
  );
};

describe('AppRoutes authentication guard', () => {
  it('shows a visible loading state while direct-route authentication is being checked', async () => {
    const pendingCheck: NonNullable<AuthBindings['check']> = () => new Promise<never>(() => {});

    renderRoutes('/roles/show/TENANT_ADMIN_corpsys', false, pendingCheck);

    expect(await screen.findByRole('status')).toHaveTextContent('Checking your session…');
    expect(screen.getByTestId('location-path')).toHaveTextContent('/roles/show/TENANT_ADMIN_corpsys');
    expect(screen.queryByText('Login page')).not.toBeInTheDocument();
  });

  it('redirects direct unauthenticated access to a protected screen to login', async () => {
    renderRoutes('/leave-requests/show/42', false);

    await waitFor(() => expect(screen.getByText('Login page')).toBeInTheDocument());
    expect(screen.getByTestId('location-path')).toHaveTextContent('/login');
    expect(screen.getByTestId('location-to')).toHaveTextContent('/leave-requests/show/42');
    expect(screen.queryByText('Leave details page')).not.toBeInTheDocument();
  });

  it('allows authenticated users to open protected screens directly', async () => {
    renderRoutes('/leave-requests/show/42', true);

    await waitFor(() => expect(screen.getByText('Leave details page')).toBeInTheDocument());
    expect(screen.getByTestId('location-path')).toHaveTextContent('/leave-requests/show/42');
    expect(screen.queryByText('Login page')).not.toBeInTheDocument();
  });

  it('keeps the login page public for unauthenticated users', async () => {
    renderRoutes('/login', false);

    await waitFor(() => expect(screen.getByText('Login page')).toBeInTheDocument());
    expect(screen.getByTestId('location-path')).toHaveTextContent('/login');
  });

  it('protects the not-found screen instead of exposing application screens before login', async () => {
    renderRoutes('/not-a-real-screen', false);

    await waitFor(() => expect(screen.getByText('Login page')).toBeInTheDocument());
    expect(screen.getByTestId('location-to')).toHaveTextContent('/not-a-real-screen');
    expect(screen.queryByText('Page not found')).not.toBeInTheDocument();
  });
});

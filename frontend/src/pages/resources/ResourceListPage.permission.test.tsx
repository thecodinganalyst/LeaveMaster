import { App } from 'antd';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ResourceListPage } from './ResourceListPage.tsx';

const useList = vi.fn();
const useCan = vi.fn();

vi.mock('@refinedev/core', () => ({
  useCan: (args: unknown) => useCan(args),
  useDelete: () => ({ mutateAsync: vi.fn() }),
  useGetIdentity: () => ({ data: { platformAdmin: false } }),
  useList: (args: unknown) => useList(args),
  useResource: () => ({ resource: { name: 'leave-types' } }),
}));

vi.mock('./resourceConfigResolver.ts', async () => {
  const actual = await vi.importActual<typeof import('./resourceConfigResolver.ts')>('./resourceConfigResolver.ts');
  return {
    ...actual,
    getAdminResourceConfig: () => ({
      name: 'leave-types',
      singular: 'Leave Type',
      label: 'Leave Types',
      idField: 'id',
      fields: [],
    }),
  };
});

const renderPage = () => render(
  <MemoryRouter>
    <App>
      <ResourceListPage />
    </App>
  </MemoryRouter>,
);

describe('ResourceListPage jurisdiction lookup permissions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useList.mockReturnValue({ data: { data: [] }, isLoading: false, isError: false });
  });

  it('does not enable supporting jurisdiction queries without their read permissions', () => {
    useCan.mockImplementation(({ resource }: { resource: string }) => ({
      data: { can: !['jurisdictions', 'tenant-jurisdictions'].includes(resource) },
    }));

    renderPage();

    const jurisdictionCall = useList.mock.calls.find(([args]) => args.resource === 'jurisdictions')?.[0];
    const tenantJurisdictionCall = useList.mock.calls.find(([args]) => args.resource === 'tenant-jurisdictions')?.[0];
    expect(jurisdictionCall.queryOptions.enabled).toBe(false);
    expect(tenantJurisdictionCall.queryOptions.enabled).toBe(false);
    expect(screen.queryByLabelText('Jurisdiction')).not.toBeInTheDocument();
  });

  it('enables both supporting queries when a tenant user has the required permissions', () => {
    useCan.mockReturnValue({ data: { can: true } });

    renderPage();

    const jurisdictionCall = useList.mock.calls.find(([args]) => args.resource === 'jurisdictions')?.[0];
    const tenantJurisdictionCall = useList.mock.calls.find(([args]) => args.resource === 'tenant-jurisdictions')?.[0];
    expect(jurisdictionCall.queryOptions.enabled).toBe(true);
    expect(tenantJurisdictionCall.queryOptions.enabled).toBe(true);
    expect(screen.getByLabelText('Jurisdiction')).toBeInTheDocument();
  });
});

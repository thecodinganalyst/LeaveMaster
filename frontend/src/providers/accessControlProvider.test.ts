import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getCurrentUser } from '../auth/session.ts';
import { accessControlProvider } from './accessControlProvider.ts';

vi.mock('../auth/session.ts', () => ({
  getCurrentUser: vi.fn(),
}));

describe('accessControlProvider', () => {
  beforeEach(() => {
    vi.mocked(getCurrentUser).mockReset();
  });

  it('allows actions backed by the authenticated user authorities', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'manager',
      staffId: 'S1',
      tenantId: 'T1',
      active: true,
      authorities: ['LEAVE_APPLICATION_READ', 'LEAVE_APPLICATION_APPROVE'],
    });

    await expect(
      accessControlProvider.can({ resource: 'leave-requests', action: 'list', params: {} }),
    ).resolves.toMatchObject({ can: true });
    await expect(
      accessControlProvider.can({ resource: 'leave-requests', action: 'approve', params: {} }),
    ).resolves.toMatchObject({ can: true });
  });

  it('allows PlatformAdmin tenant CRUD when tenant authorities are present', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'PlatformAdmin',
      staffId: null,
      tenantId: null,
      active: true,
      platformAdmin: true,
      authorities: ['TENANT_READ', 'TENANT_WRITE'],
    });

    await expect(accessControlProvider.can({ resource: 'tenants', action: 'list', params: {} }))
      .resolves.toMatchObject({ can: true });
    await expect(accessControlProvider.can({ resource: 'tenants', action: 'create', params: {} }))
      .resolves.toMatchObject({ can: true });
    await expect(accessControlProvider.can({ resource: 'tenants', action: 'edit', params: {} }))
      .resolves.toMatchObject({ can: true });
    await expect(accessControlProvider.can({ resource: 'tenants', action: 'delete', params: {} }))
      .resolves.toMatchObject({ can: true });
  });

  it('allows platform public holidays without granting tenant leave calendar access', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'PlatformAdmin',
      staffId: null,
      tenantId: null,
      active: true,
      platformAdmin: true,
      authorities: ['PUBLIC_HOLIDAY_READ', 'PUBLIC_HOLIDAY_WRITE'],
    });

    await expect(accessControlProvider.can({ resource: 'public-holidays', action: 'list', params: {} }))
      .resolves.toMatchObject({ can: true });
    await expect(accessControlProvider.can({ resource: 'public-holidays', action: 'edit', params: {} }))
      .resolves.toMatchObject({ can: true });
    await expect(accessControlProvider.can({ resource: 'leave-calendars', action: 'list', params: {} }))
      .resolves.toMatchObject({ can: false, reason: 'Missing LEAVE_CALENDAR_READ' });
  });

  it('keeps tenant administration read-only without TENANT_WRITE', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'tenant-reader',
      staffId: null,
      tenantId: null,
      active: true,
      authorities: ['TENANT_READ'],
    });

    await expect(accessControlProvider.can({ resource: 'tenants', action: 'list', params: {} }))
      .resolves.toMatchObject({ can: true });
    await expect(accessControlProvider.can({ resource: 'tenants', action: 'create', params: {} }))
      .resolves.toMatchObject({ can: false });
    await expect(accessControlProvider.can({ resource: 'tenants', action: 'delete', params: {} }))
      .resolves.toMatchObject({ can: false });
  });

  it('denies write actions when the required authority is absent', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'reader',
      staffId: 'S2',
      tenantId: 'T1',
      active: true,
      authorities: ['STAFF_READ'],
    });

    const result = await accessControlProvider.can({ resource: 'employees', action: 'edit', params: {} });

    expect(result.can).toBe(false);
    expect(result.reason).toContain('STAFF_WRITE');
  });

  it('denies tenant admins jurisdiction create and delete even with JURISDICTION_WRITE', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'Bravo_Admin',
      staffId: '001',
      tenantId: 'Bravo',
      active: true,
      platformAdmin: false,
      authorities: ['JURISDICTION_READ', 'JURISDICTION_WRITE'],
    });

    await expect(accessControlProvider.can({ resource: 'jurisdictions', action: 'create', params: {} }))
      .resolves.toMatchObject({ can: false, reason: 'Platform administrator access required.' });
    await expect(accessControlProvider.can({ resource: 'jurisdictions', action: 'delete', params: {} }))
      .resolves.toMatchObject({ can: false, reason: 'Platform administrator access required.' });
  });

  it('allows platform admin jurisdiction writes and keeps tenant jurisdiction reads available', async () => {
    vi.mocked(getCurrentUser).mockResolvedValueOnce({
      loginName: 'PlatformAdmin',
      staffId: null,
      tenantId: null,
      active: true,
      platformAdmin: true,
      authorities: ['JURISDICTION_READ', 'JURISDICTION_WRITE'],
    });

    await expect(accessControlProvider.can({ resource: 'jurisdictions', action: 'edit', params: {} }))
      .resolves.toMatchObject({ can: true });

    vi.mocked(getCurrentUser).mockResolvedValueOnce({
      loginName: 'Bravo_Admin',
      staffId: '001',
      tenantId: 'Bravo',
      active: true,
      platformAdmin: false,
      authorities: ['JURISDICTION_READ'],
    });

    await expect(accessControlProvider.can({ resource: 'jurisdictions', action: 'list', params: {} }))
      .resolves.toMatchObject({ can: true });
  });
});

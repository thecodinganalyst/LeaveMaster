import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getCurrentUser } from '../auth/session.ts';
import { accessControlProvider } from './accessControlProvider.ts';

vi.mock('../auth/session.ts', () => ({
  getCurrentUser: vi.fn(),
}));

describe('accessControlProvider self staff access', () => {
  beforeEach(() => {
    vi.mocked(getCurrentUser).mockReset();
  });

  it('allows a linked user to open staff details without STAFF_READ', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'staff-user',
      staffId: 'S001',
      tenantId: 'T1',
      active: true,
      authorities: [],
    });

    await expect(accessControlProvider.can({ resource: 'employees', action: 'show', params: {} }))
      .resolves.toMatchObject({ can: true });
  });

  it('still denies staff editing without STAFF_WRITE', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'staff-user',
      staffId: 'S001',
      tenantId: 'T1',
      active: true,
      authorities: [],
    });

    await expect(accessControlProvider.can({ resource: 'employees', action: 'edit', params: {} }))
      .resolves.toMatchObject({ can: false, reason: 'Missing STAFF_WRITE' });
  });

  it('does not grant staff detail access to an account without a linked staff record', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'unlinked-user',
      staffId: null,
      tenantId: 'T1',
      active: true,
      authorities: [],
    });

    await expect(accessControlProvider.can({ resource: 'employees', action: 'show', params: {} }))
      .resolves.toMatchObject({ can: false, reason: 'Missing STAFF_READ' });
  });
});

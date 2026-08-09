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
});

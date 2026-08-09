import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../api/http.ts';
import { clearCurrentUser, getCurrentUser } from './session.ts';

vi.mock('../api/http.ts', () => ({ apiFetch: vi.fn() }));

const user = {
  loginName: 'dennis',
  staffId: 'S1',
  tenantId: 'T1',
  active: true,
  authorities: ['LEAVE_APPLICATION_READ'],
};

describe('current user session cache', () => {
  beforeEach(() => {
    clearCurrentUser();
    vi.mocked(apiFetch).mockReset();
  });

  it('loads the authenticated identity once and reuses the cached value', async () => {
    vi.mocked(apiFetch).mockResolvedValue(user);

    await expect(getCurrentUser()).resolves.toEqual(user);
    await expect(getCurrentUser()).resolves.toEqual(user);

    expect(apiFetch).toHaveBeenCalledTimes(1);
    expect(apiFetch).toHaveBeenCalledWith('/auth/me');
  });

  it('force refreshes identity and clearCurrentUser invalidates the cache', async () => {
    const updated = { ...user, authorities: ['LEAVE_APPLICATION_READ', 'LEAVE_APPLICATION_WRITE'] };
    vi.mocked(apiFetch).mockResolvedValueOnce(user).mockResolvedValueOnce(updated).mockResolvedValueOnce(user);

    await getCurrentUser();
    await expect(getCurrentUser(true)).resolves.toEqual(updated);
    clearCurrentUser();
    await expect(getCurrentUser()).resolves.toEqual(user);

    expect(apiFetch).toHaveBeenCalledTimes(3);
  });
});

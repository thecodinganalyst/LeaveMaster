import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError, apiFetch, clearCsrfToken, loginWithSession } from '../api/http.ts';
import { clearCurrentUser, getCurrentUser } from '../auth/session.ts';
import { authProvider } from './authProvider.ts';

vi.mock('../api/http.ts', async () => {
  const actual = await vi.importActual<typeof import('../api/http.ts')>('../api/http.ts');
  return {
    ...actual,
    apiFetch: vi.fn(),
    clearCsrfToken: vi.fn(),
    loginWithSession: vi.fn(),
  };
});

vi.mock('../auth/session.ts', () => ({
  clearCurrentUser: vi.fn(),
  getCurrentUser: vi.fn(),
}));

describe('authProvider', () => {
  beforeEach(() => vi.clearAllMocks());

  it('rejects missing tenant-aware login credentials without calling the backend', async () => {
    await expect(authProvider.login?.({ tenantId: '', loginName: '', password: '' })).resolves.toMatchObject({
      success: false,
      error: { name: 'InvalidCredentials' },
    });
    expect(loginWithSession).not.toHaveBeenCalled();
  });

  it('establishes a backend session with tenant ID and refreshes server identity on login', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'dennis', staffId: 'S1', tenantId: 'T1', active: true, authorities: ['STAFF_READ'],
    });

    await expect(authProvider.login?.({ tenantId: ' T1 ', loginName: ' dennis ', password: 'secret' })).resolves.toEqual({
      success: true,
      redirectTo: '/',
    });
    expect(loginWithSession).toHaveBeenCalledWith('T1', 'dennis', 'secret');
    expect(clearCurrentUser).toHaveBeenCalled();
    expect(getCurrentUser).toHaveBeenCalledWith(true);
  });

  it('redirects to the originally requested protected route after login', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'dennis', staffId: 'S1', tenantId: 'T1', active: true, authorities: ['STAFF_READ'],
    });

    await expect(
      authProvider.login?.({
        tenantId: 'T1',
        loginName: 'dennis',
        password: 'secret',
        redirectPath: '/leave-requests/show/42',
      }),
    ).resolves.toEqual({
      success: true,
      redirectTo: '/leave-requests/show/42',
    });
  });

  it('surfaces generic backend authentication failures safely', async () => {
    vi.mocked(loginWithSession).mockRejectedValue(new ApiError('Invalid tenant ID, login name, or password.', 401));
    await expect(authProvider.login?.({ tenantId: 'T1', loginName: 'dennis', password: 'bad' })).resolves.toMatchObject({
      success: false,
      error: { name: 'AuthenticationError', message: 'Invalid tenant ID, login name, or password.' },
    });
  });

  it('clears cached identity and csrf state even if logout fails', async () => {
    vi.mocked(apiFetch).mockRejectedValue(new Error('network'));
    await expect(authProvider.logout?.({})).rejects.toThrow('network');
    expect(clearCurrentUser).toHaveBeenCalled();
    expect(clearCsrfToken).toHaveBeenCalled();
  });

  it('redirects expired sessions and preserves non-authentication errors', async () => {
    vi.mocked(getCurrentUser).mockRejectedValueOnce(new ApiError('Expired', 401));
    await expect(authProvider.check?.()).resolves.toEqual({ authenticated: false, redirectTo: '/login' });
    expect(clearCurrentUser).toHaveBeenCalled();

    vi.mocked(getCurrentUser).mockRejectedValueOnce(new Error('offline'));
    await expect(authProvider.check?.()).resolves.toMatchObject({
      authenticated: false,
      redirectTo: '/login',
      error: { name: 'AuthenticationError', message: 'offline' },
    });
  });

  it('returns only server-loaded identity fields', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'manager',
      staffId: 'S2',
      tenantId: 'T2',
      country: 'Singapore',
      active: true,
      platformAdmin: false,
      authorities: ['LEAVE_APPLICATION_APPROVE'],
    });
    await expect(authProvider.getIdentity?.()).resolves.toEqual({
      id: 'manager',
      name: 'manager',
      staffId: 'S2',
      tenantId: 'T2',
      country: 'Singapore',
      platformAdmin: false,
      authorities: ['LEAVE_APPLICATION_APPROVE'],
    });
  });

  it('exposes the platform admin flag used by administration field visibility', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({
      loginName: 'platformadmin',
      staffId: null,
      tenantId: null,
      active: true,
      platformAdmin: true,
      authorities: ['LEAVE_ENTITLEMENT_POLICY_WRITE'],
    });

    await expect(authProvider.getIdentity?.()).resolves.toMatchObject({
      id: 'platformadmin',
      platformAdmin: true,
      tenantId: null,
    });
  });

  it('requests logout only for 401 errors and does not treat 403 as session expiry', async () => {
    const unauthorized = new ApiError('Expired', 401);
    await expect(authProvider.onError?.(unauthorized)).resolves.toMatchObject({ logout: true, redirectTo: '/login' });
    expect(clearCurrentUser).toHaveBeenCalled();

    const forbidden = new ApiError('Forbidden', 403);
    await expect(authProvider.onError?.(forbidden)).resolves.toEqual({ error: forbidden });
  });
});

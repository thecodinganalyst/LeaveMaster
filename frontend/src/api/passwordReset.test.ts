import { beforeEach, describe, expect, it, vi } from 'vitest';

const apiFetch = vi.fn();

vi.mock('./http.ts', async () => {
  const actual = await vi.importActual<typeof import('./http.ts')>('./http.ts');
  return {
    ...actual,
    apiFetch: (...args: unknown[]) => apiFetch(...args),
  };
});

import {
  requestPasswordResetPin,
  setResetPassword,
  verifyPasswordResetPin,
} from './passwordReset.ts';

describe('password reset API client', () => {
  beforeEach(() => vi.clearAllMocks());

  it('sends tenant and login context through the password reset journey', async () => {
    const identity = { tenantId: 'tenant-a', loginName: 'alice' };
    apiFetch.mockResolvedValue({ message: 'accepted' });

    await requestPasswordResetPin(identity);
    expect(apiFetch).toHaveBeenCalledWith('/password-reset/request', {
      method: 'POST',
      body: JSON.stringify(identity),
    });

    await verifyPasswordResetPin(identity, '123456');
    expect(apiFetch).toHaveBeenCalledWith('/password-reset/verify', {
      method: 'POST',
      body: JSON.stringify({ ...identity, pin: '123456' }),
    });

    await setResetPassword(identity, 'new-password');
    expect(apiFetch).toHaveBeenCalledWith('/password-reset/set-password', {
      method: 'POST',
      body: JSON.stringify({ ...identity, password: 'new-password' }),
    });
  });
});

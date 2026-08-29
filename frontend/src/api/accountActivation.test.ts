import { beforeEach, describe, expect, it, vi } from 'vitest';

const apiFetch = vi.fn();

vi.mock('./http.ts', () => ({ apiFetch: (...args: unknown[]) => apiFetch(...args) }));

import {
  lookupAccountActivation,
  requestAccountActivationPin,
  setInitialAccountPassword,
  verifyAccountActivationPin,
} from './accountActivation.ts';

describe('account activation API client', () => {
  beforeEach(() => vi.clearAllMocks());

  it('sends tenant and login context through every activation request', async () => {
    const identity = { tenantId: 'tenant-a', loginName: 'alice' };
    apiFetch.mockResolvedValue({ nextStep: 'ACTIVATION' });

    await lookupAccountActivation(identity);
    expect(apiFetch).toHaveBeenCalledWith('/account-activation/lookup', {
      method: 'POST',
      body: JSON.stringify(identity),
    });

    await requestAccountActivationPin(identity);
    expect(apiFetch).toHaveBeenCalledWith('/account-activation/request', {
      method: 'POST',
      body: JSON.stringify(identity),
    });

    await verifyAccountActivationPin(identity, '123456');
    expect(apiFetch).toHaveBeenCalledWith('/account-activation/verify', {
      method: 'POST',
      body: JSON.stringify({ ...identity, pin: '123456' }),
    });

    await setInitialAccountPassword(identity, 'strongpass');
    expect(apiFetch).toHaveBeenCalledWith('/account-activation/set-password', {
      method: 'POST',
      body: JSON.stringify({ ...identity, password: 'strongpass' }),
    });
  });
});

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

  it('uses the backend activation contract', async () => {
    apiFetch.mockResolvedValue({ nextStep: 'ACTIVATION' });
    await lookupAccountActivation('alice');
    expect(apiFetch).toHaveBeenCalledWith('/account-activation/lookup', {
      method: 'POST',
      body: JSON.stringify({ loginName: 'alice' }),
    });

    await requestAccountActivationPin('alice');
    expect(apiFetch).toHaveBeenCalledWith('/account-activation/request', {
      method: 'POST',
      body: JSON.stringify({ loginName: 'alice' }),
    });

    await verifyAccountActivationPin('alice', '123456');
    expect(apiFetch).toHaveBeenCalledWith('/account-activation/verify', {
      method: 'POST',
      body: JSON.stringify({ loginName: 'alice', pin: '123456' }),
    });

    await setInitialAccountPassword('alice', 'strongpass');
    expect(apiFetch).toHaveBeenCalledWith('/account-activation/set-password', {
      method: 'POST',
      body: JSON.stringify({ loginName: 'alice', password: 'strongpass' }),
    });
  });
});

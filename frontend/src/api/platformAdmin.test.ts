import { beforeEach, describe, expect, it, vi } from 'vitest';

const apiFetch = vi.fn();

vi.mock('./http.ts', async () => {
  const actual = await vi.importActual<typeof import('./http.ts')>('./http.ts');
  return {
    ...actual,
    apiFetch: (...args: unknown[]) => apiFetch(...args),
  };
});

import { updatePlatformAdminRecoveryEmail } from './platformAdmin.ts';

describe('platform admin API client', () => {
  beforeEach(() => vi.clearAllMocks());

  it('updates the signed-in platform admin recovery email', async () => {
    apiFetch.mockResolvedValue(undefined);

    await updatePlatformAdminRecoveryEmail('admin@example.com');

    expect(apiFetch).toHaveBeenCalledWith('/platform-admin/recovery-email', {
      method: 'PUT',
      body: JSON.stringify({ email: 'admin@example.com' }),
    });
  });
});

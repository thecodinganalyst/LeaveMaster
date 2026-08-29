import { beforeEach, describe, expect, it, vi } from 'vitest';

const apiFetch = vi.fn();
const getCsrfToken = vi.fn();

vi.mock('./http.ts', () => ({
  apiFetch: (...args: unknown[]) => apiFetch(...args),
  getCsrfToken: (...args: unknown[]) => getCsrfToken(...args),
}));

vi.mock('../config/env.ts', () => ({
  env: { apiUrl: 'https://api.example.test' },
}));

import {
  clearRememberedOAuthProvider,
  getRememberedOAuthProvider,
  rememberOAuthProvider,
  startOAuthLink,
} from './oauth.ts';

describe('OAuth API helpers', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.sessionStorage.clear();
  });

  it('remembers only supported OAuth providers for the return trip', () => {
    rememberOAuthProvider('google');
    expect(getRememberedOAuthProvider()).toBe('google');

    clearRememberedOAuthProvider();
    expect(getRememberedOAuthProvider()).toBeUndefined();

    window.sessionStorage.setItem('leavemaster.oauthProvider', 'microsoft');
    expect(getRememberedOAuthProvider()).toBeUndefined();
  });

  it('posts the CSRF token in a real browser form so the OAuth redirect is followed as navigation', async () => {
    apiFetch.mockResolvedValue({ linked: false, provider: null });
    getCsrfToken.mockResolvedValue({ token: 'csrf-value', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' });
    const submit = vi.spyOn(HTMLFormElement.prototype, 'submit').mockImplementation(() => undefined);

    await startOAuthLink('github');

    expect(apiFetch).toHaveBeenCalledWith('/auth/oauth-link/status');
    expect(getCsrfToken).toHaveBeenCalled();
    const form = document.body.querySelector('form');
    expect(form).not.toBeNull();
    expect(form?.method).toBe('post');
    expect(form?.action).toBe('https://api.example.test/auth/oauth-link/github/start');
    const csrfInput = form?.querySelector('input[name="_csrf"]') as HTMLInputElement | null;
    expect(csrfInput?.value).toBe('csrf-value');
    expect(submit).toHaveBeenCalled();
    expect(getRememberedOAuthProvider()).toBe('github');

    form?.remove();
    submit.mockRestore();
  });

  it('does not start linking when the LeaveMaestro account already has a provider', async () => {
    apiFetch.mockResolvedValue({ linked: true, provider: 'google' });

    await expect(startOAuthLink('github')).rejects.toThrow('already has an OAuth provider linked (google)');
    expect(getCsrfToken).not.toHaveBeenCalled();
  });
});

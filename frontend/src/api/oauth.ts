import { getCsrfToken, apiFetch } from './http.ts';
import { env } from '../config/env.ts';

export type OAuthProvider = 'google' | 'github';

export interface OAuthLinkStatus {
  linked: boolean;
  provider: string | null;
}

const OAUTH_PROVIDER_KEY = 'leavemaster.oauthProvider';

const buildUrl = (path: string) => `${env.apiUrl}${path.startsWith('/') ? path : `/${path}`}`;

export const rememberOAuthProvider = (provider: OAuthProvider) => {
  window.sessionStorage.setItem(OAUTH_PROVIDER_KEY, provider);
};

export const getRememberedOAuthProvider = (): OAuthProvider | undefined => {
  const provider = window.sessionStorage.getItem(OAUTH_PROVIDER_KEY);
  return provider === 'google' || provider === 'github' ? provider : undefined;
};

export const clearRememberedOAuthProvider = () => {
  window.sessionStorage.removeItem(OAUTH_PROVIDER_KEY);
};

export const startOAuthLogin = (provider: OAuthProvider) => {
  rememberOAuthProvider(provider);
  window.location.assign(buildUrl(`/oauth2/authorization/${provider}`));
};

export const getOAuthLinkStatus = () => apiFetch<OAuthLinkStatus>('/auth/oauth-link/status');

export const startOAuthLink = async (provider: OAuthProvider) => {
  const status = await getOAuthLinkStatus();
  if (status.linked) {
    const linkedProvider = status.provider ? ` (${status.provider})` : '';
    throw new Error(`This LeaveMaestro account already has an OAuth provider linked${linkedProvider}.`);
  }

  const csrf = await getCsrfToken();
  rememberOAuthProvider(provider);

  const form = document.createElement('form');
  form.method = 'POST';
  form.action = buildUrl(`/auth/oauth-link/${provider}/start`);
  form.style.display = 'none';

  const csrfInput = document.createElement('input');
  csrfInput.type = 'hidden';
  csrfInput.name = csrf.parameterName;
  csrfInput.value = csrf.token;
  form.appendChild(csrfInput);

  document.body.appendChild(form);
  form.submit();
};

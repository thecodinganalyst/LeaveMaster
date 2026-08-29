import { apiFetch } from './http.ts';

export type AccountActivationNextStep = 'PASSWORD' | 'ACTIVATION';

interface LookupResponse {
  nextStep: AccountActivationNextStep;
}

interface MessageResponse {
  message: string;
}

interface AccountIdentity {
  tenantId: string;
  loginName: string;
}

export const lookupAccountActivation = async ({ tenantId, loginName }: AccountIdentity) =>
  apiFetch<LookupResponse>('/account-activation/lookup', {
    method: 'POST',
    body: JSON.stringify({ tenantId, loginName }),
  });

export const requestAccountActivationPin = async ({ tenantId, loginName }: AccountIdentity) =>
  apiFetch<MessageResponse>('/account-activation/request', {
    method: 'POST',
    body: JSON.stringify({ tenantId, loginName }),
  });

export const verifyAccountActivationPin = async ({ tenantId, loginName }: AccountIdentity, pin: string) =>
  apiFetch<MessageResponse>('/account-activation/verify', {
    method: 'POST',
    body: JSON.stringify({ tenantId, loginName, pin }),
  });

export const setInitialAccountPassword = async ({ tenantId, loginName }: AccountIdentity, password: string) =>
  apiFetch<void>('/account-activation/set-password', {
    method: 'POST',
    body: JSON.stringify({ tenantId, loginName, password }),
  });

import { apiFetch } from './http.ts';

interface AccountIdentity {
  tenantId: string;
  loginName: string;
}

interface MessageResponse {
  message: string;
}

export const requestPasswordResetPin = async ({ tenantId, loginName }: AccountIdentity) =>
  apiFetch<MessageResponse>('/password-reset/request', {
    method: 'POST',
    body: JSON.stringify({ tenantId, loginName }),
  });

export const verifyPasswordResetPin = async ({ tenantId, loginName }: AccountIdentity, pin: string) =>
  apiFetch<MessageResponse>('/password-reset/verify', {
    method: 'POST',
    body: JSON.stringify({ tenantId, loginName, pin }),
  });

export const setResetPassword = async ({ tenantId, loginName }: AccountIdentity, password: string) =>
  apiFetch<void>('/password-reset/set-password', {
    method: 'POST',
    body: JSON.stringify({ tenantId, loginName, password }),
  });

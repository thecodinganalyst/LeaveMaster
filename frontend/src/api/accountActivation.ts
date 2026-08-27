import { apiFetch } from './http.ts';

export type AccountActivationNextStep = 'PASSWORD' | 'ACTIVATION';

interface LookupResponse {
  nextStep: AccountActivationNextStep;
}

interface MessageResponse {
  message: string;
}

export const lookupAccountActivation = async (loginName: string) =>
  apiFetch<LookupResponse>('/account-activation/lookup', {
    method: 'POST',
    body: JSON.stringify({ loginName }),
  });

export const requestAccountActivationPin = async (loginName: string) =>
  apiFetch<MessageResponse>('/account-activation/request', {
    method: 'POST',
    body: JSON.stringify({ loginName }),
  });

export const verifyAccountActivationPin = async (loginName: string, pin: string) =>
  apiFetch<MessageResponse>('/account-activation/verify', {
    method: 'POST',
    body: JSON.stringify({ loginName, pin }),
  });

export const setInitialAccountPassword = async (loginName: string, password: string) =>
  apiFetch<void>('/account-activation/set-password', {
    method: 'POST',
    body: JSON.stringify({ loginName, password }),
  });

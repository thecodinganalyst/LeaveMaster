import { apiFetch } from './http.ts';

export const updatePlatformAdminRecoveryEmail = async (email: string) =>
  apiFetch<void>('/platform-admin/recovery-email', {
    method: 'PUT',
    body: JSON.stringify({ email }),
  });

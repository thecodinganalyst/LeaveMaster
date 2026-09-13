import { apiFetch } from '../api/http.ts';

export type TenantType = 'STANDARD' | 'DEMO';

export interface CurrentUser {
  loginName: string;
  staffId: string | null;
  tenantId: string | null;
  country?: string | null;
  active: boolean;
  platformAdmin?: boolean;
  authorities: string[];
  tenantType?: TenantType;
  demo?: boolean;
}

let cachedUser: CurrentUser | undefined;

export const clearCurrentUser = () => {
  cachedUser = undefined;
};

export const getCurrentUser = async (forceRefresh = false) => {
  if (cachedUser && !forceRefresh) {
    return cachedUser;
  }

  cachedUser = await apiFetch<CurrentUser>('/auth/me');
  return cachedUser;
};

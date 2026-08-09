import { apiFetch } from '../api/http.ts';

export interface CurrentUser {
  loginName: string;
  staffId: string | null;
  tenantId: string | null;
  active: boolean;
  authorities: string[];
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

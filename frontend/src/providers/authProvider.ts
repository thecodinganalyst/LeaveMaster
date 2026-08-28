import type { AuthBindings } from '@refinedev/core';

import { ApiError, apiFetch, clearCsrfToken, loginWithSession } from '../api/http.ts';
import { clearCurrentUser, getCurrentUser } from '../auth/session.ts';

interface LoginParams {
  tenantId?: string;
  loginName?: string;
  password?: string;
  redirectPath?: string;
}

export const authProvider: AuthBindings = {
  login: async (params) => {
    const { tenantId, loginName, password, redirectPath } = (params ?? {}) as LoginParams;

    if (!tenantId || !loginName || !password) {
      return {
        success: false,
        error: {
          name: 'InvalidCredentials',
          message: 'Tenant ID, login name, and password are required.',
        },
      };
    }

    try {
      await loginWithSession(tenantId.trim(), loginName.trim(), password);
      clearCurrentUser();
      await getCurrentUser(true);
      return { success: true, redirectTo: redirectPath ?? '/' };
    } catch (error) {
      const message = error instanceof ApiError ? error.message : 'Unable to sign in.';
      return {
        success: false,
        error: { name: 'AuthenticationError', message },
      };
    }
  },

  logout: async () => {
    try {
      await apiFetch<void>('/logout', { method: 'POST' });
    } finally {
      clearCurrentUser();
      clearCsrfToken();
    }

    return { success: true, redirectTo: '/login' };
  },

  check: async () => {
    try {
      await getCurrentUser(true);
      return { authenticated: true };
    } catch (error) {
      if (error instanceof ApiError && [401, 403, 404].includes(error.statusCode)) {
        clearCurrentUser();
        return { authenticated: false, redirectTo: '/login' };
      }
      return {
        authenticated: false,
        redirectTo: '/login',
        error: {
          name: 'AuthenticationError',
          message: error instanceof Error ? error.message : 'Unable to verify authentication.',
        },
      };
    }
  },

  getIdentity: async () => {
    const user = await getCurrentUser();
    return {
      id: user.loginName,
      name: user.loginName,
      staffId: user.staffId,
      tenantId: user.tenantId,
      country: user.country,
      platformAdmin: user.platformAdmin,
      authorities: user.authorities,
    };
  },

  onError: async (error) => {
    if (error instanceof ApiError && error.statusCode === 401) {
      clearCurrentUser();
      return { logout: true, redirectTo: '/login', error };
    }

    if (error instanceof ApiError && error.statusCode === 403) {
      return { error };
    }

    return { error };
  },
};

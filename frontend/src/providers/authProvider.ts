import type { AuthBindings } from '@refinedev/core';

const AUTH_KEY = 'leavemaster.authenticated';

export const authProvider: AuthBindings = {
  login: async () => {
    localStorage.setItem(AUTH_KEY, 'true');
    return {
      success: true,
      redirectTo: '/',
    };
  },
  logout: async () => {
    localStorage.removeItem(AUTH_KEY);
    return {
      success: true,
      redirectTo: '/login',
    };
  },
  check: async () => {
    const isAuthenticated = localStorage.getItem(AUTH_KEY) === 'true';

    if (isAuthenticated) {
      return {
        authenticated: true,
      };
    }

    return {
      authenticated: false,
      redirectTo: '/login',
    };
  },
  getIdentity: async () => ({
    id: 1,
    name: 'LeaveMaster User',
  }),
  onError: async () => {
    return {
      error: {
        name: 'AuthenticationError',
        message: 'Authentication failed',
      },
    };
  },
};

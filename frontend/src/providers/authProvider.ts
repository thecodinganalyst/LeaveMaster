import type { AuthBindings } from '@refinedev/core';

const AUTH_KEY = 'leavemaster.authenticated';
const DEMO_EMAIL = 'admin@leavemaster.dev';
const DEMO_PASSWORD = 'LeaveMaster123!';

interface LoginParams {
  email?: string;
  password?: string;
}

export const authProvider: AuthBindings = {
  login: async (params) => {
    const { email, password } = (params ?? {}) as LoginParams;
    const isValidCredentials = email === DEMO_EMAIL && password === DEMO_PASSWORD;

    if (!isValidCredentials) {
      return {
        success: false,
        error: {
          name: 'InvalidCredentials',
          message: 'Use demo credentials to sign in.',
        },
      };
    }

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

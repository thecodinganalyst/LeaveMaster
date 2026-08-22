import { env } from '../config/env.ts';

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly statusCode: number,
    public readonly details?: unknown,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

interface CsrfResponse {
  token: string;
  headerName: string;
  parameterName: string;
}

let csrfToken: CsrfResponse | undefined;

const buildUrl = (path: string) => `${env.apiUrl}${path.startsWith('/') ? path : `/${path}`}`;

const fetchWithNetworkDiagnostics = async (input: RequestInfo | URL, init?: RequestInit) => {
  try {
    return await fetch(input, init);
  } catch (error) {
    if (error instanceof TypeError) {
      throw new ApiError(
        'The connection to LeaveMaestro was interrupted or timed out. Please try again.',
        0,
        { networkFailure: true },
      );
    }
    throw error;
  }
};

const parseBody = async (response: Response) => {
  if (response.status === 204) {
    return undefined;
  }

  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    return response.json();
  }

  const text = await response.text();
  return text || undefined;
};

const messageFromBody = (body: unknown, fallback: string) => {
  if (body && typeof body === 'object' && 'error' in body && typeof body.error === 'string') {
    return body.error;
  }
  if (body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') {
    return body.message;
  }
  if (typeof body === 'string' && body.trim()) {
    return body;
  }
  return fallback;
};

export const clearCsrfToken = () => {
  csrfToken = undefined;
};

export const getCsrfToken = async (forceRefresh = false) => {
  if (csrfToken && !forceRefresh) {
    return csrfToken;
  }

  const response = await fetchWithNetworkDiagnostics(buildUrl('/auth/csrf'), {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  });
  const body = await parseBody(response);

  if (!response.ok) {
    throw new ApiError(messageFromBody(body, 'Unable to initialize secure session.'), response.status, body);
  }

  csrfToken = body as CsrfResponse;
  return csrfToken;
};

const isUnsafeMethod = (method: string) => !['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase());

export const apiFetch = async <T>(path: string, init: RequestInit = {}): Promise<T> => {
  const method = (init.method ?? 'GET').toUpperCase();
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');

  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  if (isUnsafeMethod(method)) {
    const csrf = await getCsrfToken();
    headers.set(csrf.headerName, csrf.token);
  }

  const response = await fetchWithNetworkDiagnostics(buildUrl(path), {
    ...init,
    method,
    headers,
    credentials: 'include',
  });
  const body = await parseBody(response);

  if (!response.ok) {
    throw new ApiError(messageFromBody(body, response.statusText || 'Request failed.'), response.status, body);
  }

  return body as T;
};

export const loginWithSession = async (loginName: string, password: string) => {
  const csrf = await getCsrfToken();
  const form = new URLSearchParams({ username: loginName, password });
  const response = await fetchWithNetworkDiagnostics(buildUrl('/auth/login'), {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      [csrf.headerName]: csrf.token,
    },
    body: form,
  });

  if (!response.ok) {
    throw new ApiError('Invalid login name or password.', response.status);
  }

  clearCsrfToken();
  await getCsrfToken(true);
};

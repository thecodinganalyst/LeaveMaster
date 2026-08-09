import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError, apiFetch, clearCsrfToken, getCsrfToken, loginWithSession } from './http.ts';

const jsonResponse = (body: unknown, init: ResponseInit = {}) =>
  new Response(JSON.stringify(body), {
    status: init.status ?? 200,
    statusText: init.statusText,
    headers: { 'content-type': 'application/json', ...(init.headers ?? {}) },
  });

describe('http client', () => {
  beforeEach(() => {
    clearCsrfToken();
    vi.restoreAllMocks();
  });

  it('loads and caches the csrf token', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse({
      token: 'csrf-1', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf',
    }));

    await expect(getCsrfToken()).resolves.toMatchObject({ token: 'csrf-1' });
    await getCsrfToken();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/auth/csrf', expect.objectContaining({ credentials: 'include' }));
  });

  it('adds credentials, json headers and csrf protection to unsafe requests', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(jsonResponse({ token: 'csrf-2', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' }))
      .mockResolvedValueOnce(jsonResponse({ id: 'T1' }));

    await expect(apiFetch('/tenants', { method: 'POST', body: JSON.stringify({ id: 'T1' }) })).resolves.toEqual({ id: 'T1' });

    const [, request] = fetchMock.mock.calls[1]!;
    const headers = request?.headers as Headers;
    expect(request).toMatchObject({ method: 'POST', credentials: 'include' });
    expect(headers.get('Accept')).toBe('application/json');
    expect(headers.get('Content-Type')).toBe('application/json');
    expect(headers.get('X-CSRF-TOKEN')).toBe('csrf-2');
  });

  it('does not fetch csrf for safe GET requests and handles no-content responses', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }));
    await expect(apiFetch<void>('/logout')).resolves.toBeUndefined();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('maps structured backend errors into ApiError details', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse({ error: 'Not allowed' }, { status: 403, statusText: 'Forbidden' }));
    await expect(apiFetch('/staff')).rejects.toMatchObject({
      name: 'ApiError', statusCode: 403, message: 'Not allowed', details: { error: 'Not allowed' },
    });
  });

  it('uses plain-text backend errors when JSON is unavailable', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('Service unavailable', {
      status: 503,
      headers: { 'content-type': 'text/plain' },
    }));
    await expect(apiFetch('/staff')).rejects.toEqual(expect.objectContaining<ApiError>({
      statusCode: 503,
      message: 'Service unavailable',
    }));
  });

  it('posts form credentials for session login then refreshes csrf', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(jsonResponse({ token: 'csrf-login', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' }))
      .mockResolvedValueOnce(new Response(null, { status: 200 }))
      .mockResolvedValueOnce(jsonResponse({ token: 'csrf-new', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' }));

    await loginWithSession('dennis', 'password');

    const [url, request] = fetchMock.mock.calls[1]!;
    expect(url).toBe('http://localhost:8080/login');
    expect(request).toMatchObject({ method: 'POST', credentials: 'include' });
    expect(String(request?.body)).toContain('username=dennis');
    expect(fetchMock).toHaveBeenCalledTimes(3);
    await expect(getCsrfToken()).resolves.toMatchObject({ token: 'csrf-new' });
  });

  it('rejects failed session login with a stable credential error', async () => {
    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(jsonResponse({ token: 'csrf-login', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }));

    await expect(loginWithSession('dennis', 'bad')).rejects.toMatchObject({
      statusCode: 401,
      message: 'Invalid login name or password.',
    });
  });
});

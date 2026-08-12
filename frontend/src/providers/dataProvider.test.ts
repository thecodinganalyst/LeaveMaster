import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../api/http.ts';
import { leaveMasterDataProvider } from './dataProvider.ts';

vi.mock('../api/http.ts', async () => {
  const actual = await vi.importActual<typeof import('../api/http.ts')>('../api/http.ts');
  return { ...actual, apiFetch: vi.fn() };
});

describe('leaveMasterDataProvider', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockReset();
  });

  it('maps employee resources to the API-prefixed staff endpoint and paginates locally', async () => {
    vi.mocked(apiFetch).mockResolvedValue([
      { id: '2', name: 'Beta' },
      { id: '1', name: 'Alpha' },
      { id: '3', name: 'Gamma' },
    ]);

    const result = await leaveMasterDataProvider.getList({
      resource: 'employees',
      pagination: { current: 1, pageSize: 2, mode: 'server' },
      sorters: [{ field: 'name', order: 'asc' }],
      filters: [],
      meta: {},
    });

    expect(apiFetch).toHaveBeenCalledWith('/api/staff');
    expect(result.total).toBe(3);
    expect(result.data.map((record) => record.id)).toEqual(['1', '2']);
  });

  it('hides PLATFORM_ADMIN from role lists', async () => {
    vi.mocked(apiFetch).mockResolvedValue([
      { id: 'PLATFORM_ADMIN', description: 'Internal platform role' },
      { id: 'TENANT_ADMIN', description: 'Tenant admin' },
    ]);

    const result = await leaveMasterDataProvider.getList({
      resource: 'roles',
      pagination: { mode: 'off' },
      sorters: [],
      filters: [],
      meta: {},
    });

    expect(result.total).toBe(1);
    expect(result.data.map((record) => record.id)).toEqual(['TENANT_ADMIN']);
  });

  it('blocks direct PLATFORM_ADMIN role navigation without calling the API', async () => {
    await expect(
      leaveMasterDataProvider.getOne({ resource: 'roles', id: 'PLATFORM_ADMIN', meta: {} }),
    ).rejects.toMatchObject({ statusCode: 404 });

    expect(apiFetch).not.toHaveBeenCalled();
  });

  it('blocks PLATFORM_ADMIN role creation and mutation without calling the API', async () => {
    await expect(
      leaveMasterDataProvider.create({ resource: 'roles', variables: { id: 'PLATFORM_ADMIN' }, meta: {} }),
    ).rejects.toMatchObject({ statusCode: 404 });
    await expect(
      leaveMasterDataProvider.update({ resource: 'roles', id: 'PLATFORM_ADMIN', variables: { active: false }, meta: {} }),
    ).rejects.toMatchObject({ statusCode: 404 });
    await expect(
      leaveMasterDataProvider.deleteOne({ resource: 'roles', id: 'PLATFORM_ADMIN', meta: {} }),
    ).rejects.toMatchObject({ statusCode: 404 });

    expect(apiFetch).not.toHaveBeenCalled();
  });

  it('uses API-prefixed JSON write requests for normal CRUD resources', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ id: 'T1', name: 'Tenant One' });

    const result = await leaveMasterDataProvider.create({
      resource: 'tenants',
      variables: { id: 'T1', name: 'Tenant One' },
      meta: {},
    });

    expect(apiFetch).toHaveBeenCalledWith('/api/tenants', {
      method: 'POST',
      body: JSON.stringify({ id: 'T1', name: 'Tenant One' }),
    });
    expect(result.data).toMatchObject({ id: 'T1' });
  });

  it('matches the backend tenant read, update and delete contracts through the API namespace', async () => {
    vi.mocked(apiFetch)
      .mockResolvedValueOnce({ id: 'Tenant A', name: 'Tenant A' })
      .mockResolvedValueOnce({ id: 'Tenant A', name: 'Tenant A Updated' })
      .mockResolvedValueOnce(undefined);

    const read = await leaveMasterDataProvider.getOne({
      resource: 'tenants',
      id: 'Tenant A',
      meta: {},
    });
    const updated = await leaveMasterDataProvider.update({
      resource: 'tenants',
      id: 'Tenant A',
      variables: { name: 'Tenant A Updated' },
      meta: {},
    });
    const deleted = await leaveMasterDataProvider.deleteOne({
      resource: 'tenants',
      id: 'Tenant A',
      meta: {},
    });

    expect(apiFetch).toHaveBeenNthCalledWith(1, '/api/tenants/Tenant%20A');
    expect(apiFetch).toHaveBeenNthCalledWith(2, '/api/tenants/Tenant%20A', {
      method: 'PUT',
      body: JSON.stringify({ name: 'Tenant A Updated' }),
    });
    expect(apiFetch).toHaveBeenNthCalledWith(3, '/api/tenants/Tenant%20A', {
      method: 'DELETE',
    });
    expect(read.data).toMatchObject({ id: 'Tenant A' });
    expect(updated.data).toMatchObject({ name: 'Tenant A Updated' });
    expect(deleted.data).toEqual({ id: 'Tenant A' });
  });

  it('reports a clear routing error when a list endpoint returns the SPA instead of JSON', async () => {
    vi.mocked(apiFetch).mockResolvedValue('<!doctype html><html></html>');

    await expect(
      leaveMasterDataProvider.getList({
        resource: 'tenants',
        pagination: { mode: 'off' },
        sorters: [],
        filters: [],
        meta: {},
      }),
    ).rejects.toMatchObject({
      name: 'ApiError',
      statusCode: 502,
      message: expect.stringContaining('/api/tenants'),
    });
  });
});

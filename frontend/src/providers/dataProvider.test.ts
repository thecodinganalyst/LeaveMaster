import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../api/http.ts';
import { leaveMasterDataProvider } from './dataProvider.ts';

vi.mock('../api/http.ts', () => ({
  apiFetch: vi.fn(),
}));

describe('leaveMasterDataProvider', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockReset();
  });

  it('maps employee resources to the staff endpoint and paginates locally', async () => {
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

    expect(apiFetch).toHaveBeenCalledWith('/staff');
    expect(result.total).toBe(3);
    expect(result.data.map((record) => record.id)).toEqual(['1', '2']);
  });

  it('uses JSON write requests for normal CRUD resources', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ id: 'T1', name: 'Tenant One' });

    const result = await leaveMasterDataProvider.create({
      resource: 'tenants',
      variables: { id: 'T1', name: 'Tenant One' },
      meta: {},
    });

    expect(apiFetch).toHaveBeenCalledWith('/tenants', {
      method: 'POST',
      body: JSON.stringify({ id: 'T1', name: 'Tenant One' }),
    });
    expect(result.data).toMatchObject({ id: 'T1' });
  });
});

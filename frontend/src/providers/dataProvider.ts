import type { BaseRecord, CrudFilter, CrudSorting, DataProvider } from '@refinedev/core';

import { apiFetch } from '../api/http.ts';
import { env } from '../config/env.ts';

type ProviderParams<K extends keyof DataProvider> = DataProvider[K] extends (...args: infer P) => unknown ? P[0] : never;

const endpointByResource: Record<string, string> = {
  tenants: '/tenants',
  users: '/users',
  roles: '/roles',
  employees: '/staff',
  staff: '/staff',
  locations: '/locations',
  'leave-types': '/leave-types',
  'leave-approvers': '/leave-approvers',
  'leave-calendars': '/leave-calendars',
  'leave-requests': '/leave-applications',
  'leave-applications': '/leave-applications',
};

const endpointFor = (resource: string) => endpointByResource[resource] ?? `/${resource}`;

const compare = (left: unknown, right: unknown) => String(left ?? '').localeCompare(String(right ?? ''), undefined, { numeric: true });

const matchesFilter = (record: BaseRecord, filter: CrudFilter) => {
  if (!('field' in filter)) {
    return true;
  }

  const value = record[filter.field];
  const expected = filter.value;

  switch (filter.operator) {
    case 'eq':
      return value === expected;
    case 'ne':
      return value !== expected;
    case 'contains':
    case 'containss':
      return String(value ?? '').toLowerCase().includes(String(expected ?? '').toLowerCase());
    case 'startswith':
      return String(value ?? '').toLowerCase().startsWith(String(expected ?? '').toLowerCase());
    case 'endswith':
      return String(value ?? '').toLowerCase().endsWith(String(expected ?? '').toLowerCase());
    case 'in':
      return Array.isArray(expected) && expected.includes(value);
    case 'nin':
      return Array.isArray(expected) && !expected.includes(value);
    case 'gt':
      return compare(value, expected) > 0;
    case 'gte':
      return compare(value, expected) >= 0;
    case 'lt':
      return compare(value, expected) < 0;
    case 'lte':
      return compare(value, expected) <= 0;
    default:
      return true;
  }
};

const applySorting = (records: BaseRecord[], sorters: CrudSorting = []) => {
  return [...records].sort((left, right) => {
    for (const sorter of sorters) {
      const result = compare(left[sorter.field], right[sorter.field]);
      if (result !== 0) {
        return sorter.order === 'desc' ? -result : result;
      }
    }
    return 0;
  });
};

const provider = {
  getApiUrl: () => env.apiUrl,

  getList: async ({ resource, pagination, filters = [], sorters = [] }: ProviderParams<'getList'>) => {
    const records = await apiFetch<BaseRecord[]>(endpointFor(resource));
    const filtered = records.filter((record) => filters.every((filter) => matchesFilter(record, filter)));
    const sorted = applySorting(filtered, sorters);

    if (pagination?.mode === 'off') {
      return { data: sorted, total: sorted.length };
    }

    const current = pagination?.current ?? 1;
    const pageSize = pagination?.pageSize ?? 10;
    const start = (current - 1) * pageSize;

    return {
      data: sorted.slice(start, start + pageSize),
      total: sorted.length,
    };
  },

  getOne: async ({ resource, id }: ProviderParams<'getOne'>) => ({
    data: await apiFetch<BaseRecord>(`${endpointFor(resource)}/${encodeURIComponent(String(id))}`),
  }),

  create: async ({ resource, variables }: ProviderParams<'create'>) => ({
    data: await apiFetch<BaseRecord>(endpointFor(resource), {
      method: 'POST',
      body: JSON.stringify(variables),
    }),
  }),

  update: async ({ resource, id, variables }: ProviderParams<'update'>) => ({
    data: await apiFetch<BaseRecord>(`${endpointFor(resource)}/${encodeURIComponent(String(id))}`, {
      method: 'PUT',
      body: JSON.stringify(variables),
    }),
  }),

  deleteOne: async ({ resource, id }: ProviderParams<'deleteOne'>) => {
    await apiFetch<void>(`${endpointFor(resource)}/${encodeURIComponent(String(id))}`, {
      method: 'DELETE',
    });
    return { data: { id } };
  },

  createMany: async ({ resource, variables }: ProviderParams<'createMany'>) => {
    const data = await Promise.all(
      variables.map((value) =>
        apiFetch<BaseRecord>(endpointFor(resource), {
          method: 'POST',
          body: JSON.stringify(value),
        }),
      ),
    );
    return { data };
  },

  deleteMany: async ({ resource, ids }: ProviderParams<'deleteMany'>) => {
    await Promise.all(
      ids.map((id) =>
        apiFetch<void>(`${endpointFor(resource)}/${encodeURIComponent(String(id))}`, {
          method: 'DELETE',
        }),
      ),
    );
    return { data: ids };
  },

  updateMany: async ({ resource, ids, variables }: ProviderParams<'updateMany'>) => {
    const data = await Promise.all(
      ids.map((id) =>
        apiFetch<BaseRecord>(`${endpointFor(resource)}/${encodeURIComponent(String(id))}`, {
          method: 'PUT',
          body: JSON.stringify(variables),
        }),
      ),
    );
    return { data };
  },

  custom: async ({ url, method, payload, headers }: ProviderParams<'custom'>) => {
    const init: RequestInit = {
      method: method.toUpperCase(),
      ...(headers ? { headers } : {}),
      ...(payload === undefined ? {} : { body: JSON.stringify(payload) }),
    };
    return { data: await apiFetch(url, init) };
  },
};

export const leaveMasterDataProvider = provider as DataProvider;

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../../api/http.ts';
import { loadStaffDependants, syncStaffDependants } from './staffDependants.ts';

vi.mock('../../api/http.ts', () => ({ apiFetch: vi.fn() }));

const mockedApiFetch = vi.mocked(apiFetch);

describe('staffDependants', () => {
  beforeEach(() => mockedApiFetch.mockReset());

  it('loads dependants from the staff-scoped endpoint', async () => {
    mockedApiFetch.mockResolvedValueOnce([]);
    await loadStaffDependants('S 001');
    expect(mockedApiFetch).toHaveBeenCalledWith('/api/staff/S%20001/dependants');
  });

  it('creates, updates and deletes dependant records to match the form', async () => {
    mockedApiFetch.mockImplementation((path: string) => {
      if (path === '/api/staff/S001/dependants') {
        return Promise.resolve([
          { id: 'D1', name: 'Existing Child', relationshipCode: 'CHILD', active: true },
          { id: 'D2', name: 'Remove Me', relationshipCode: 'CHILD', active: true },
        ]);
      }
      return Promise.resolve({});
    });

    await syncStaffDependants('S001', [
      { id: 'D1', name: 'Updated Child', relationshipCode: 'CHILD', citizenshipCode: 'SG', active: true },
      { name: 'New Parent', relationshipCode: 'PARENT', active: true },
    ]);

    expect(mockedApiFetch).toHaveBeenCalledWith('/api/staff/S001/dependants/D1', expect.objectContaining({ method: 'PUT' }));
    expect(mockedApiFetch).toHaveBeenCalledWith('/api/staff/S001/dependants', expect.objectContaining({ method: 'POST' }));
    expect(mockedApiFetch).toHaveBeenCalledWith('/api/staff/S001/dependants/D2', { method: 'DELETE' });
  });

  it('does not touch dependant APIs when the form has not loaded dependant values', async () => {
    await syncStaffDependants('S001', undefined);
    expect(mockedApiFetch).not.toHaveBeenCalled();
  });
});

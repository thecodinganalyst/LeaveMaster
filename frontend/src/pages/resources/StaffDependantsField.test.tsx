import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { Form } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../../api/http.ts';
import { StaffDependantsField } from './StaffDependantsField.tsx';

vi.mock('../../api/http.ts', () => ({ apiFetch: vi.fn() }));

const mockedApiFetch = vi.mocked(apiFetch);

const renderField = (editing = false, staffId?: string) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <Form>
        <StaffDependantsField editing={editing} {...(staffId ? { staffId } : {})} />
      </Form>
    </QueryClientProvider>,
  );
};

describe('StaffDependantsField', () => {
  beforeEach(() => {
    mockedApiFetch.mockReset();
    mockedApiFetch.mockImplementation(async (path) => {
      if (path === '/api/jurisdictions') {
        return [{ id: 'SG', code: 'SG', name: 'Singapore', parentId: null }] as never;
      }
      if (path === '/api/staff/S001/dependants') {
        return [{
          id: 'D1',
          name: 'Child One',
          relationshipCode: 'CHILD',
          dateOfBirth: '2023-05-10',
          citizenshipCode: 'SG',
          residencyCode: 'PERMANENT_RESIDENT',
          active: true,
        }] as never;
      }
      return [] as never;
    });
  });

  it('loads existing dependant records with normalized citizenship and residency labels', async () => {
    renderField(true, 'S001');
    expect(await screen.findByDisplayValue('Child One')).toBeInTheDocument();
    expect(screen.getByDisplayValue('2023-05-10')).toBeInTheDocument();
    expect(await screen.findByText('Singapore (SG)')).toBeInTheDocument();
    expect(screen.getByText('Permanent resident')).toBeInTheDocument();
    expect(mockedApiFetch).toHaveBeenCalledWith('/api/staff/S001/dependants');
    expect(mockedApiFetch).toHaveBeenCalledWith('/api/jurisdictions');
  });

  it('adds and removes a dependant in the staff form', async () => {
    renderField();
    fireEvent.click(screen.getByRole('button', { name: /Add dependant/i }));
    expect(screen.getByLabelText('Name')).toBeInTheDocument();
    expect(screen.getByLabelText('Citizenship')).toBeInTheDocument();
    expect(screen.getByLabelText('Residency status')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Remove dependant/i }));
    await waitFor(() => expect(screen.queryByLabelText('Name')).not.toBeInTheDocument());
  });
});

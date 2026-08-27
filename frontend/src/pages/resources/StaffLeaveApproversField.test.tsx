import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { Form } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../../api/http.ts';
import { StaffLeaveApproversField } from './StaffLeaveApproversField.tsx';

vi.mock('../../api/http.ts', () => ({ apiFetch: vi.fn() }));

const mockedApiFetch = vi.mocked(apiFetch);

const renderField = (editing = false, staffId?: string) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <Form>
        <StaffLeaveApproversField editing={editing} {...(staffId ? { staffId } : {})} />
      </Form>
    </QueryClientProvider>,
  );
};

describe('StaffLeaveApproversField', () => {
  beforeEach(() => {
    mockedApiFetch.mockReset();
    mockedApiFetch.mockImplementation((path: string) => {
      if (path === '/api/leave-approvers/approver-options') {
        return Promise.resolve([{ id: 'M001', name: 'Manager One' }]);
      }
      if (path === '/api/leave-approvers/staff/S001') {
        return Promise.resolve([{
          id: 'LA1',
          approver: { id: 'M001', name: 'Manager One' },
          effectiveFrom: '2026-01-01',
          effectiveTo: '2026-12-31',
        }]);
      }
      return Promise.resolve([]);
    });
  });

  it('adds a new assignment with the effective start date defaulted to today', async () => {
    renderField();

    fireEvent.click(screen.getByRole('button', { name: /Add Leave Approver/i }));

    const expected = new Date();
    const expectedDate = `${expected.getFullYear()}-${String(expected.getMonth() + 1).padStart(2, '0')}-${String(expected.getDate()).padStart(2, '0')}`;
    expect(screen.getByLabelText('Effective Start Date')).toHaveValue(expectedDate);
    await waitFor(() => expect(mockedApiFetch).toHaveBeenCalledWith('/api/leave-approvers/approver-options'));
  });

  it('loads historical/current assignments when editing staff', async () => {
    renderField(true, 'S001');

    expect(await screen.findByDisplayValue('2026-01-01')).toBeInTheDocument();
    expect(screen.getByDisplayValue('2026-12-31')).toBeInTheDocument();
    expect(mockedApiFetch).toHaveBeenCalledWith('/api/leave-approvers/staff/S001');
  });

  it('removes an assignment row from the staff form', async () => {
    renderField(true, 'S001');

    expect(await screen.findByDisplayValue('2026-01-01')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Remove/i }));
    await waitFor(() => expect(screen.queryByDisplayValue('2026-01-01')).not.toBeInTheDocument());
  });
});

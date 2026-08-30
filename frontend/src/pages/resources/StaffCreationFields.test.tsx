import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { Form } from 'antd';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { StaffCreationFields } from './StaffCreationFields.tsx';

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }));

vi.mock('../../api/http.ts', () => ({ apiFetch: apiFetchMock }));

afterEach(() => {
  apiFetchMock.mockReset();
});

const renderReviewStep = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <Form
        initialValues={{
          jurisdictionId: 'SG',
          joinDate: '2026-01-01',
          employmentType: 'FULL_TIME',
          dependants: [{
            name: 'Child',
            relationshipCode: 'CHILD',
            dateOfBirth: '2024-01-01',
            citizenshipCode: 'SG',
            residencyCode: 'SG',
            active: true,
          }],
        }}
      >
        <StaffCreationFields step={1} />
      </Form>
    </QueryClientProvider>,
  );
};

describe('StaffCreationFields entitlement review', () => {
  it('renders generated entitlements stored in the unregistered review field', async () => {
    apiFetchMock.mockImplementation((path: string) => {
      if (path === '/api/leave-calendars') return Promise.resolve([]);
      if (path === '/api/jurisdictions') return Promise.resolve([]);
      if (path === '/api/staff/entitlement-proposals/analysis') {
        return Promise.resolve({
          status: 'AVAILABLE',
          proposals: [{
            leaveType: { id: 'ANNUAL_LEAVE', name: 'Annual Leave' },
            from: '2026-01-01',
            to: '2026-12-31',
            entitlement: 14,
            policyId: 'SG-ANNUAL',
            baseEntitlementAmount: 14,
          }],
        });
      }
      return Promise.reject(new Error(`Unexpected API call: ${path}`));
    });

    renderReviewStep();

    expect(await screen.findByText('Annual Leave')).toBeTruthy();
    expect(screen.getByText('14 days')).toBeTruthy();

    await waitFor(() => {
      expect(apiFetchMock).toHaveBeenCalledWith(
        '/api/staff/entitlement-proposals/analysis',
        expect.objectContaining({ method: 'POST' }),
      );
    });
    expect(apiFetchMock).not.toHaveBeenCalledWith('/api/staff', expect.anything());
  });
});

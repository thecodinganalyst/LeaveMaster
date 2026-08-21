import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { Form } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../../api/http.ts';
import { StaffFormFields } from './StaffFormFields.tsx';
import { DEFAULT_STAFF_WORK_SCHEDULE } from './staffFormHelpers.ts';

const mockState = vi.hoisted(() => ({
  proposals: [] as Array<Record<string, unknown>>,
}));

vi.mock('../../api/http.ts', () => ({
  apiFetch: vi.fn(async (path: string) => {
    if (path === '/api/leave-calendars') return [{ jurisdictionId: 'SG' }];
    if (path === '/api/jurisdictions') return [{ id: 'SG', code: 'SG', name: 'Singapore', parentId: null }];
    if (path === '/api/leave-types') return [{ id: 'tenant-a:ANNUAL_LEAVE', name: 'Annual Leave' }];
    if (path === '/api/staff/entitlement-proposals') return mockState.proposals;
    return [];
  }),
}));

const renderStaffForm = () => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <Form
        initialValues={{
          id: 'S1',
          name: 'Staff One',
          joinDate: '2026-07-01',
          jurisdictionId: 'SG',
          workSchedule: DEFAULT_STAFF_WORK_SCHEDULE,
          leaveEntitlements: [],
        }}
      >
        <StaffFormFields />
      </Form>
    </QueryClientProvider>,
  );
};

describe('staff entitlement template proposals', () => {
  beforeEach(() => {
    mockState.proposals = [];
    vi.mocked(apiFetch).mockClear();
  });

  it('automatically populates editable entitlements from the proposal endpoint', async () => {
    mockState.proposals = [{
      leaveType: { id: 'tenant-a:ANNUAL_LEAVE', name: 'Annual Leave' },
      from: '2026-01-01',
      to: '2026-12-31',
      entitlement: 10.08,
      policyId: 'template-sg-annual',
      baseEntitlementAmount: 10.08,
      carriedForwardAmount: 0,
      adjustmentAmount: 0,
    }];

    renderStaffForm();

    await waitFor(() => expect(apiFetch).toHaveBeenCalledWith(
      '/api/staff/entitlement-proposals',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ jurisdictionId: 'SG', joinDate: '2026-07-01' }),
      }),
    ));
    expect(await screen.findByDisplayValue('template-sg-annual')).toBeInTheDocument();
    expect(document.getElementById('leaveEntitlements_0_entitlement')).toHaveAttribute('value', '10.08');
  });

  it('recalculates when an eligibility-relevant staff value changes', async () => {
    mockState.proposals = [];
    renderStaffForm();

    await waitFor(() => expect(
      vi.mocked(apiFetch).mock.calls.filter(([path]) => path === '/api/staff/entitlement-proposals'),
    ).toHaveLength(1));

    fireEvent.change(screen.getByLabelText('Join date'), { target: { value: '2026-01-01' } });

    await waitFor(() => expect(apiFetch).toHaveBeenCalledWith(
      '/api/staff/entitlement-proposals',
      expect.objectContaining({
        body: JSON.stringify({ jurisdictionId: 'SG', joinDate: '2026-01-01' }),
      }),
    ));
    expect(await screen.findByText('No entitlement policy templates currently match this staff member')).toBeInTheDocument();
  });
});

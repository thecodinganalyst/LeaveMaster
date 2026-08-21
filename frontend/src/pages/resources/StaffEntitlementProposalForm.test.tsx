import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { Form } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../../api/http.ts';
import { StaffFormFields } from './StaffFormFields.tsx';
import { DEFAULT_STAFF_WORK_SCHEDULE } from './staffFormHelpers.ts';

const mockState = vi.hoisted(() => ({
  proposals: [] as Array<Record<string, unknown>>,
  status: 'NO_TEMPLATE' as 'AVAILABLE' | 'NO_TEMPLATE' | 'NOT_ELIGIBLE_IN_PERIOD',
}));

vi.mock('../../api/http.ts', () => ({
  apiFetch: vi.fn(async (path: string) => {
    if (path === '/api/leave-calendars') return [{ jurisdictionId: 'SG' }];
    if (path === '/api/jurisdictions') return [{ id: 'SG', code: 'SG', name: 'Singapore', parentId: null }];
    if (path === '/api/leave-types') return [{ id: 'tenant-a:ANNUAL_LEAVE', name: 'Annual Leave' }];
    if (path === '/api/staff/entitlement-proposals/analysis') {
      return { proposals: mockState.proposals, status: mockState.status };
    }
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
    mockState.status = 'NO_TEMPLATE';
    vi.mocked(apiFetch).mockClear();
  });

  it('automatically populates editable entitlements from the proposal analysis endpoint', async () => {
    mockState.status = 'AVAILABLE';
    mockState.proposals = [{
      leaveType: { id: 'tenant-a:ANNUAL_LEAVE', name: 'Annual Leave' },
      from: '2026-11-03',
      to: '2026-12-31',
      entitlement: 2,
      policyId: 'template-sg-annual',
      baseEntitlementAmount: 2,
      carriedForwardAmount: 0,
      adjustmentAmount: 0,
    }];

    renderStaffForm();

    await waitFor(() => expect(apiFetch).toHaveBeenCalledWith(
      '/api/staff/entitlement-proposals/analysis',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ jurisdictionId: 'SG', joinDate: '2026-07-01' }),
      }),
    ));
    expect(await screen.findByDisplayValue('template-sg-annual')).toBeInTheDocument();
    expect(document.getElementById('leaveEntitlements_0_from')).toHaveAttribute('value', '2026-11-03');
    expect(document.getElementById('leaveEntitlements_0_entitlement')).toHaveAttribute('value', '2');
  });

  it('distinguishes missing templates from not eligible in the current period', async () => {
    mockState.status = 'NO_TEMPLATE';
    const firstRender = renderStaffForm();
    expect(await screen.findByText("No entitlement policy templates are configured for this staff member's leave types")).toBeInTheDocument();
    firstRender.unmount();

    mockState.status = 'NOT_ELIGIBLE_IN_PERIOD';
    renderStaffForm();
    expect(await screen.findByText(
      'Entitlement policy templates exist, but this staff member is not eligible during the current leave-calendar period',
    )).toBeInTheDocument();
  });

  it('recalculates when an eligibility-relevant staff value changes', async () => {
    mockState.status = 'NOT_ELIGIBLE_IN_PERIOD';
    renderStaffForm();

    await waitFor(() => expect(
      vi.mocked(apiFetch).mock.calls.filter(([path]) => path === '/api/staff/entitlement-proposals/analysis'),
    ).toHaveLength(1));

    fireEvent.change(screen.getByLabelText('Join date'), { target: { value: '2026-01-01' } });

    await waitFor(() => expect(apiFetch).toHaveBeenCalledWith(
      '/api/staff/entitlement-proposals/analysis',
      expect.objectContaining({
        body: JSON.stringify({ jurisdictionId: 'SG', joinDate: '2026-01-01' }),
      }),
    ));
    expect(await screen.findByText(
      'Entitlement policy templates exist, but this staff member is not eligible during the current leave-calendar period',
    )).toBeInTheDocument();
  });
});

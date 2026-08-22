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

const renderStaffForm = (extraInitialValues: Record<string, unknown> = {}) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const onFinish = vi.fn();
  const result = render(
    <QueryClientProvider client={queryClient}>
      <Form
        initialValues={{
          id: 'S1',
          name: 'Staff One',
          joinDate: '2026-07-01',
          workSchedule: DEFAULT_STAFF_WORK_SCHEDULE,
          leaveEntitlements: [],
          ...extraInitialValues,
        }}
        onFinish={onFinish}
      >
        <StaffFormFields />
        <button type="submit">Save</button>
      </Form>
    </QueryClientProvider>,
  );
  return { ...result, onFinish };
};

describe('staff entitlement template proposals', () => {
  beforeEach(() => {
    mockState.proposals = [];
    mockState.status = 'NO_TEMPLATE';
    vi.mocked(apiFetch).mockClear();
  });

  it('preselects the only jurisdiction and automatically populates editable entitlements', async () => {
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
    expect(await screen.findByText('Singapore')).toBeInTheDocument();
    expect(await screen.findByDisplayValue('template-sg-annual')).toBeInTheDocument();
    expect(document.getElementById('leaveEntitlements_0_from')).toHaveAttribute('value', '2026-11-03');
    expect(document.getElementById('leaveEntitlements_0_entitlement')).toHaveAttribute('value', '2.0');
  });

  it('rejects an invalid email and allows submission after the email is corrected', async () => {
    const { onFinish } = renderStaffForm({ email: 'not-an-email' });

    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('Enter a valid email address')).toBeInTheDocument();
    expect(onFinish).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'staff@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => expect(onFinish).toHaveBeenCalledTimes(1));
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

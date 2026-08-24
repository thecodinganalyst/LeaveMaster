import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { StaffLeaveEntitlementsField, StaffWorkScheduleField } from './StaffDetailStructuredFields.tsx';

describe('Staff detail structured fields', () => {
  it('renders a readable weekly work schedule from the serialized staff value', () => {
    render(<StaffWorkScheduleField value={JSON.stringify([
      { dayOfWeek: 'MONDAY', daySchedule: 'FULL' },
      { dayOfWeek: 'TUESDAY', daySchedule: 'AM' },
      { dayOfWeek: 'WEDNESDAY', daySchedule: 'PM' },
    ])} />);

    expect(screen.getByText('Monday')).toBeInTheDocument();
    expect(screen.getByText('Full day')).toBeInTheDocument();
    expect(screen.getByText('AM')).toBeInTheDocument();
    expect(screen.getByText('PM')).toBeInTheDocument();
    expect(screen.getByText('Sunday')).toBeInTheDocument();
    expect(screen.getAllByText('Not working')).toHaveLength(4);
    expect(screen.queryByText(/dayOfWeek/)).not.toBeInTheDocument();
  });

  it('groups leave entitlements by period and hides internal ids', () => {
    render(<StaffLeaveEntitlementsField value={JSON.stringify([
      {
        id: 'entitlement-123',
        leaveType: { id: 'leave-type-123', name: 'Annual Leave' },
        from: '2026-01-01',
        to: '2026-12-31',
        entitlement: 14,
        policyId: 'policy-123',
        baseEntitlementAmount: 12,
        carriedForwardAmount: 1,
        adjustmentAmount: 1,
      },
      {
        id: 'entitlement-456',
        leaveType: { id: 'leave-type-456', name: 'Marriage Leave' },
        from: '2026-01-01',
        to: '2026-12-31',
        entitlement: 2,
        baseEntitlementAmount: 2,
        carriedForwardAmount: 0,
        adjustmentAmount: 0,
      },
      {
        leaveType: { name: 'Sick Leave' },
        from: '2026-11-10',
        to: '2026-12-31',
        entitlement: 5,
      },
    ])} />);

    expect(screen.getByText('Annual Leave')).toBeInTheDocument();
    expect(screen.getByText('Marriage Leave')).toBeInTheDocument();
    expect(screen.getByText('Sick Leave')).toBeInTheDocument();
    expect(screen.getByText('2026-01-01 to 2026-12-31')).toBeInTheDocument();
    expect(screen.getByText('2026-11-10 to 2026-12-31')).toBeInTheDocument();
    expect(screen.queryByRole('columnheader', { name: 'Period' })).not.toBeInTheDocument();
    expect(screen.getByText('Base 12 · Carry forward 1 · Adjustment 1')).toBeInTheDocument();
    expect(screen.queryByText('entitlement-123')).not.toBeInTheDocument();
    expect(screen.queryByText('leave-type-123')).not.toBeInTheDocument();
    expect(screen.queryByText('policy-123')).not.toBeInTheDocument();
  });

  it('uses clear empty states for empty, missing, or invalid structured values', () => {
    const { rerender } = render(<StaffWorkScheduleField value="[]" />);
    expect(screen.getByText('No work schedule configured.')).toBeInTheDocument();

    rerender(<StaffLeaveEntitlementsField value={null} />);
    expect(screen.getByText('No leave entitlements configured.')).toBeInTheDocument();

    rerender(<StaffWorkScheduleField value="not-json" />);
    expect(screen.getByText('No work schedule configured.')).toBeInTheDocument();
  });
});
